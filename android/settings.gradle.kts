pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://nexus-external.vkteam.ru/repository/maven/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Официальный актуальный репозиторий VK ID SDK
        maven("https://nexus-external.vkteam.ru/repository/maven/")
        // Старые репозитории оставлены как резервные
        maven("https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-android/")
        maven("https://artifactory-external.vkpartner.ru/artifactory/maven/")
        maven("https://artifactory-external.vkpartner.ru/artifactory/vk-id-captcha/android/")
    }
}

rootProject.name = "Kontakt"
include(":app")
