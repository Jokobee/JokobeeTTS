plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// :free — normalizers for 10 locales, homographs, official voices, public API.
// Published as: com.jokobee:jokobeetts (main artifact, arm64-v8a).
android {
    namespace = "com.jokobee.tts"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        // Free = arm64-v8a only (x86_64 emulator is reserved for Pro).
        ndk { abiFilters += listOf("arm64-v8a") }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
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
    api(project(":core"))
    api(libs.onnxruntime.android)          // Kokoro + CharsiuG2P inference
    // ⚠ icu4j BUNDLED: android.icu does NOT expose RuleBasedNumberFormat (spellout
    //   number→words) in the public API (API 36: NumberFormat/PluralRules yes, RBNF
    //   NO). We bundle icu4j (Unicode license, permissive). Verbalizer is an
    //   interface → replaceable later by a compact in-house impl (~13 MB to
    //   optimize vs <5 MB AAR target). License: see THIRD-PARTY-NOTICES.md.
    api(libs.icu4j)
    api(libs.kotlinx.coroutines.core)      // public API Flow<StreamChunk> (streaming F2)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)      // reads JSON test cases (test-scope)
    // Instrumented (arm64 device): validates real-world G2P latency. org.json = platform.
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

mavenPublishing {
    coordinates("com.jokobee", "jokobeetts", "1.1.1")
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
        publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    }

    pom {
        name.set("JokobeeTTS")
        description.set("On-device multilingual text-to-speech engine for Android, based on Kokoro-82M — normalization, G2P and 37 official voices across 6 languages.")
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

// THIRD-PARTY-NOTICES.txt bundled into the AAR assets (accessible at runtime via
// context.assets.open("THIRD-PARTY-NOTICES.txt") — "open source licenses" screen).
tasks.register<Copy>("copyThirdPartyNotices") {
    from(rootProject.file("THIRD-PARTY-NOTICES.txt"))
    into(layout.projectDirectory.dir("src/main/assets"))
}
tasks.named("preBuild") { dependsOn("copyThirdPartyNotices") }
