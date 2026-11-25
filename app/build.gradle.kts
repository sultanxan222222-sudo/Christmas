plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.christmasdream.launcher"
    // Match the SDK level used by the TV-compatible Matrix build.
    compileSdk = 36
    defaultConfig {
        applicationId = "com.christmasdream.launcher"
        minSdk = 23
        targetSdk = 28
        versionCode = 2
        versionName = "1.1.0"
    }
    flavorDimensions += "device"
    productFlavors {
        create("phone") {
            dimension = "device"
        }
        create("tv") {
            dimension = "device"
            applicationIdSuffix = ".tv"
            versionNameSuffix = "-tv"
            // The TV build follows the same Android 8+ baseline as the proven Matrix TV app.
            minSdk = 26
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging {
        jniLibs {
            // Match the packaging mode used by the successfully installed Matrix TV APK.
            useLegacyPackaging = true
        }
    }
}
