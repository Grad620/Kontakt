plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.21" apply false
    id("vkid.manifest.placeholders") version "1.1.0" apply true
}


val vkClientId = providers.gradleProperty("VK_CLIENT_ID")
    .orElse(providers.environmentVariable("VK_CLIENT_ID"))
    .orElse("")
    .get()
    .trim()
val vkClientSecret = providers.gradleProperty("VK_CLIENT_SECRET")
    .orElse(providers.environmentVariable("VK_CLIENT_SECRET"))
    .orElse("")
    .get()
    .trim()

// Official VK ID SDK manifest configuration. The redirect scheme is derived as vk{clientId}.
vkidManifestPlaceholders {
    init(
        clientId = vkClientId,
        clientSecret = vkClientSecret,
    )
}
