// NeoApk is the zip and signing engine the upstream patcher moved to. Rather than building it as
// its own Gradle build, which would need a Kotlin plugin version this checkout cannot fetch, its
// sources are compiled here inside the build that consumes them.
plugins {
    id("java-library")
    // Applied without a version and aliased from the catalogue: AGP already puts Kotlin on the
    // classpath, and asking for a second, versioned copy of that plugin is refused.
    id("org.jetbrains.kotlin.jvm")
}

val androidSourceCompatibility = rootProject.extra["androidSourceCompatibility"] as JavaVersion
val androidTargetCompatibility = rootProject.extra["androidTargetCompatibility"] as JavaVersion

java {
    sourceCompatibility = androidSourceCompatibility
    targetCompatibility = androidTargetCompatibility
}

kotlin {
    sourceSets["main"].kotlin.srcDir(file("../../NeoApk/src/main/kotlin"))
}

dependencies {
    // Signing reads and writes v2/v3 blocks, which NeoApk hands to BouncyCastle.
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
}
