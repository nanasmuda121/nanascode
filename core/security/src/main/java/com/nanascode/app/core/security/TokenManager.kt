package com.nanascode.app.core.security

import com.nanascode.app.core.model.GitHubToken
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface pengelola token autentikasi GitHub.
 * Mendukung penyimpanan multi-akun, enkripsi Keystore AES-GCM, dan mode offline (tanpa akun).
 */
interface TokenManager {
    val activeTokenFlow: StateFlow<GitHubToken?>
    fun saveToken(username: String, token: GitHubToken)
    fun getActiveToken(): GitHubToken?
    fun getActiveUsername(): String?
    fun getAllAccounts(): List<String>
    fun setActiveAccount(username: String): Boolean
    fun removeAccount(username: String)
    fun clearAll()
}
