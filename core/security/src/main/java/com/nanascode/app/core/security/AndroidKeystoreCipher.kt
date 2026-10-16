package com.nanascode.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.nanascode.app.core.common.SecureLogger
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidKeystoreCipher @Inject constructor() : CryptoCipher {

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    } catch (e: Exception) {
        SecureLogger.w(TAG, "AndroidKeyStore tidak tersedia (lingkungan non-Android/JVM test). Menggunakan fallback memory key.")
        null
    }

    private var fallbackKey: SecretKey? = null

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        fallbackKey?.let { return it }

        if (keyStore == null) {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            val generated = keyGen.generateKey()
            fallbackKey = generated
            return generated
        }

        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val parameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(parameterSpec)
                return keyGenerator.generateKey()
            }

            val key = (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)
                ?: (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            if (key != null) return key
        } catch (e: Exception) {
            SecureLogger.w(TAG, "Gagal mengakses Android Keystore hardware key: ${e.message}. Menggunakan fallback.")
        }

        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val generated = keyGen.generateKey()
        fallbackKey = generated
        return generated
    }

    override fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv ?: throw IllegalStateException("IV tidak dihasilkan oleh cipher")
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Format data: [1 byte iv_len] + [iv bytes] + [cipher + gcm_tag bytes]
        val combined = ByteArray(1 + iv.size + cipherBytes.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(cipherBytes, 0, combined, 1 + iv.size, cipherBytes.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    override fun decrypt(cipherTextBase64: String): String {
        val combined = Base64.getDecoder().decode(cipherTextBase64)
        if (combined.isEmpty()) {
            throw IllegalArgumentException("Ciphertext tidak boleh kosong")
        }

        val ivLength = combined[0].toInt() and 0xFF
        if (combined.size < 1 + ivLength) {
            throw IllegalArgumentException("Panjang paket ciphertext tidak valid")
        }

        val iv = combined.copyOfRange(1, 1 + ivLength)
        val cipherBytes = combined.copyOfRange(1 + ivLength, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

        val decrypted = cipher.doFinal(cipherBytes)
        return String(decrypted, Charsets.UTF_8)
    }

    companion object {
        private const val TAG = "KeystoreCipher"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "nanas_master_token_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
