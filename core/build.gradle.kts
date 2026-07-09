plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// :core — contracts, exceptions, LangRouter (interface), TextSplitter, Voice.
// Published as: com.jokobee:jokobeetts-core
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

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.org.json)   // manifest.json parsed in JVM tests (org.json provided by Android at runtime)
}

mavenPublishing {
    coordinates("com.jokobee", "jokobeetts-core", "1.0.0")
}
