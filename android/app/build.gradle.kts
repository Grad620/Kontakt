import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// ID классического приложения VK (тип Standalone) берётся из VK_STANDALONE_APP_ID.
// Не используем VK ID client_secret в Android APK: для входа применяется официальный VK Android SDK.
fun config(name: String): String =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orElse("").get().trim()

val vkStandaloneAppId = config("VK_STANDALONE_APP_ID")

// Release signing credentials are supplied only by CI secrets or local environment.
// Never commit the signing keystore or passwords.
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

        buildConfigField("String", "VK_STANDALONE_APP_ID", "\"$vkStandaloneAppId\"")
        buildConfigField("boolean", "VK_ID_CONFIGURED", (vkStandaloneAppId.toIntOrNull()?.let { it > 0 } == true).toString())
        // The classic VK Android SDK reads this resource; no client secret is embedded in the APK.
        resValue("integer", "com_vk_sdk_AppId", vkStandaloneAppId.ifBlank { "0" })
    }

    // Keep the existing debug signing identity temporarily so test APKs can update the
    // already-installed app. Migrate this key to a GitHub Actions secret before removing it
    // from version control; changing it now would force an uninstall/reinstall on the phone.
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

        // Phone-installable QA build: normal (non-test-only) APK, signed with the
        // repository's stable debug key so successive test builds can update each other.
        // This is NOT a production release and must never be published to stores.
        create("installable") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += listOf("release")
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

    // Официальный классический VK Android SDK (Standalone app login).
    // Required by TokenStore for the VK SDK key-value storage contract.
    implementation("com.vk:android-sdk-core:4.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
