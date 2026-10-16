enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}

rootProject.name = "NanasCode"

// App shell
include(":app")

// Core modules
include(":core:model")
include(":core:common")
include(":core:security")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:git")
include(":core:ui")

// Feature modules
include(":feature:auth")
include(":feature:repo")
include(":feature:actions")
include(":feature:workspace")
include(":feature:editor")
