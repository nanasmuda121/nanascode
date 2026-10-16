package com.nanascode.app.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nanascode.app.core.common.DispatcherProvider
import com.nanascode.app.core.common.SecureLogger
import com.nanascode.app.core.model.GitHubToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

val Context.secureTokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "nanas_secure_keystore_tokens")

@Singleton
class DataStoreTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cipher: CryptoCipher,
    private val dispatcherProvider: DispatcherProvider
) : TokenManager {

    // Secondary constructor untuk unit testing
    constructor(
        context: Context,
        cipher: CryptoCipher,
        dispatcherProvider: DispatcherProvider,
        customDataStore: DataStore<Preferences>?
    ) : this(context, cipher, dispatcherProvider) {
        if (customDataStore != null) {
            this.testDataStore = customDataStore
        }
    }

    private var testDataStore: DataStore<Preferences>? = null
    private val dataStore: DataStore<Preferences>
        get() = testDataStore ?: context.secureTokenDataStore
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    private val _activeTokenFlow = MutableStateFlow<GitHubToken?>(null)
    override val activeTokenFlow: StateFlow<GitHubToken?> = _activeTokenFlow.asStateFlow()

    init {
        // Inisialisasi token aktif saat startup
        scope.launch {
            try {
                _activeTokenFlow.value = getActiveTokenInternal()
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Gagal memuat token saat inisialisasi", e)
                _activeTokenFlow.value = null
            }
        }
    }

    private suspend fun getActiveTokenInternal(): GitHubToken? {
        val prefs = dataStore.data.first()
        val activeUser = prefs[KEY_ACTIVE_USER] ?: return null
        val encryptedBase64 = prefs[stringPreferencesKey(KEY_TOKEN_PREFIX + activeUser)] ?: return null
        return try {
            val decryptedRaw = cipher.decrypt(encryptedBase64)
            GitHubToken(decryptedRaw)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Gagal mendekripsi token untuk user $activeUser", e)
            null
        }
    }

    override fun saveToken(username: String, token: GitHubToken) {
        runBlocking(dispatcherProvider.io) {
            try {
                val encryptedCiphertext = cipher.encrypt(token.rawValue)
                dataStore.edit { preferences ->
                    val currentAccounts = (preferences[KEY_ALL_ACCOUNTS] ?: emptySet()).toMutableSet()
                    currentAccounts.add(username)
                    preferences[KEY_ALL_ACCOUNTS] = currentAccounts
                    preferences[stringPreferencesKey(KEY_TOKEN_PREFIX + username)] = encryptedCiphertext
                    preferences[KEY_ACTIVE_USER] = username
                }
                _activeTokenFlow.value = token
                SecureLogger.i(TAG, "Token terenkripsi (AES-256-GCM) berhasil disimpan di DataStore untuk: $username")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Gagal menyimpan token ke DataStore", e)
                throw e
            }
        }
    }

    override fun getActiveToken(): GitHubToken? {
        _activeTokenFlow.value?.let { return it }
        return runBlocking(dispatcherProvider.io) {
            val token = getActiveTokenInternal()
            if (token != null) {
                _activeTokenFlow.value = token
            }
            token
        }
    }

    override fun getActiveUsername(): String? {
        return runBlocking(dispatcherProvider.io) {
            val prefs = dataStore.data.first()
            prefs[KEY_ACTIVE_USER]
        }
    }

    override fun getAllAccounts(): List<String> {
        return runBlocking(dispatcherProvider.io) {
            val prefs = dataStore.data.first()
            val set = prefs[KEY_ALL_ACCOUNTS] ?: emptySet()
            set.toList().sorted()
        }
    }

    override fun setActiveAccount(username: String): Boolean {
        return runBlocking(dispatcherProvider.io) {
            val prefs = dataStore.data.first()
            val encryptedBase64 = prefs[stringPreferencesKey(KEY_TOKEN_PREFIX + username)]
            if (encryptedBase64 != null) {
                try {
                    val decrypted = cipher.decrypt(encryptedBase64)
                    dataStore.edit { it[KEY_ACTIVE_USER] = username }
                    val token = GitHubToken(decrypted)
                    _activeTokenFlow.value = token
                    true
                } catch (e: Exception) {
                    SecureLogger.e(TAG, "Gagal beralih ke akun $username karena dekripsi error", e)
                    false
                }
            } else {
                false
            }
        }
    }

    override fun removeAccount(username: String) {
        runBlocking(dispatcherProvider.io) {
            dataStore.edit { preferences ->
                val currentAccounts = (preferences[KEY_ALL_ACCOUNTS] ?: emptySet()).toMutableSet()
                currentAccounts.remove(username)
                preferences[KEY_ALL_ACCOUNTS] = currentAccounts
                preferences.remove(stringPreferencesKey(KEY_TOKEN_PREFIX + username))

                if (preferences[KEY_ACTIVE_USER] == username) {
                    val nextUser = currentAccounts.firstOrNull()
                    if (nextUser != null) {
                        preferences[KEY_ACTIVE_USER] = nextUser
                        val nextEnc = preferences[stringPreferencesKey(KEY_TOKEN_PREFIX + nextUser)]
                        _activeTokenFlow.value = nextEnc?.let { GitHubToken(cipher.decrypt(it)) }
                    } else {
                        preferences.remove(KEY_ACTIVE_USER)
                        _activeTokenFlow.value = null
                    }
                }
            }
            SecureLogger.i(TAG, "Akun $username berhasil dihapus")
        }
    }

    override fun clearAll() {
        runBlocking(dispatcherProvider.io) {
            dataStore.edit { it.clear() }
            _activeTokenFlow.value = null
            SecureLogger.i(TAG, "Semua kredensial akun telah dibersihkan dari DataStore")
        }
    }

    companion object {
        private const val TAG = "DataStoreTokenStorage"
        private const val KEY_TOKEN_PREFIX = "secure_token_"
        private val KEY_ACTIVE_USER = stringPreferencesKey("active_github_username")
        private val KEY_ALL_ACCOUNTS = stringSetPreferencesKey("all_registered_accounts")
    }
}
