// GRIN Android Demo Application
// Demonstrates GRIN file loading, validation, and playback

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "io.grin.demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.grin.demo"
        minSdk = 21  // Match library minimum
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // GRIN library
    implementation(project(":lib"))
    
    // AndroidX core - stable, minimal
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // Material Design for demo UI
    implementation("com.google.android.material:material:1.11.0")
    
    // ConstraintLayout for demo layouts
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Lifecycle for demo
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
