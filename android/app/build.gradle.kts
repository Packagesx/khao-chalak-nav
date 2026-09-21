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
        versionCode = 2
        versionName = "0.2.0-milestone2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // Split per-ABI instead of shipping one "fat" APK with all four native
    // architectures' copies of libmaplibre.so -- that universal APK runs
    // ~55MB even in debug. arm64-v8a covers effectively all Android phones
    // sold since ~2017, so it's the one worth sideloading for hands-on
    // testing; a real Play Store release would instead use an .aab
    // (Play's App Bundle format) and let Play serve each device its own
    // ABI slice automatically.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
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
