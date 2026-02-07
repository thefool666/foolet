plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // AGP 8.0+ 必须显式声明 namespace
    namespace = "com.fool.et"
    
    // 必须是 34，因为 Manifest 里用到了 Android 14 的新权限
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fool.et"
        
        // 最低兼容 Android 7.0
        minSdk = 24
        
        // 目标 SDK 设为 34 (Android 14)
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
        // AGP 8.0+ 必须使用 Java 17
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // 核心 KTX 扩展库
    implementation("androidx.core:core-ktx:1.12.0")
    
    // AppCompat (你的 Manifest 用到了 Theme.AppCompat)
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Material Design (按钮、文本样式)
    implementation("com.google.android.material:material:1.11.0")
    
    // 约束布局 (你的 activity_main.xml 使用了 ConstraintLayout)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
