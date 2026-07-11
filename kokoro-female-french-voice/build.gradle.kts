plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// :kokoro-female-french-voice — discoverability alias, zero code. Depends entirely
// on kokoro-french-voice-female (the real content) so it stays in lockstep with no
// duplicated maintenance. Published as: com.jokobee:kokoro-female-french-voice.
android {
    namespace = "com.jokobee.tts.voice.marine.alias1"
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
    api(project(":kokoro-french-voice-female"))
}

mavenPublishing {
    coordinates("com.jokobee", "kokoro-female-french-voice", "1.0.0")
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
        publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    }

    pom {
        name.set("Kokoro Female French Voice (Jokobee)")
        description.set("Marine, a free French female voice for Kokoro-82M text-to-speech. Alias for com.jokobee:kokoro-french-voice-female.")
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
