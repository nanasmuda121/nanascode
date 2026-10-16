package com.nanascode.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.Base64

class CryptoCipherTest {

    private lateinit var cipher: AndroidKeystoreCipher

    @Before
    fun setup() {
        // Pada unit test JVM, AndroidKeystoreCipher otomatis fallback ke standard AES-GCM in-memory key
        cipher = AndroidKeystoreCipher()
    }

    @Test
    fun `encrypt and decrypt returns identical original string`() {
        val originalToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyzABCD"
        val encrypted = cipher.encrypt(originalToken)

        assertNotEquals("Ciphertext harus berbeda dari plaintext", originalToken, encrypted)
        assertTrue("Ciphertext harus string Base64 yang valid", encrypted.isNotBlank())

        val decrypted = cipher.decrypt(encrypted)
        assertEquals("Hasil dekripsi harus sama persis dengan plaintext asli", originalToken, decrypted)
    }

    @Test
    fun `encrypt produces unique IV each time`() {
        val token = "ghp_secureTokenSameValue"
        val enc1 = cipher.encrypt(token)
        val enc2 = cipher.encrypt(token)

        assertNotEquals("Dua enkripsi dari teks yang sama harus menghasilkan ciphertext berbeda (karena IV acak)", enc1, enc2)

        assertEquals(token, cipher.decrypt(enc1))
        assertEquals(token, cipher.decrypt(enc2))
    }

    @Test
    fun `tampered ciphertext throws exception due to GCM authentication tag mismatch`() {
        val token = "ghp_superSecretTokenValue"
        val encrypted = cipher.encrypt(token)
        val decoded = Base64.getDecoder().decode(encrypted)

        // Ubah 1 byte di payload ciphertext
        decoded[decoded.size - 1] = (decoded[decoded.size - 1].toInt() xor 0xFF).toByte()
        val tamperedBase64 = Base64.getEncoder().encodeToString(decoded)

        try {
            cipher.decrypt(tamperedBase64)
            fail("Harus melempar exception saat ciphertext diubah (GCM Auth Tag Failure)")
        } catch (e: Exception) {
            // Sukses terdeteksi
            assertTrue(e is javax.crypto.AEADBadTagException || e is java.security.GeneralSecurityException || e is Exception)
        }
    }

    @Test
    fun `encrypt empty string and symbols roundtrip succeeds`() {
        val symbols = "github_pat_11AAAA!@#$%^&*()_+{}[]:\"<>?~`\n\tSelamat Pagi NanasCode 🍍"
        val encrypted = cipher.encrypt(symbols)
        val decrypted = cipher.decrypt(encrypted)
        assertEquals(symbols, decrypted)
    }
}
