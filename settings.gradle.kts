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

rootProject.name = "JokobeeTTS"

// Open Core — PUBLIC repo: :core + :free (Maven Central).
// The :pro tier lives in the PRIVATE JokobeeTTS-Private repo (composite build into here).
// tts-ai-android / tts-android-ai: zero-code discoverability aliases for :free.
include(":core", ":free", ":tts-ai-android", ":tts-android-ai")
