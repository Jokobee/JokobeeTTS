plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// :free — normaliseurs 10 locales, homographes, voix officielles, API publique.
// Publié : com.jokobee:jokobeetts (artefact principal, arm64-v8a).
android {
    namespace = "com.jokobee.tts"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        // Free = arm64-v8a uniquement (le x86_64 émulateur est réservé au Pro).
        ndk { abiFilters += listOf("arm64-v8a") }
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
    api(libs.onnxruntime.android)          // inférence Kokoro + CharsiuG2P
    // ICU = android.icu.text (plateforme, API 24+) : aucune dépendance à ajouter.
}

mavenPublishing {
    coordinates("com.jokobee", "jokobeetts", "0.1.0")
}
