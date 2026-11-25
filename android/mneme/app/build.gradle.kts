import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val releaseKeyStore = providers.environmentVariable("RETENQO_RELEASE_KEYSTORE")
    .orElse(File(System.getProperty("user.home"), ".android/retenqo-release.p12").absolutePath)
    .get()
val releasePassword = providers.environmentVariable("RETENQO_RELEASE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("RETENQO_RELEASE_KEY_ALIAS").orElse("retenqo-release").get()
val releaseSigningConfigured = File(releaseKeyStore).isFile && !releasePassword.isNullOrBlank()

android {
    namespace = "com.mneme.app"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.mneme.app"
        minSdk = 33
        targetSdk = 36
        versionCode = 5
        versionName = "0.1.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseKeyStore)
                storePassword = releasePassword
                keyAlias = releaseKeyAlias
                keyPassword = releasePassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (releaseSigningConfigured) signingConfigs.getByName("release") else null
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }

}

tasks.configureEach {
    if (name == "preReleaseBuild") {
        doFirst {
            check(releaseSigningConfigured) {
                "Release signing is required. Run scripts/create-release-key.ps1 or provide RETENQO_RELEASE_KEYSTORE and RETENQO_RELEASE_PASSWORD."
            }
        }
    }
}

configurations.all {
    resolutionStrategy {
        // Match litertlm-android 0.16.x (SendChannel.close$default); avoid 1.10.x crash
        force(libs.kotlinx.coroutines.core.get().toString())
        force(libs.kotlinx.coroutines.android.get().toString())
    }
}

dependencies {
    implementation(project(":whisper-lib"))
    implementation(project(":llama-runtime"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.material)
    implementation(libs.mlkit.text.recognition)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.sqlite)
    implementation(libs.sqlcipher.android)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
