package com.nanascode.app.core.security

interface CryptoCipher {
    /**
     * Mengenkripsi teks biasa menjadi Base64 ciphertext (termasuk IV & tag autentikasi GCM).
     */
    fun encrypt(plainText: String): String

    /**
     * Mendekripsi Base64 ciphertext menjadi teks asli.
     * Melempar Exception bila integritas data rusak atau tag autentikasi tidak valid.
     */
    fun decrypt(cipherTextBase64: String): String
}
