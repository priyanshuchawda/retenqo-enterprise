plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

// Redmi / iQOO Snapdragon phones: arm64-v8a only (faster build, smaller APK).
subprojects {
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.gradle.BaseExtension>("android") {
            defaultConfig {
                ndk {
                    abiFilters.clear()
                    abiFilters.add("arm64-v8a")
                }
            }
        }
    }
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.gradle.BaseExtension>("android") {
            defaultConfig {
                ndk {
                    abiFilters.clear()
                    abiFilters.add("arm64-v8a")
                }
            }
        }
    }
}
