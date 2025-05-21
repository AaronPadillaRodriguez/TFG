plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services) apply true
}

// Método alternativo más simple sin usar Properties ni FileUtils
val localPropertiesFile = rootProject.file("local.properties")
val apiKey = if (localPropertiesFile.exists()) {
    localPropertiesFile.readLines()
        .firstOrNull { it.startsWith("API_KEY_TMDB=") }
        ?.substringAfter("API_KEY_TMDB=")
        ?: ""
} else {
    ""
}

android {
    namespace = "com.example.tfg"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tfg"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Usar la API key cargada
        buildConfigField(
            "String",
            "API_KEY_TMDB",
            "\"$apiKey\""
        )

        // Para debug
        println("API Key loaded: ${if (apiKey.isNotEmpty()) "YES (length: ${apiKey.length})" else "NO - EMPTY"}")
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.com.google.gms.google.services.gradle.plugin)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.picasso)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.picasso.transformations)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}