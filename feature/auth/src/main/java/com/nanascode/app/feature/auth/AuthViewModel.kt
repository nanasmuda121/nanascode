package com.nanascode.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanascode.app.core.common.DefaultDispatcherProvider
import com.nanascode.app.core.common.DispatcherProvider
import com.nanascode.app.core.database.UserDao
import com.nanascode.app.core.database.UserEntity
import com.nanascode.app.core.model.GitHubToken
import com.nanascode.app.core.model.NetworkResult
import com.nanascode.app.core.network.GitHubRepository
import com.nanascode.app.core.security.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository,
    private val tokenManager: TokenManager,
    private val userDao: UserDao,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    private val _savedAccounts = MutableStateFlow<List<String>>(emptyList())
    val savedAccounts: StateFlow<List<String>> = _savedAccounts.asStateFlow()

    val rateLimitFlow = gitHubRepository.rateLimitFlow
    val lastScopeResultFlow = gitHubRepository.lastScopeResultFlow

    init {
        refreshAccountsList()
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val activeToken = tokenManager.getActiveToken()
        val activeUsername = tokenManager.getActiveUsername()
        if (activeToken != null && activeUsername != null) {
            viewModelScope.launch {
                val cachedUser = withContext(dispatcherProvider.io) {
                    userDao.getUserByLogin(activeUsername)
                }
                if (cachedUser != null) {
                    _uiState.value = AuthState.Authenticated(
                        user = cachedUser.toDomain(),
                        scopeResult = gitHubRepository.lastScopeResultFlow.value
                    )
                } else {
                    // Validasi ulang token di background
                    validateAndSaveToken(activeToken.rawValue, isSilent = true)
                }
            }
        }
    }

    fun login(rawToken: String) {
        val token = rawToken.trim()
        if (token.isBlank()) {
            _uiState.value = AuthState.Error("Token tidak boleh kosong")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            validateAndSaveToken(token, isSilent = false)
        }
    }

    private suspend fun validateAndSaveToken(tokenString: String, isSilent: Boolean) {
        val token = GitHubToken(tokenString)
        when (val result = gitHubRepository.validateToken(token)) {
            is NetworkResult.Success -> {
                val (user, scopeResult) = result.data
                // Simpan token terenkripsi di Keystore
                tokenManager.saveToken(user.login, token)
                // Cache user ke database lokal Room
                withContext(dispatcherProvider.io) {
                    userDao.insertUser(UserEntity.fromDomain(user))
                }
                refreshAccountsList()
                _uiState.value = AuthState.Authenticated(user, scopeResult)
            }
            is NetworkResult.Unauthorized -> {
                _uiState.value = AuthState.Error("Token tidak valid atau telah kedaluwarsa")
            }
            is NetworkResult.Error -> {
                _uiState.value = AuthState.Error(
                    message = result.message,
                    isRateLimit = result.isRateLimit,
                    resetSeconds = result.rateLimitInfo?.resetEpochSeconds
                )
            }
            is NetworkResult.NetworkUnavailable -> {
                _uiState.value = AuthState.Error("Tidak ada koneksi internet. Periksa jaringan Anda.")
            }
        }
    }

    fun switchAccount(username: String) {
        viewModelScope.launch {
            if (tokenManager.setActiveAccount(username)) {
                val cached = withContext(dispatcherProvider.io) {
                    userDao.getUserByLogin(username)
                }
                if (cached != null) {
                    _uiState.value = AuthState.Authenticated(
                        user = cached.toDomain(),
                        scopeResult = null
                    )
                }
                val token = tokenManager.getActiveToken()
                if (token != null) {
                    validateAndSaveToken(token.rawValue, isSilent = true)
                }
            }
        }
    }

    fun logoutCurrentAccount() {
        val currentUser = tokenManager.getActiveUsername()
        if (currentUser != null) {
            viewModelScope.launch {
                tokenManager.removeAccount(currentUser)
                withContext(dispatcherProvider.io) {
                    userDao.deleteUser(currentUser)
                }
                refreshAccountsList()
                _uiState.value = AuthState.Idle
                checkExistingSession()
            }
        } else {
            _uiState.value = AuthState.Idle
        }
    }

    fun logoutAll() {
        viewModelScope.launch {
            tokenManager.clearAll()
            withContext(dispatcherProvider.io) {
                userDao.clearAll()
            }
            refreshAccountsList()
            _uiState.value = AuthState.Idle
        }
    }

    private fun refreshAccountsList() {
        _savedAccounts.value = tokenManager.getAllAccounts()
    }

    fun resetState() {
        _uiState.value = AuthState.Idle
    }
}
