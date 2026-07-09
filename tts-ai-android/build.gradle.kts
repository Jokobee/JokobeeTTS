plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// :tts-ai-android — discoverability alias, zero code. Depends entirely on
// jokobeetts (the real engine) so it stays in lockstep with no duplicated
// maintenance. Published as: com.jokobee:tts-ai-android.
android {
    namespace = "com.jokobee.tts.alias"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":free"))
}

mavenPublishing {
    coordinates("com.jokobee", "tts-ai-android", "1.0.0")
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
        publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    }

    pom {
        name.set("TTS AI Android (JokobeeTTS)")
        description.set("On-device multilingual AI text-to-speech engine for Android, based on Kokoro-82M. Alias for com.jokobee:jokobeetts.")
        url.set("https://github.com/Jokobee/JokobeeTTS")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("jokobee")
                name.set("Jokobee")
                email.set("contact@jokobee.com")
                url.set("https://jokobee.com")
            }
        }
        scm {
            url.set("https://github.com/Jokobee/JokobeeTTS")
            connection.set("scm:git:https://github.com/Jokobee/JokobeeTTS.git")
            developerConnection.set("scm:git:ssh://git@github.com/Jokobee/JokobeeTTS.git")
        }
    }
}
