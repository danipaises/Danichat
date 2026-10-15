pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "danichat-android"
include(":app")
include(":wear")
include(":feature:welcome")
include(":data:account")
include(":feature:account")
// Using published Arti AAR; local module not included
