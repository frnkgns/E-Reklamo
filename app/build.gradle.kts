plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    kotlin("plugin.serialization") version "1.9.10"
}

android {
    namespace = "com.example.e_reklamo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.e_reklamo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth.ktx)

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.15.0")
    implementation(libs.androidx.ui.graphics.android)
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.0")

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Supabase SDK
    implementation("io.github.jan-tennert.supabase:storage-kt:3.0.2")
    implementation("io.ktor:ktor-client-android:3.0.0")
}
