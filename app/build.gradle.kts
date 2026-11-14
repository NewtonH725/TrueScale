plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.truescale"
    compileSdk = 37

    // Protocollo di versionamento semantico True Scale (vX.Y.Z):
    // - X (versionMajor): nuove funzionalita' architetturali o cambi di paradigma
    // - Y (versionMinor): upgrade sostanziosi, nuovi strumenti metrologici o calibrazioni
    // - Z (versionPatch): correzioni di bug, rifiniture e ottimizzazioni minori
    val versionMajor = 1
    val versionMinor = 3
    val versionPatch = 2

    defaultConfig {
        applicationId = "com.example.truescale"
        minSdk = 35
        targetSdk = 37
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch // 10000 per v1.0.0
        versionName = "v$versionMajor.$versionMinor.$versionPatch" // "v1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
