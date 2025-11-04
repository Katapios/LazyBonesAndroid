plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.katapandroid.lazybones.wear"
    // Используем тот же package для связи через Wearable API
    compileSdk = 34

    defaultConfig {
        // Используем тот же applicationId для связи через Wearable API
        // namespace остается разным для компиляции
        applicationId = "com.katapandroid.lazybones"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    // Wear OS
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear.compose:compose-material:1.2.1")
    implementation("androidx.wear.compose:compose-foundation:1.2.1")
    implementation("androidx.wear.compose:compose-navigation:1.2.1")
    
    // Watch Face API - исключаем activity-compose из транзитивных зависимостей
    implementation("androidx.wear.watchface:watchface:1.2.1") {
        exclude(group = "androidx.activity", module = "activity-compose")
    }
    implementation("androidx.wear.watchface:watchface-complications-data:1.2.1") {
        exclude(group = "androidx.activity", module = "activity-compose")
    }
    implementation("androidx.wear.watchface:watchface-complications-data-source:1.2.1") {
        exclude(group = "androidx.activity", module = "activity-compose")
    }
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1") {
        exclude(group = "androidx.activity", module = "activity-compose")
    }
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Pager for swipe navigation
    implementation("androidx.compose.foundation:foundation:1.5.4")
    
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Используем явную версию activity-compose вместо libs для избежания конфликтов
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Wearable Data Layer для синхронизации с телефоном
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    
    // Compose Tooling
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

