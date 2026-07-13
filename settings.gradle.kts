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

rootProject.name = "pictospeak"

include(":app")
include(":nlg")
include(":speech")
include(":llm")
include(":benchmark")
// Offline content pipeline CLIs — run manually per release, never shipped (tools/README.md).
include(":tools:arasaac-fetch")
