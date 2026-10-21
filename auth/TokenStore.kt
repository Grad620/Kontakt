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

/**
 * Stores the access token encrypted with AES-256-GCM. The key remains in Android Keystore.
 * If encrypted data or the Keystore key becomes unusable, the session is discarded safely:
 * tokens are never written to plaintext storage.
 */
class TokenStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs by lazy {
        appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun save(token: String) {
        require(token.isNotBlank()) { "Access token must not be blank" }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key())
            }
            val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            val blob = cipher.iv + encrypted
            prefs.edit().putString(KEY_TOKEN, Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
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
            val blob = Base64.decode(stored, Base64.NO_WRAP)
            if (blob.size <= IV_SIZE) {
                throw GeneralSecurityException("Stored token payload is truncated")
            }
            val iv = blob.copyOfRange(0, IV_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
            }
            String(cipher.doFinal(blob, IV_SIZE, blob.size - IV_SIZE), Charsets.UTF_8)
                .takeIf { it.isNotBlank() }
                ?: throw GeneralSecurityException("Stored token is blank")
        } catch (_: Exception) {
            // Missing, corrupted, or undecryptable session: discard it and let the user sign in again.
            resetAfterCryptoFailure()
            null
        }
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    /**
     * Best-effort cleanup only. Never falls back to plaintext storage and never masks the original
     * save error. Removing an unusable alias allows a later sign-in to create a fresh Keystore key.
     */
    private fun resetAfterCryptoFailure() {
        runCatching { prefs.edit().remove(KEY_TOKEN).apply() }
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
        const val PREF_FILE = "vg_secure"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
