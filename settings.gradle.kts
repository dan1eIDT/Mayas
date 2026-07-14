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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Mayas"
include(":app")
include(":core:ui")
include(":core:data")
include(":core:network")
include(":feature:auth")
include(":feature:call")
include(":feature")
include(":feature:chat")
include(":feature:profile")
include(":feature:chats")
include(":core:ui")
include(":core")
include(":core:data")
include(":feature:profile")
include(":feature:chats")
include(":feature:auth")
include(":feature:chat")
include(":core:network")

include(":feature:call")
