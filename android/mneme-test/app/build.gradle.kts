plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.mneme.test"
    compileSdk = 36

    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "com.mneme.test"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
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
    kotlin {
        jvmToolchain(17)
    }

    // llama.cpp and whisper.cpp both ship ggml shared libs
    packaging {
        jniLibs {
            // Required so ggml_backend_load_all_from_path() finds CPU backends on disk
            useLegacyPackaging = true
            pickFirsts += listOf(
                "lib/arm64-v8a/libggml-base.so",
                "lib/arm64-v8a/libggml-cpu.so",
                "lib/arm64-v8a/libggml.so",
                "**/libggml*.so",
            )
        }
    }
}

dependencies {
    implementation(project(":llama-lib"))
    implementation(project(":whisper-lib"))
    implementation(libs.bundles.androidx)
    implementation(libs.material)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.tensorflow.lite)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
