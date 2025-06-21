plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.artificialinsightsllc.foulweather"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.artificialinsightsllc.foulweather"
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "2.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core for compatibility
    implementation(libs.androidx.core.ktx)
    // Lifecycle components for Composables
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Activity Compose integration
    implementation(libs.androidx.activity.compose)
    // BOM for Compose Material 3
    implementation(platform(libs.androidx.compose.bom))
    // UI elements for Compose
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    // Material 3 Design System
    implementation(libs.androidx.material3)

    // Navigation for Compose
    implementation("androidx.navigation:navigation-compose:2.8.0-beta01")

    // Glide for image loading (replacing Coil)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01") // For Glide with Compose
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")

    // OkHttp for network requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.11.0")

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")
    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Firebase Cloud Messaging (FCM)
    implementation ("com.google.firebase:firebase-messaging-ktx")

    // Firebase Storage
    implementation("com.google.firebase:firebase-storage-ktx")

    // Firebase Remote Config
    implementation("com.google.firebase:firebase-config-ktx")

    // Google Play Services Location (for Geocoding via Android's Geocoder or other location services)
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Material3 Extended Icon set
    implementation("androidx.compose.material:material-icons-extended")

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
