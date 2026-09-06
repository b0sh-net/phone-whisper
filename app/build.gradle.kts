plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.util.Properties

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "net.b0sh.audiotext"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.b0sh.audiotext"
        minSdk = 30
        targetSdk = 36
        versionCode = 18
        versionName = "0.9.1"
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps["storeFile"]?.toString() ?: "none")
            storePassword = keystoreProps["storePassword"] as? String
            keyAlias = keystoreProps["keyAlias"] as? String
            keyPassword = keystoreProps["keyPassword"] as? String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    @Suppress("DEPRECATION")
    kotlinOptions { jvmTarget = "17" }

    testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation(files("libs/sherpa-onnx-1.12.39.aar"))

    testImplementation("junit:junit:4.13.2")
}
