plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.christmasdream.launcher"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.christmasdream.launcher"
        minSdk = 23
        targetSdk = 28
        versionCode = 3
        versionName = "1.2.0"
    }
    flavorDimensions += "device"
    productFlavors {
        create("phone") { dimension = "device" }
        create("tv") {
            dimension = "device"
            applicationIdSuffix = ".tv"
            versionNameSuffix = "-tv"
            minSdk = 26
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { jniLibs { useLegacyPackaging = true } }
}
