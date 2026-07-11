plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// :kokoro-french-voice-female — Marine, a free French voice for Kokoro-82M.
// Zero Android dependency (pure ByteArray via classloader resources) — works in
// any JVM or Android project. Published as: com.jokobee:kokoro-french-voice-female.
android {
    namespace = "com.jokobee.tts.voice.marine"
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

mavenPublishing {
    coordinates("com.jokobee", "kokoro-french-voice-female", "1.0.0")
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
        publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    }

    pom {
        name.set("Kokoro French Voice — Marine (Free)")
        description.set("Marine, a free French female voice for Kokoro-82M text-to-speech. Ready-to-use ByteArray, no Android Context required — works standalone with Kokoro-82M/kokoro-onnx, or with JokobeeTTS. jokobee.com")
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
