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
    coordinates("com.jokobee", "jokobeetts-core", "1.1.1")
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
        publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    }

    pom {
        name.set("JokobeeTTS Core")
        description.set("Contracts, exceptions, LangRouter, TextSplitter and Voice model shared by JokobeeTTS. jokobee.com")
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
