pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Retenqo"
include(":app")
include(":whisper-lib")
include(":llama-runtime")

project(":whisper-lib").projectDir = file("whisper-lib")
project(":llama-runtime").projectDir = file("../../tools/llama.cpp/examples/llama.android/lib")
