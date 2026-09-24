plugins {
    alias(libs.plugins.agp.lib)
}

android {
    namespace = "top.nkbe.npatch.share"

    androidResources.enable = false

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation("vector:daemon-service")
}
