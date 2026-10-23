package app.kontakt.client.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import com.vk.api.sdk.VKKeyValueStorage

/**
 * Stores the access token encrypted with AES-256-GCM. The key remains in Android Keystore.
 * If encrypted data or the Keystore key becomes unusable, the session is discarded safely:
 * tokens are never written to plaintext storage.
 */
class TokenStore(context: Context) : VKKeyValueStorage {
    private val appContext = context.applicationContext
    private val prefs by lazy {
        appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun save(token: String) {
        require(token.isNotBlank()) { "Access token must not be blank" }
        try {
            prefs.edit().putString(KEY_TOKEN, encrypt(token)).apply()
        } catch (e: Exception) {
            // A stale/invalidated Keystore key must not permanently block future sign-ins.
            resetAfterCryptoFailure()
            throw e
        }
    }

    @Synchronized
    fun load(): String? {
        return try {
            val stored = prefs.getString(KEY_TOKEN, null) ?: return null
            decrypt(stored).takeIf { it.isNotBlank() }
                ?: throw GeneralSecurityException("Stored token is blank")
        } catch (_: Exception) {
            // Missing, corrupted, or undecryptable session: discard it and let the user sign in again.
            resetAfterCryptoFailure()
            null
        }
    }

    @Synchronized
    fun clear() {
        val editor = prefs.edit().remove(KEY_TOKEN)
        prefs.all.keys.filter { it.startsWith(VK_PREFIX) }.forEach(editor::remove)
        editor.apply()
    }

    @Synchronized override fun put(key: String, value: String) {
        prefs.edit().putString(VK_PREFIX + key, encrypt(value)).apply()
    }
    @Synchronized override fun get(key: String): String? {
        val stored = prefs.getString(VK_PREFIX + key, null) ?: return null
        return try { decrypt(stored) } catch (_: Exception) {
            prefs.edit().remove(VK_PREFIX + key).apply()
            null
        }
    }
    @Synchronized override fun remove(key: String) {
        prefs.edit().remove(VK_PREFIX + key).apply()
    }
    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
    private fun decrypt(encoded: String): String {
        val blob = Base64.decode(encoded, Base64.NO_WRAP)
        if (blob.size <= IV_SIZE) throw GeneralSecurityException("Stored payload is truncated")
        val iv = blob.copyOfRange(0, IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv)) }
        return String(cipher.doFinal(blob, IV_SIZE, blob.size - IV_SIZE), Charsets.UTF_8)
    }

    /**
     * Best-effort cleanup only. Never falls back to plaintext storage and never masks the original
     * save error. Removing an unusable alias allows a later sign-in to create a fresh Keystore key.
     */
    private fun resetAfterCryptoFailure() {
        runCatching { clear() }
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "vg_token_key"
        const val KEY_TOKEN = "token"
        const val VK_PREFIX = "vk_sdk_"
        const val PREF_FILE = "vg_secure"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
