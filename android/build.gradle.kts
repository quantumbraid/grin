// GRIN Android Library - Root Build Configuration
// Grouped Readdressable Indexed Nodes - Android/Kotlin Implementation

plugins {
    id("com.android.library") version "8.2.0" apply false
    id("com.android.application") version "8.2.0" apply false
    kotlin("android") version "1.9.21" apply false
    id("de.mannodermaus.android-junit5") version "1.10.0.0" apply false
    id("org.jetbrains.dokka") version "1.9.20" apply false
}

allprojects {
    group = "io.grin"
    version = "0.1.0"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
