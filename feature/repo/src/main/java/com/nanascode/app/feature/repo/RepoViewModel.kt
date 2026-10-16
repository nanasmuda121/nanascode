package com.nanascode.app.feature.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanascode.app.core.common.DispatcherProvider
import com.nanascode.app.core.database.RepoDao
import com.nanascode.app.core.database.RepoEntity
import com.nanascode.app.core.model.CreateRepoRequest
import com.nanascode.app.core.model.NetworkResult
import com.nanascode.app.core.model.RepoFilterType
import com.nanascode.app.core.model.RepoSortType
import com.nanascode.app.core.network.GitHubRepository
import com.nanascode.app.core.security.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RepoViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository,
    private val repoDao: RepoDao,
    private val tokenManager: TokenManager,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoListState())
    val uiState: StateFlow<RepoListState> = _uiState.asStateFlow()

    val rateLimitFlow = gitHubRepository.rateLimitFlow
    val activeToken = tokenManager.activeTokenFlow

    init {
        loadCachedRepos()
        if (tokenManager.getActiveToken() != null) {
            refreshRepos()
        }
    }

    private fun loadCachedRepos() {
        viewModelScope.launch {
            repoDao.getAllReposFlow().collect { cachedList ->
                if (cachedList.isNotEmpty() && _uiState.value.repos.isEmpty()) {
                    _uiState.update { it.copy(repos = cachedList.map { entity -> entity.toDomain() }) }
                }
            }
        }
    }

    fun getActiveUsername(): String? = tokenManager.getActiveUsername()

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearAll()
            withContext(dispatcherProvider.io) {
                repoDao.clearAll()
            }
            _uiState.update { it.copy(repos = emptyList(), errorMessage = null) }
        }
    }

    fun refreshRepos() {
        viewModelScope.launch {
            val token = tokenManager.getActiveToken()
            if (token == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Silakan masuk dengan akun GitHub untuk melihat daftar repositori."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = gitHubRepository.getUserRepositories(
                filter = _uiState.value.activeFilter,
                sort = _uiState.value.activeSort,
                page = 1
            )) {
                is NetworkResult.Success -> {
                    val repos = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            repos = repos,
                            rateLimitInfo = result.rateLimit
                        )
                    }
                    // Simpan ke cache Room secara async
                    withContext(dispatcherProvider.io) {
                        repoDao.insertRepos(repos.map { RepoEntity.fromDomain(it) })
                    }
                }
                is NetworkResult.Unauthorized -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Sesi login tidak valid atau token kedaluwarsa. Silakan masuk kembali."
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            rateLimitInfo = result.rateLimitInfo
                        )
                    }
                }
                is NetworkResult.NetworkUnavailable -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Tidak ada koneksi internet. Menampilkan repositori dari cache offline."
                        )
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilter(filter: RepoFilterType) {
        if (_uiState.value.activeFilter != filter) {
            _uiState.update { it.copy(activeFilter = filter) }
            if (tokenManager.getActiveToken() != null) {
                refreshRepos()
            }
        }
    }

    fun setSort(sort: RepoSortType) {
        if (_uiState.value.activeSort != sort) {
            _uiState.update { it.copy(activeSort = sort) }
            if (tokenManager.getActiveToken() != null) {
                refreshRepos()
            }
        }
    }

    fun createRepository(
        name: String,
        description: String?,
        isPrivate: Boolean,
        autoInit: Boolean,
        gitignoreTemplate: String?,
        licenseTemplate: String?
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama repositori tidak boleh kosong") }
            return
        }

        val token = tokenManager.getActiveToken()
        if (token == null) {
            _uiState.update { it.copy(errorMessage = "Login diperlukan untuk membuat repositori di GitHub.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingRepo = true, errorMessage = null) }

            val request = CreateRepoRequest(
                name = trimmedName,
                description = description?.takeIf { it.isNotBlank() },
                isPrivate = isPrivate,
                autoInit = autoInit,
                gitignoreTemplate = gitignoreTemplate?.takeIf { it.isNotBlank() },
                licenseTemplate = licenseTemplate?.takeIf { it.isNotBlank() }
            )

            when (val result = gitHubRepository.createRepository(request)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreatingRepo = false,
                            isCreateSuccess = true,
                            rateLimitInfo = result.rateLimit
                        )
                    }
                    // Segera refresh list
                    refreshRepos()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreatingRepo = false,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Unauthorized -> {
                    _uiState.update {
                        it.copy(
                            isCreatingRepo = false,
                            errorMessage = "Akses ditolak. Pastikan token memiliki scope 'repo'."
                        )
                    }
                }
                is NetworkResult.NetworkUnavailable -> {
                    _uiState.update {
                        it.copy(
                            isCreatingRepo = false,
                            errorMessage = "Gagal membuat repositori: Tidak ada jaringan."
                        )
                    }
                }
            }
        }
    }

    fun clearCreateSuccess() {
        _uiState.update { it.copy(isCreateSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
