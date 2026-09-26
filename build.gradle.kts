import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.extra
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
}

/**
 * A ValueSource that counts the commits reachable from the first ref that resolves.
 *
 * The ref is not always `master`: the repository may have renamed its default branch (this one
 * calls it `ShimmerPatch`), so the conventional names are probed behind the requested one. A ref
 * that does not resolve is expected and must not print a fatal error into the build log, so every
 * probe captures its stderr.
 *
 * This runs as a ValueSource because it has to start an external process while the configuration
 * cache is being written, where a build script may not: the previous JGit-based version failed the
 * build for that reason, and JGit itself probes the local git installation by starting
 * `git --version` and `git config --system ...`.
 */
abstract class GitCommitCountValueSource : ValueSource<Int, GitCommitCountValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val workingDirectory: Property<String>

        /** The refs to probe, in order. */
        val candidateRefs: ListProperty<String>

        /** Used when no ref resolves, so a checkout without git metadata still has a version. */
        val fallback: Property<Int>
    }

    @get:Inject abstract val execOperations: ExecOperations

    override fun obtain(): Int {
        for (ref in parameters.candidateRefs.get()) {
            val output = ByteArrayOutputStream()
            val result = execOperations.exec {
                commandLine("git", "-C", parameters.workingDirectory.get(), "rev-list", "--count", ref)
                standardOutput = output
                errorOutput = ByteArrayOutputStream()
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) {
                output.toString().trim().toIntOrNull()?.let { return it }
            }
        }
        return parameters.fallback.get()
    }
}

/**
 * Rewrites AGP's optimized resource archive through `aapt2 optimize`, in place.
 *
 * The action deliberately captures nothing but plain files. Holding the AGP extension, the project
 * or a lazy delegate here used to drag AGP's whole service graph - Kotlin's built-in compilation
 * state included - into the configuration cache, which cannot serialise any of it, and a task class
 * declared in this script would not be serialisable either. The archive is an output of AGP's own
 * `optimizeReleaseResources`, so it is not tracked as an input or an output.
 */
fun Project.registerResourceOptimizer(androidComponents: ApplicationAndroidComponentsExtension) {
    val isWindows = providers.systemProperty("os.name").get().lowercase().contains("windows")
    tasks.register("optimizeReleaseRes") {
        val aapt2 = File(
            androidComponents.sdkComponents.sdkDirectory.get().asFile,
            "build-tools/$androidBuildToolsVersion/${if (isWindows) "aapt2.exe" else "aapt2"}",
        )
        val archive = layout.buildDirectory.get().asFile.resolve(
            "intermediates/optimized_processed_res/release/optimizeReleaseResources/resources-release-optimize.ap_"
        )
        doLast {
            if (!archive.isFile) return@doLast
            val optimized = File(archive.parentFile, "${archive.name}.opt")
            optimized.delete()
            val process = ProcessBuilder(
                aapt2.absolutePath,
                "optimize",
                "--collapse-resource-names",
                "--enable-sparse-encoding",
                "-o", optimized.absolutePath,
                archive.absolutePath,
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            check(process.waitFor() == 0) { "aapt2 optimize failed for ${archive.name}: $output" }
            archive.delete()
            check(optimized.renameTo(archive)) { "Could not replace ${archive.name} with its optimized copy" }
        }
    }
}

val commitCount = providers.of(GitCommitCountValueSource::class) {
    parameters.workingDirectory.set(rootDir.absolutePath)
    parameters.candidateRefs.set(
        listOf(
            "refs/remotes/origin/HEAD",
            "refs/remotes/origin/ShimmerPatch",
            "refs/remotes/origin/master",
            "refs/heads/ShimmerPatch",
            "refs/heads/master",
            "HEAD",
        )
    )
    parameters.fallback.set(1)
}.get().coerceAtLeast(1)

val coreCommitCount = providers.of(GitCommitCountValueSource::class) {
    // A submodule's .git is a gitdir pointer file, which the git CLI resolves on its own.
    parameters.workingDirectory.set(File(rootDir, "core").absolutePath)
    parameters.candidateRefs.set(listOf("HEAD"))
    parameters.fallback.set(3083)
}.get()

val defaultManagerPackageName = "moe.shimmerfly.shimmerpatch"
val apiCode = 102

// CI resolves both of these and hands them over, so a release is named after the commit it was
// built from and no file has to be edited to cut one: the name is `git+<sha>` and the code is that
// commit's position in the branch. A local build keeps the two values below, which is what an
// unstamped build has always shown.
val verCode = providers.gradleProperty("verCode").orNull?.toIntOrNull() ?: commitCount
val verName = providers.gradleProperty("verName").orNull ?: "1.1.4"
val coreVerCode = coreCommitCount
val coreVerName = "v2.2-core"
val androidMinSdkVersion = 28
val androidTargetSdkVersion = 37
val androidCompileSdkVersion = 37
val androidCompileNdkVersion = "30.0.16248370"
val androidBuildToolsVersion = "37.0.0"
val androidSourceCompatibility = JavaVersion.VERSION_21
val androidTargetCompatibility = JavaVersion.VERSION_21

extra.set("defaultManagerPackageName", defaultManagerPackageName)
extra.set("apiCode", apiCode)
extra.set("verCode", verCode)
extra.set("verName", verName)
extra.set("coreVerCode", coreVerCode)
extra.set("coreVerName", coreVerName)
extra.set("androidMinSdkVersion", androidMinSdkVersion)
extra.set("androidTargetSdkVersion", androidTargetSdkVersion)
extra.set("androidCompileSdkVersion", androidCompileSdkVersion)
extra.set("androidCompileNdkVersion", androidCompileNdkVersion)
extra.set("androidBuildToolsVersion", androidBuildToolsVersion)
extra.set("androidSourceCompatibility", androidSourceCompatibility)
extra.set("androidTargetCompatibility", androidTargetCompatibility)

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

listOf("Debug", "Release").forEach { variant ->
    val variantLower = variant.lowercase()
    val remoteApiTask = tasks.register<Copy>("buildRemoteApi$variant") {
        description = "Build and collect the ShimmerPatch Remote API $variant AAR"
        dependsOn(":remote-api:assemble$variant")
        from(project(":remote-api").layout.buildDirectory.dir("outputs/aar")) {
            include("remote-api-$variantLower.aar")
            rename { "shimmerpatch-remote-api-v1.0.0-$variantLower.aar" }
        }
        into(layout.projectDirectory.dir("out/$variantLower"))
    }

    tasks.register("build$variant") {
        description = "Build ShimmerPatch with $variant"
        dependsOn(tasks.findByPath(":jar:build$variant") ?: "jar:build$variant")
        dependsOn(tasks.findByPath(":manager:build$variant") ?: "manager:build$variant")
        dependsOn(remoteApiTask)
    }
}

tasks.register("buildAll") {
    dependsOn("buildDebug", "buildRelease")
}

fun Project.configureBaseExtension() {
    extensions.findByType(CommonExtension::class)?.run {
        compileSdk = androidCompileSdkVersion
        ndkVersion = androidCompileNdkVersion
        buildToolsVersion = androidBuildToolsVersion
        // The other Android modules are Java-only and must not gain an implicit Kotlin runtime.
        enableKotlin = this@configureBaseExtension.path == ":manager"

        externalNativeBuild.cmake {
            version = "3.29.8+"
            buildStagingDirectory = layout.buildDirectory.get().asFile
        }

        defaultConfig.apply {
            minSdk = androidMinSdkVersion
            if (this is ApplicationDefaultConfig) targetSdk = androidTargetSdkVersion

            externalNativeBuild {
                cmake {
                    arguments += "-DVECTOR_ROOT=${File(rootDir.absolutePath, "core")}"
                    arguments += "-DEXTERNAL_ROOT=${File(rootDir.absolutePath, "core/external")}"
                    arguments += "-DCORE_ROOT=${File(rootDir.absolutePath, "core/native") }"
                    abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
                    val flags = arrayOf(
                        "-Wall",
                        "-Qunused-arguments",
                        "-Wno-gnu-string-literal-operator-template",
                        "-fno-rtti",
                        "-fvisibility=hidden",
                        "-fvisibility-inlines-hidden",
                        "-fno-exceptions",
                        "-fno-stack-protector",
                        "-fomit-frame-pointer",
                        "-Wno-builtin-macro-redefined",
                        "-Wno-unused-value",
                        "-D__FILE__=__FILE_NAME__",
                    )
                    cppFlags.addAll(listOf("-std=c++20", *flags))
                    cFlags.addAll(listOf("-std=c18", *flags))
                    arguments.addAll(listOf(
                        "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
                        "-DVERSION_CODE=$verCode",
                        "-DVERSION_NAME=$verName",
                    ))
                }
            }
        }

        compileOptions.apply {
            targetCompatibility = androidTargetCompatibility
            sourceCompatibility = androidSourceCompatibility
        }

        buildTypes.apply {
            getByName("debug").apply {
                externalNativeBuild {
                    cmake {
                        arguments.addAll(
                            arrayOf(
                                "-DCMAKE_CXX_FLAGS_DEBUG=-Og",
                                "-DCMAKE_C_FLAGS_DEBUG=-Og",
                            )
                        )
                    }
                }
            }
            getByName("release").apply {
                externalNativeBuild {
                    cmake {
                        val flags = arrayOf(
                            "-Wl,--exclude-libs,ALL",
                            "-ffunction-sections",
                            "-fdata-sections",
                            "-Wl,--gc-sections",
                            "-fno-unwind-tables",
                            "-fno-asynchronous-unwind-tables",
                            "-flto=thin",
                            "-Wl,--thinlto-cache-policy,cache_size_bytes=300m",
                            "-Wl,--thinlto-cache-dir=${layout.buildDirectory.get().asFile.absolutePath}/.lto-cache", 
                        )
                        cppFlags.addAll(flags)
                        cFlags.addAll(flags)
                        val configFlags = arrayOf(
                            "-Oz",
                            "-DNDEBUG"
                        ).joinToString(" ")
                        arguments.addAll(
                            arrayOf(
                                "-DCMAKE_CXX_FLAGS_RELEASE=$configFlags",
                                "-DCMAKE_CXX_FLAGS_RELWITHDEBINFO=$configFlags",
                                "-DCMAKE_C_FLAGS_RELEASE=$configFlags",
                                "-DCMAKE_C_FLAGS_RELWITHDEBINFO=$configFlags",
                                "-DDEBUG_SYMBOLS_PATH=${layout.buildDirectory.get().asFile.absolutePath}/symbols", 
                            )
                        )
                    }
                }
            }
        }
    }
}

fun Project.configureApplicationExtension(extension: ApplicationExtension) {
    extension.run {
        defaultConfig {
            versionCode = verCode
            versionName = verName
        }

        val config = signingConfigs.create("config") {
            val androidStoreFile = (
                System.getenv("ANDROID_STORE_FILE")
                    ?: project.findProperty("androidStoreFile")?.toString()
                )?.takeIf { it.isNotBlank() }
            val androidStorePassword = System.getenv("ANDROID_STORE_PASSWORD")
                ?: project.findProperty("androidStorePassword")?.toString()
            val androidKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
                ?: project.findProperty("androidKeyAlias")?.toString()
            val androidKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
                ?: project.findProperty("androidKeyPassword")?.toString()

            if (androidStoreFile != null && androidStorePassword != null && androidKeyAlias != null && androidKeyPassword != null) {
                storeFile = rootProject.file(androidStoreFile)
                storePassword = androidStorePassword
                keyAlias = androidKeyAlias
                keyPassword = androidKeyPassword
            }
            enableV2Signing = true
            enableV3Signing = true
        }
        val selectedSigningConfig = if (config.storeFile != null) config else signingConfigs["debug"]
        buildTypes.configureEach {
            signingConfig = selectedSigningConfig
        }
        lint {
            abortOnError = true
            checkReleaseBuilds = false
        }
    }

    extensions.findByType(ApplicationAndroidComponentsExtension::class)?.let { androidComponents ->
        registerResourceOptimizer(androidComponents)

        tasks.configureEach {
            if (name == "optimizeReleaseResources") {
                finalizedBy("optimizeReleaseRes")
            }
        }
    }
}

subprojects {
    plugins.withId("com.android.application") {
        configureBaseExtension()
        extensions.findByType(ApplicationExtension::class)?.let {
            configureApplicationExtension(it)
        }
    }
    plugins.withId("com.android.library") {
        configureBaseExtension()
    }
}
