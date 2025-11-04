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
    }
}

rootProject.name = "LazyBones"
include(
    ":app",
    ":core:designsystem",
    ":core:domain",
    ":core:database",
    ":core:preferences",
    ":core:network",
    ":core:notification",
    ":feature:home",
    ":feature:plan",
    ":feature:reports",
    ":feature:voicenotes",
    ":feature:settings",
    ":feature:widget"
)
