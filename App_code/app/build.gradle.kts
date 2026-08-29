plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    kotlin("plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.teapink.waste_samaritan.aegionwealth"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.teapink.waste_samaritan.aegionwealth"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // google fonts
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // koin library
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    // Declare the koin dependencies without versions
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.core.coroutines)
    implementation(libs.koin.androidx.workmanager)

    // Works with test libraries too!
    testImplementation(libs.koin.test.junit4)
    testImplementation(libs.koin.android.test)

    // retrofit
    implementation(libs.retrofit)
    implementation (libs.gson)
    implementation (libs.converter.gson)

    // JSON serialization library, works with the Kotlin serialization plugin
    implementation(libs.kotlinx.serialization.json)

    // coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Jetpack Compose integration
    implementation(libs.androidx.navigation.compose)

    //icons
    implementation(libs.androidx.material.icons.extended)

    // datastore
    implementation(libs.androidx.datastore.preferences)



}