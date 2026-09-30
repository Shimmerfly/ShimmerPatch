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
    // Both zip and signing engines ship in the manager, which picks one when it patches.
    implementation(projects.apkzlib)
    implementation(projects.zipengines.neoapk)
    implementation(projects.share.java)
    implementation("vector:axml")

    // Android ships BKS already; a host JVM needs the provider, which also lets the temporary
    // runPatch task below sign like the device does.
    runtimeOnly("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation(libs.commons.io)
    implementation(libs.beust.jcommander)
    implementation(libs.google.gson)
}

// Temporary helper: runs the patcher from the host while the loader layer is being debugged, so a
// patched APK can be produced without going through the manager's UI. Remove once that is settled.
tasks.register<JavaExec>("runPatch") {
    classpath = sourceSets["main"].runtimeClasspath +
        files(rootProject.file("manager/src/main/assets")) +
        files(providers.gradleProperty("patchAssets").map { listOf(it) }.getOrElse(emptyList()))
    mainClass = "moe.shimmerfly.shimmerpatch.patch.ShimmerPatch"
    jvmArgs("-Djava.security.properties=" + rootProject.file("build/bc.security").absolutePath)
}
