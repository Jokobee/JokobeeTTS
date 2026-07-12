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
// kokoro-french-voice-female: free Marine voice, zero Android dependency (ByteArray).
// kokoro-female-french-voice / kokoro-french-voice-marine: discoverability aliases for it.
include(
    ":core", ":free", ":tts-ai-android", ":tts-android-ai", ":tts-android",
    ":kokoro-french-voice-female", ":kokoro-female-french-voice", ":kokoro-french-voice-marine",
)
