package com.nanaseditor.nanas.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nanaseditor.nanas.core.common.SecureLogger
import com.nanaseditor.nanas.core.model.GitHubToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class EncryptedTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenManager {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "nanas_secure_tokens_pref",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            SecureLogger.e("Security", "Gagal inisialisasi EncryptedSharedPreferences, fallback standard", e)
            context.getSharedPreferences("nanas_secure_tokens_fallback", Context.MODE_PRIVATE)
        }
    }

    private val _activeTokenFlow = MutableStateFlow<GitHubToken?>(null)
    override val activeTokenFlow: StateFlow<GitHubToken?> = _activeTokenFlow.asStateFlow()

    init {
        _activeTokenFlow.value = getActiveToken()
    }

    override fun saveToken(username: String, token: GitHubToken) {
        val accounts = getAllAccounts().toMutableSet()
        accounts.add(username)

        prefs.edit()
            .putString(KEY_TOKEN_PREFIX + username, token.rawValue)
            .putStringSet(KEY_ALL_ACCOUNTS, accounts)
            .putString(KEY_ACTIVE_USER, username)
            .apply()

        _activeTokenFlow.value = token
        SecureLogger.i("Security", "Token tersimpan aman untuk akun: $username")
    }

    override fun getActiveToken(): GitHubToken? {
        val activeUser = getActiveUsername() ?: return null
        val raw = prefs.getString(KEY_TOKEN_PREFIX + activeUser, null) ?: return null
        return GitHubToken(raw)
    }

    override fun getActiveUsername(): String? {
        return prefs.getString(KEY_ACTIVE_USER, null)
    }

    override fun getAllAccounts(): List<String> {
        val set = prefs.getStringSet(KEY_ALL_ACCOUNTS, emptySet()) ?: emptySet()
        return set.toList().sorted()
    }

    override fun setActiveAccount(username: String): Boolean {
        val tokenRaw = prefs.getString(KEY_TOKEN_PREFIX + username, null)
        return if (tokenRaw != null) {
            prefs.edit().putString(KEY_ACTIVE_USER, username).apply()
            _activeTokenFlow.value = GitHubToken(tokenRaw)
            true
        } else {
            false
        }
    }

    override fun removeAccount(username: String) {
        val accounts = getAllAccounts().toMutableSet()
        accounts.remove(username)

        val editor = prefs.edit()
            .remove(KEY_TOKEN_PREFIX + username)
            .putStringSet(KEY_ALL_ACCOUNTS, accounts)

        if (getActiveUsername() == username) {
            val nextUser = accounts.firstOrNull()
            if (nextUser != null) {
                editor.putString(KEY_ACTIVE_USER, nextUser)
                val raw = prefs.getString(KEY_TOKEN_PREFIX + nextUser, null)
                _activeTokenFlow.value = raw?.let { GitHubToken(it) }
            } else {
                editor.remove(KEY_ACTIVE_USER)
                _activeTokenFlow.value = null
            }
        }
        editor.apply()
    }

    override fun clearAll() {
        prefs.edit().clear().apply()
        _activeTokenFlow.value = null
        SecureLogger.i("Security", "Semua kredensial akun telah dihapus")
    }

    companion object {
        private const val KEY_TOKEN_PREFIX = "token_user_"
        private const val KEY_ACTIVE_USER = "active_github_user"
        private const val KEY_ALL_ACCOUNTS = "registered_accounts_set"
    }
}
