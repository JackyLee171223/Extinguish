pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
    }
}


rootProject.name = "Extinguish"
include(":app")
include(":kotlinBenchmark")
include(":hidden-api")
include(":root-service")
include(":shizuku-service")
include(":ipc")
