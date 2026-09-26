import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuildConfigField

val defaultManagerPackageName = rootProject.extra["defaultManagerPackageName"] as String
val apiCode = rootProject.extra["apiCode"] as Int
val verCode = rootProject.extra["verCode"] as Int
val verName = rootProject.extra["verName"] as String
val coreVerCode = rootProject.extra["coreVerCode"] as Int
val coreVerName = rootProject.extra["coreVerName"] as String

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.rikka.tools.refine)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    defaultConfig {
        applicationId = defaultManagerPackageName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Stated here as well as in the root, where every module picks it up: the IDE reads a
        // module's own configuration first, and without it its analysis assumes an API floor of 1
        // and reports APIs this app has always been able to call.
        minSdk = rootProject.extra["androidMinSdkVersion"] as Int
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        jniLibs {
            excludes += "lib/*/libandroidx.graphics.path.so"
            excludes += "lib/*/libdatastore_shared_counter.so"
        }
        resources {
            excludes += "kotlin/**"
            excludes += "META-INF/androidx*"
            excludes += "META-INF/androidx/**"
            excludes += "DebugProbesKt.bin"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true      // 启用 R8/ProGuard 进行代码压缩、优化和混淆。
            isShrinkResources = true    // 启用资源缩减，移除未被引用的资源文件。
            isDebuggable = false        // 发布版本禁止调试。
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        all {
            sourceSets[name].assets.directories.add(rootProject.projectDir.resolve("out/assets/$name").absolutePath)
        }
    }

    buildFeatures {
        aidl = true
        compose = true
        buildConfig = true
    }

    // In-app language selection must work offline without Play language-split downloads.
    bundle.language.enableSplit = false

    namespace = "moe.shimmerfly.shimmerpatch"

}

androidComponents {
    onVariants { variant ->
        val variantLowered = variant.name.lowercase()
        val variantCapped = variant.name.replaceFirstChar { it.uppercase() }

        val configuredSignature = providers.environmentVariable("SHIMMERPATCH_MANAGER_SIGNATURE_SHA256")
            .orElse(providers.gradleProperty("shimmerpatchManagerSignatureSha256"))
        val signingConfig = android.buildTypes.getByName(requireNotNull(variant.buildType)).signingConfig
        // Resolve the signing values now, and keep the provider body free of project and script
        // references: the configuration cache cannot serialise either of them.
        val signatureConfig = requireNotNull(signingConfig) { "Missing manager signing config for ${variant.name}" }
        val keystorePath = requireNotNull(signatureConfig.storeFile) { "Missing manager signing keystore for ${variant.name}" }.path
        val keystoreType = signatureConfig.storeType ?: KeyStore.getDefaultType()
        val keystorePassword = signatureConfig.storePassword ?: ""
        val keystoreAlias = signatureConfig.keyAlias
        val variantName = variant.name
        val configuredValue = configuredSignature.orNull
        val signatureAllowlist = providers.provider {
            val fingerprints = configuredValue ?: run {
                val keystore = File(keystorePath)
                if (!keystore.isFile) {
                    // A machine that has never built this project - CI, a fresh clone - has no debug
                    // keystore yet: AGP creates it while validating the signing config, which runs
                    // after the configuration cache has been written, so reading the file here would
                    // fail the build instead of the task that needs it. An empty allowlist is what
                    // the runtime check already treats as "not pinned".
                    ""
                } else {
                    val store = KeyStore.getInstance(keystoreType)
                    keystore.inputStream().use { store.load(it, keystorePassword.toCharArray()) }
                    val certificate = requireNotNull(store.getCertificate(keystoreAlias)) {
                        "Missing manager signing certificate for $variantName"
                    }
                    MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
                        .joinToString("") { "%02X".format(Locale.ROOT, it) }
                }
            }
            fingerprints.split(',', ';', ' ', '\n', '\r', '\t')
                .map { entry -> entry.trim().uppercase(Locale.ROOT) }
                .filter { entry -> entry.isNotEmpty() }
                .distinct()
                .joinToString(",") { entry ->
                    // Store the digest obfuscated (XOR 0x5A, base64) so the fingerprint is not a
                    // plain literal in the APK.
                    require(entry.length == 64) { "Manager signature digest must be 64 hex chars: $entry" }
                    val digest = ByteArray(32) { index ->
                        entry.substring(index * 2, index * 2 + 2).toInt(16).toByte()
                    }
                    Base64.getEncoder()
                        .encodeToString(ByteArray(32) { index -> (digest[index].toInt() xor 0x5A).toByte() })
                }
        }
        requireNotNull(variant.buildConfigFields).put(
            "MANAGER_SIGNATURE_SHA256_ALLOWLIST",
            signatureAllowlist.map { encoded ->
                BuildConfigField("String", "\"$encoded\"", "SHA-256 fingerprints for the selected signing certificate")
            },
        )
        // validateSigning also creates the default debug keystore on a fresh checkout.
        tasks.configureEach {
            if (name == "generate${variantCapped}BuildConfig") {
                dependsOn("validateSigning$variantCapped")
            }
        }

        // Plain files, so the task actions do not capture the project or the script.
        val assetsSourceDir = rootProject.layout.projectDirectory.dir("out/assets/${variant.name}").asFile
        val mergedAssetsDir = layout.buildDirectory.dir("intermediates/assets/$variantLowered/merge${variantCapped}Assets")
        val staleLoaderDex = mergedAssetsDir.map { it.file("shimmerpatch/loader.dex") }.get().asFile

        val copyAssetsTaskProvider = tasks.register<Copy>("copy${variantCapped}Assets") {
            dependsOn(":meta-loader:copy$variantCapped")
            dependsOn(":patch-loader:copy$variantCapped")

            doFirst {
                staleLoaderDex.delete()
            }
            into(mergedAssetsDir)

            from(assetsSourceDir)
        }

        tasks.configureEach {
            if (name == "merge${variantCapped}Assets") {
                dependsOn(copyAssetsTaskProvider)
            }
            // Lint inspects the same asset directory. If packaging is requested in the
            // same invocation, wait for its producers; standalone Lint need not build JNI.
            if (name == "generate${variantCapped}LintReportModel" || name == "lintAnalyze${variantCapped}") {
                mustRunAfter(
                    ":meta-loader:copyDex$variantCapped",
                    ":patch-loader:copyDex$variantCapped",
                    ":patch-loader:copySo$variantCapped",
                )
            }
        }

        tasks.register<Copy>("build$variantCapped") {
            dependsOn("assemble$variantCapped")
            from(variant.artifacts.get(SingleArtifact.APK))
            into("${rootProject.projectDir}/out/$variantLowered")
            rename(".*.apk", "ShimmerPatch-v$verName-$verCode-$variantLowered.apk")
        }
    }
}

dependencies {
    implementation(projects.patch)
    implementation(projects.share.android)
    implementation(projects.share.java)
    implementation(libs.vector.daemon.service)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.materialkolor)
    // Miuix is confined to navigation and blur; all widgets use Material 3.
    implementation(libs.miuix.nav)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.shader)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.gson)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.okhttp)
    implementation(libs.okhttp.dnsoverhttps)

    implementation(libs.gson)
    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)
    implementation(libs.rikka.refine)
    //implementation(libs.raamcosta.compose.destinations)
    implementation(libs.appiconloader)
    implementation(libs.hiddenapibypass)

    implementation(libs.androidx.webkit)


    annotationProcessor(libs.androidx.room.compiler)
    compileOnly(libs.rikka.hidden.stub)
    ksp(libs.androidx.room.compiler)
    //ksp(libs.raamcosta.compose.destinations.ksp)

    // Keep app/test runtime versions aligned with AndroidX Test 1.7.
    implementation(libs.androidx.concurrent.futures)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.customview)
    debugImplementation(libs.androidx.customview.poolingcontainer)
}

kotlin {
    compilerOptions {
        // Match WeKit: all manager compilations, including tests, use these Material APIs.
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}
