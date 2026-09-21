// Top-level build file — plugin versions declared here, applied per-module.
plugins {
    id("com.android.application") version "8.6.1" apply false
    // Pinned to match the Kotlin version org.maplibre.gl:android-sdk was
    // compiled with (its bundled .kotlin_module metadata requires it) --
    // Gradle's own bundled Kotlin (2.0.21) is too old and fails
    // compileDebugKotlin with an "incompatible version of Kotlin" error.
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
}
