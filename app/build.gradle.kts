plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.fool.et" // 这里的名字必须和代码包名完全一致
    compileSdk = 34 // 使用较新的 SDK 版本编译

    defaultConfig {
        applicationId = "com.fool.et"
        minSdk = 24 // Android 7.0+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    // Android 核心
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // 注意：我们不需要 Moshi、Wire 或 Protobuf，因为我们没用官方的 Manager 架构
}
