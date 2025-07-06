pluginManagement {
    repositories {
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}
pluginManagement {
    plugins {
        id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
        id("com.android.application") version "8.12.0-alpha08" apply false
        id("org.jetbrains.kotlin.android") version "2.2.0" apply false
        id("kotlinx-serialization") version "3.2.0" apply false
        id("stringfog") version "5.2.0" apply false
    }
}

rootProject.name = "ZalithLauncher"
include(":ZalithLauncher")
