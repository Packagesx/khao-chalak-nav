// Top-level build file — plugin versions declared here, applied per-module.
plugins {
    id("com.android.application") version "8.6.1" apply false
    // Pinned to match the Kotlin version org.maplibre.gl:android-sdk was
    // compiled with (its bundled .kotlin_module metadata requires it) --
    // Gradle's own bundled Kotlin (2.0.21) is too old and fails
    // compileDebugKotlin with an "incompatible version of Kotlin" error.
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    // Milestone 3: Room (local GPS-recording persistence) needs an
    // annotation processor. Tried kapt first, but kapt reads Kotlin's own
    // compiled metadata via an older bundled kotlinx-metadata-jvm inside
    // Room's processor, which can't parse the metadata format our pinned
    // Kotlin 2.2.10 writes ("Provided Metadata instance has version 2.2.0,
    // while maximum supported version is 2.0.0") -- KSP processes Kotlin
    // symbols directly instead of reading that metadata, sidestepping the
    // mismatch entirely, and is what Room's own docs recommend over kapt
    // now anyway. Version pinned to match Kotlin 2.2.10 exactly (KSP
    // releases are versioned "<kotlin-version>-<ksp-version>").
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}
