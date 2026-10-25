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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Официальный репозиторий VK ID SDK (с 2.7.3 SDK и Captcha SDK лежат здесь).
        maven("https://nexus-external.vkteam.ru/repository/maven/")
        // Прежние адреса оставлены резервными: именно с ними VK ID 2.7.3 собирался в CI.
        maven("https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-android/")
        maven("https://artifactory-external.vkpartner.ru/artifactory/maven/")
        maven("https://artifactory-external.vkpartner.ru/artifactory/vk-id-captcha/android/")
    }
}

rootProject.name = "Kontakt"
include(":app")
