enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ShimmerPatch"
include(
    ":apkzlib",
    ":jar",
    ":manager",
    ":meta-loader",
    ":patch",
    ":patch-loader",
    ":remote-api",
    ":share:android",
    ":share:java",
    ":zipengines:neoapk",
)

includeBuild("core") {
    dependencySubstitution {
        substitute(module("vector:axml")).using(project(":external:axml"))
        substitute(module("vector:bridge")).using(project(":hiddenapi:bridge"))
        substitute(module("vector:legacy")).using(project(":legacy"))
        substitute(module("vector:core")).using(project(":xposed"))
        substitute(module("vector:daemon-service")).using(project(":services:daemon-service"))
        substitute(module("vector:stubs")).using(project(":hiddenapi:stubs"))
    }
}

// NeoApk is the zip and signing engine the upstream patcher moved to. It is kept beside the
// in-tree apkzlib rather than replacing it, because the manager offers either engine at run time.
// Its sources are compiled by the :zipengines:neoapk project from the submodule, instead of being
// built as its own Gradle build or fetched from the coordinate upstream names - that coordinate
// has never been published, and the build it would come from needs a plugin version this
// checkout cannot download.
