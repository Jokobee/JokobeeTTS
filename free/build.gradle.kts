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
    api(libs.onnxruntime.android)          // inférence Kokoro + CharsiuG2P
    // ⚠ icu4j EMBARQUÉ : android.icu n'expose PAS RuleBasedNumberFormat (spellout
    //   nombre→mots) dans l'API publique (API 36 : NumberFormat/PluralRules oui, RBNF
    //   NON). On bundle icu4j (licence Unicode, permissive). Le Verbalizer est une
    //   interface → remplaçable par une impl maison compacte plus tard (~13 Mo à
    //   optimiser vs cible AAR <5 Mo). Licence : voir THIRD-PARTY-NOTICES.md.
    api(libs.icu4j)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)      // lecture des cas JSON (test-scope)
    // Instrumented (device arm64) : validation latence G2P réelle. org.json = plateforme.
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

mavenPublishing {
    coordinates("com.jokobee", "jokobeetts", "0.1.0")
}
