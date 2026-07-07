plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// :core — contrats, exceptions, LangRouter (interface), TextSplitter, Voice.
// Publié : com.jokobee:jokobeetts-core
android {
    namespace = "com.jokobee.tts.core"
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
    coordinates("com.jokobee", "jokobeetts-core", "0.1.0")
}
