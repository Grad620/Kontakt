plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("vkid.manifest.placeholders")
}

// Значения VK берутся из свойств Gradle (-PVK_CLIENT_ID=...) или переменных окружения.
// В репозитории их нет. Без них проект всё равно собирается, но вход через VK ID будет недоступен.
fun config(name: String): String =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orElse("").get().trim()

val vkClientId = config("VK_CLIENT_ID")
val vkClientSecret = config("VK_CLIENT_SECRET")
val vkScopes = config("VK_SCOPES") // например: friends,wall,messages — только после одобрения VK

android {
    namespace = "com.kontakt.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kontakt.app"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Идентификатор приложения и список прав — не секреты. Секрет в BuildConfig не попадает.
        buildConfigField("String", "VK_CLIENT_ID", "\"$vkClientId\"")
        buildConfigField("String", "VK_SCOPES", "\"$vkScopes\"")
        // Do not expose the client secret in BuildConfig; only expose whether both values exist.
        buildConfigField("boolean", "VK_ID_CONFIGURED", (vkClientId.isNotBlank() && vkClientSecret.isNotBlank()).toString())

        // Требование VK ID SDK: значения подставляются в манифест.
        manifestPlaceholders["VKIDClientID"] = vkClientId.ifBlank { "0" }
        manifestPlaceholders["VKIDClientSecret"] = vkClientSecret.ifBlank { "unset" }
        manifestPlaceholders["VKIDRedirectHost"] = "vk.ru"
        manifestPlaceholders["VKIDRedirectScheme"] = "vk${vkClientId.ifBlank { "0" }}"
    }

    // Фиксированный debug-ключ (не секрет, пароль стандартный), чтобы SHA-1 отпечаток
    // был одинаковым на любой сборке и его можно было один раз указать в настройках VK.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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

    // Официальная авторизация VK ID (лицензия MIT)
    implementation("com.vk.id:vkid:2.0.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
