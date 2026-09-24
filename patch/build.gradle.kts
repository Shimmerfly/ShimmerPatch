val androidSourceCompatibility = rootProject.extra["androidSourceCompatibility"] as JavaVersion
val androidTargetCompatibility = rootProject.extra["androidTargetCompatibility"] as JavaVersion

plugins {
    id("java-library")
}

java {
    sourceCompatibility = androidSourceCompatibility
    targetCompatibility = androidTargetCompatibility
    sourceSets {
        main {
            java.srcDirs("libs/manifest-editor/lib/src/main/java")
            resources.srcDirs("libs/manifest-editor/lib/src/main")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation(projects.apkzlib)
    implementation(projects.share.java)
    implementation("vector:axml")

    implementation(libs.commons.io)
    implementation(libs.beust.jcommander)
    implementation(libs.google.gson)
}
