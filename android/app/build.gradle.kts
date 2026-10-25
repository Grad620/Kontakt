import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun config(name: String): String =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orElse("").get().trim()

// ID приложения VK (client_id из кабинета VK ID) — не секрет. Имя VK_STANDALONE_APP_ID оставлено, потому что
// именно оно уже задано в GitHub Actions и проверяется в CI; VK_ID_CLIENT_ID и VK_CLIENT_ID — равноправные псевдонимы.
val vkClientId = config("VK_ID_CLIENT_ID").ifBlank { config("VK_CLIENT_ID") }.ifBlank { config("VK_STANDALONE_APP_ID") }
// Права доступа должны быть подмножеством прав, включённых для приложения в кабинете VK ID, иначе VK отклонит
// запрос на вход. Поэтому по умолчанию просим только базовое право (пустой список).
val vkScopes = config("VK_SCOPES")

val signingKeyBase64 = config("ANDROID_SIGNING_KEY_BASE64")
val signingStorePassword = config("ANDROID_SIGNING_STORE_PASSWORD")
val signingKeyAlias = config("ANDROID_SIGNING_KEY_ALIAS")
val signingKeyPassword = config("ANDROID_SIGNING_KEY_PASSWORD")
val releaseKeystore = layout.buildDirectory.file("kontakt-release.keystore").get().asFile
if (signingKeyBase64.isNotBlank()) {
    releaseKeystore.parentFile.mkdirs()
    releaseKeystore.writeBytes(Base64.getDecoder().decode(signingKeyBase64))
}

android {
    namespace = "app.kontakt.client"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kontakt.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 6
        versionName = "0.1.5"

        val vkConfigured = vkClientId.matches(Regex("[1-9][0-9]*"))
        buildConfigField("String", "VK_CLIENT_ID", "\"$vkClientId\"")
        buildConfigField("String", "VK_SCOPES", "\"$vkScopes\"")
        buildConfigField("boolean", "VK_ID_CONFIGURED", vkConfigured.toString())

        // Обязательные подстановки VK ID SDK: ID приложения и схема возврата vk{ID}://vk.ru/blank.html.
        // Защищённый ключ (client_secret) в APK НЕ кладётся: обмен кода на токен идёт по PKCE и ключа не требует
        // (проверено по исходникам VK ID SDK 2.7.3), а ключ внутри публично скачиваемого APK перестал бы быть секретом.
        // SDK требует непустую строку; ключ нужен ему только для статистики и необязательного поиска провайдеров
        // тихого входа, при отказе сервера SDK подставляет встроенный список провайдеров.
        manifestPlaceholders["VKIDClientID"] = if (vkConfigured) vkClientId else "0"
        manifestPlaceholders["VKIDClientSecret"] = "not-embedded"
        manifestPlaceholders["VKIDRedirectHost"] = "vk.ru"
        manifestPlaceholders["VKIDRedirectScheme"] = "vk${if (vkConfigured) vkClientId else "0"}"
        // Короткий хэш коммита, из которого собран APK (в GitHub Actions это GITHUB_SHA). Не секрет.
        // Показывается в «О приложении», чтобы на телефоне можно было сверить версию с последним коммитом.
        buildConfigField("String", "BUILD_COMMIT", "\"${config("GITHUB_SHA").take(7).ifBlank { "local" }}\"")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (signingKeyBase64.isNotBlank()) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (signingKeyBase64.isNotBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Официальный VK ID SDK (OAuth 2.1 + PKCE). Репозиторий: nexus-external.vkteam.ru, см. settings.gradle.kts.
    implementation("com.vk.id:vkid:2.7.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
