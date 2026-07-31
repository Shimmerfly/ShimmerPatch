plugins {
    id("com.android.library")
    id("maven-publish")
}

group = "top.nkbe.npatch"
version = "1.0.0"

android {
    namespace = "top.nkbe.npatch.remote"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        androidResources = false
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    compileOnly("io.github.libxposed:interface:102.0.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "npatch-remote-api"
            version = project.version.toString()
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name = "NPatch Remote API"
                description = "Authenticated NPatch Manager remote storage client for Xposed module apps"
                url = "https://github.com/7723mod/NPatch-Remote-API"
                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.html"
                    }
                }
                scm {
                    url = "https://github.com/7723mod/NPatch-Remote-API"
                    connection = "scm:git:https://github.com/7723mod/NPatch-Remote-API.git"
                }
            }
        }
    }
}
