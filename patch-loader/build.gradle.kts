import java.util.Locale

plugins {
    alias(libs.plugins.agp.app)
}

extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    ndkVersion = "30.0.16248370"
    defaultConfig {
        multiDexEnabled = false

        externalNativeBuild {
            cmake {
                arguments += "-DCORE_ROOT=${File(rootDir.absolutePath, "core/native") }"
                arguments += "-DEXTERNAL_ROOT=${File(rootDir.absolutePath, "core/external") }"
                arguments += "-DVERSION_CODE=${rootProject.extra["verCode"]}"
                arguments += "-DVERSION_NAME=${rootProject.extra["verName"]}"
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/jni/CMakeLists.txt")
            version = "3.31.6"
        }
    }

    packaging {
        dex {
            useLegacyPackaging = true
        }
    }
    namespace = "moe.shimmerfly.shimmerpatch.loader"
}

androidComponents.onVariants { variant ->
    val variantCapped = variant.name.replaceFirstChar { it.uppercase() }
    val variantLowered = variant.name.lowercase()
    val dexDirProvider = if (variant.buildType == "release") {
        layout.buildDirectory.dir("intermediates/dex/$variantLowered/minify${variantCapped}WithR8")
    } else {
        layout.buildDirectory.dir("intermediates/dex/$variantLowered/mergeDex$variantCapped")
    }

    // Resolve the output locations while configuring, so the tasks capture plain values instead of
    // the project and the script state, which the configuration cache cannot serialise.
    val assetsDir = rootProject.layout.projectDirectory.dir("out/assets/${variant.name}/shimmerpatch").asFile
    val outDirLabel = rootProject.layout.projectDirectory.dir("out").asFile.path

    val copyDexTask = tasks.register<Copy>("copyDex$variantCapped") {
        dependsOn("assemble$variantCapped")
        doFirst {
            File(assetsDir, "loader.dex").delete()
            File(assetsDir, "loader.bin").delete()
        }
        from(dexDirProvider)
        rename("classes.dex", "loader.bin")
        into(assetsDir)
    }

    val copySoTask = tasks.register<Copy>("copySo$variantCapped") {
        dependsOn("assemble$variantCapped")
        dependsOn("strip${variantCapped}DebugSymbols")
        from(
            fileTree(
                "dir" to layout.buildDirectory.dir("intermediates/stripped_native_libs/${variant.name}/strip${variantCapped}DebugSymbols/out/lib"),
                "include" to listOf("**/libshimmerpatch.so")
            )
        )
        into(File(assetsDir, "so"))
    }

    tasks.register("copy$variantCapped") {
        dependsOn(copySoTask)
        dependsOn(copyDexTask)

        doLast {
            println("Dex and so files have been copied to $outDirLabel")
        }
    }
}

dependencies {
    compileOnly("vector:stubs")
    implementation("vector:core")
    implementation("vector:bridge")
    implementation("vector:daemon-service")
    implementation("vector:legacy")
    implementation(projects.share.android)
    implementation(projects.share.java)
    implementation(libs.hiddenapibypass)

    implementation(libs.gson)
}
