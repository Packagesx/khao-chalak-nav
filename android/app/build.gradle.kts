plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.khaochalak.nav"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.khaochalak.nav"
        // minSdk 24 (Android 7.0, ~2016+) -- wide device coverage while still
        // giving us modern location/permission APIs without extra shims.
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-milestone1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // MapLibre Native Android SDK -- the native (non-WebView) equivalent of
    // the MapLibre GL JS map used in the web-frontend track (now archived).
    // See https://github.com/maplibre/maplibre-native
    implementation("org.maplibre.gl:android-sdk:13.6.1")
}
