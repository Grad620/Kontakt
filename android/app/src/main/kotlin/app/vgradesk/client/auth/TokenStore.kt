package app.vgradesk.client.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Хранит токен доступа в зашифрованном виде: AES-256-GCM, ключ лежит в Android Keystore
 * и не покидает устройство. В открытом виде токен на диск не пишется и в логи не попадает.
 */
class TokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("vg_secure", Context.MODE_PRIVATE)

    fun save(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val blob = cipher.iv + encrypted
        prefs.edit().putString(KEY_TOKEN, Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
    }

    fun load(): String? {
        val stored = prefs.getString(KEY_TOKEN, null) ?: return null
        return try {
            val blob = Base64.decode(stored, Base64.NO_WRAP)
            val iv = blob.copyOfRange(0, IV_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
            }
            String(cipher.doFinal(blob, IV_SIZE, blob.size - IV_SIZE), Charsets.UTF_8)
        } catch (e: Exception) {
            // Ключ потерян или данные повреждены — безопаснее выйти, чем работать с мусором.
            clear()
            null
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
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
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
