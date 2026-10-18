plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.animation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.navigation.compose)
            implementation(libs.datastore.preferences.core)
            implementation(libs.coil.compose)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.datastore.preferences)
            implementation(libs.security.crypto)
            implementation(libs.coil.network.okhttp)
        }

    }
}

android {
    namespace = "dev.sebas1705.careereditor"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.sebas1705.careereditor"
        minSdk = 24
        targetSdk = 35
        // CI sets both from the tag (VERSION_CODE = run number); local builds keep these
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 2
        versionName = System.getenv("VERSION_NAME") ?: "1.3.0"

        // The API token comes from Doppler (`doppler run -- ./gradlew ...`), never from the
        // repository. Without it the app builds fine and asks for the token on first launch.
        val apiToken = System.getenv("API_TOKEN").orEmpty().trim().replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "API_TOKEN", "\"$apiToken\"")

        // HTTP is refused everywhere except debug builds, which may talk to a local API (10.0.2.2)
        manifestPlaceholders["usesCleartext"] = "false"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Signing material comes from the environment, never from the repository: no
    // keystore and no password is ever committed. When the variables are absent the
    // release build simply stays unsigned, so cloning and building locally still
    // works without anyone having to hold the key.
    val keystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            manifestPlaceholders["usesCleartext"] = "true"
        }
        getByName("release") {
            isMinifyEnabled = false
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}
