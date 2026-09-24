plugins {
    alias(libs.plugins.agp.lib)
}

android {
    namespace = "moe.shimmerfly.shimmerpatch.share"

    androidResources.enable = false

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation("vector:daemon-service")
}
