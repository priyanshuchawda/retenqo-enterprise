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

rootProject.name = "MnemeTest"
include(":app")
include(":llama-lib")
include(":whisper-lib")

project(":llama-lib").projectDir =
    file("../../tools/llama.cpp/examples/llama.android/lib")
project(":whisper-lib").projectDir = file("whisper-lib")
