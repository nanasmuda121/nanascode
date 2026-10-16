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

    fun openRepoExplorer(repo: com.nanascode.app.core.model.GitHubRepo) {
        val defaultBranch = repo.defaultBranch?.takeIf { it.isNotBlank() } ?: "main"
        _uiState.update {
            it.copy(
                explorerState = RepoExplorerState(
                    isOpen = true,
                    repo = repo,
                    selectedBranch = defaultBranch,
                    currentPath = "",
                    isLoadingBranches = true,
                    isLoadingContents = true,
                    isRepoEmpty = false,
                    errorMessage = null
                )
            )
        }

        val owner = repo.owner?.login?.takeIf { it.isNotBlank() }
            ?: repo.fullName.split("/").firstOrNull()
            ?: getActiveUsername()
            ?: ""

        loadBranches(owner, repo.name, defaultBranch)
        loadContents(owner, repo.name, path = "", branch = defaultBranch)
    }

    fun selectBranch(branch: String) {
        val current = _uiState.value.explorerState
        val repo = current.repo ?: return
        val owner = repo.owner?.login?.takeIf { it.isNotBlank() }
            ?: repo.fullName.split("/").firstOrNull() ?: ""

        _uiState.update {
            it.copy(
                explorerState = current.copy(
                    selectedBranch = branch,
                    currentPath = "",
                    isLoadingContents = true,
                    errorMessage = null,
                    isRepoEmpty = false
                )
            )
        }

        loadContents(owner, repo.name, path = "", branch = branch)
    }

    fun navigateToFolder(path: String) {
        val current = _uiState.value.explorerState
        val repo = current.repo ?: return
        val owner = repo.owner?.login?.takeIf { it.isNotBlank() }
            ?: repo.fullName.split("/").firstOrNull() ?: ""

        _uiState.update {
            it.copy(
                explorerState = current.copy(
                    currentPath = path,
                    isLoadingContents = true,
                    errorMessage = null
                )
            )
        }

        loadContents(owner, repo.name, path = path, branch = current.selectedBranch)
    }

    fun navigateUp() {
        val current = _uiState.value.explorerState
        if (current.currentPath.isBlank()) return
        val segments = current.pathSegments
        val parentPath = if (segments.size <= 1) "" else segments.dropLast(1).joinToString("/")
        navigateToFolder(parentPath)
    }

    fun refreshCurrentFolder() {
        val current = _uiState.value.explorerState
        val repo = current.repo ?: return
        val owner = repo.owner?.login?.takeIf { it.isNotBlank() }
            ?: repo.fullName.split("/").firstOrNull() ?: ""
        loadContents(owner, repo.name, current.currentPath, current.selectedBranch)
    }

    fun closeRepoExplorer() {
        _uiState.update {
            it.copy(explorerState = it.explorerState.copy(isOpen = false))
        }
    }

    private fun loadBranches(owner: String, repoName: String, currentBranch: String) {
        viewModelScope.launch {
            when (val result = gitHubRepository.getBranches(owner, repoName)) {
                is NetworkResult.Success -> {
                    val branchList = result.data
                    val activeBranch = if (branchList.any { it.name == currentBranch }) {
                        currentBranch
                    } else {
                        branchList.firstOrNull()?.name ?: currentBranch
                    }
                    _uiState.update {
                        it.copy(
                            explorerState = it.explorerState.copy(
                                branches = branchList,
                                selectedBranch = activeBranch,
                                isLoadingBranches = false
                            )
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            explorerState = it.explorerState.copy(
                                isLoadingBranches = false,
                                isRepoEmpty = result.code == 409 || result.code == 404,
                                errorMessage = if (result.code == 409 || result.code == 404) null else result.message
                            )
                        )
                    }
                }
                is NetworkResult.Unauthorized -> {
                    _uiState.update {
                        it.copy(
                            explorerState = it.explorerState.copy(
                                isLoadingBranches = false,
                                errorMessage = "HTTP 401: Token tidak valid atau kedaluwarsa"
                            )
                        )
                    }
                }
                is NetworkResult.NetworkUnavailable -> {
                    _uiState.update {
                        it.copy(
                            explorerState = it.explorerState.copy(
                                isLoadingBranches = false,
                                errorMessage = "Koneksi terputus. Periksa jaringan Anda."
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadContents(owner: String, repoName: String, path: String, branch: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(explorerState = it.explorerState.copy(isLoadingContents = true, errorMessage = null))
            }
            when (val result = gitHubRepository.getContents(owner, repoName, path, branch)) {
                is NetworkResult.Success -> {
                    val sorted = result.data.sortedWith(
                        compareByDescending<com.nanascode.app.core.model.GitHubContentItem> { it.isDirectory }
                            .thenBy { it.name.lowercase() }
                    )
                    _uiState.update {
                        it.copy(
                            explorerState = it.explorerState.copy(
                                contents = sorted,
                                isLoadingContents = false,
                                isRepoEmpty = sorted.isEmpty() && path.isEmpty(),
                                errorMessage = null
                            )
                        )
                    }
                }
                is NetworkResult.Error -> {
                    val isEmptyRepo = result.code == 409 ||
                            result.message.contains("empty", ignoreCase = true) ||
                            (result.code == 404 && path.isEmpty())
                    _uiState.update {
                        it.copy(
                            explorerState = it.explorerState.copy(
                                isLoadingContents = false,
                                isRepoEmpty = isEmptyRepo,
                                errorMessage = if (isEmptyRepo) null else result.message
                            )
                        )
                    }
                }
                is NetworkResult.Unauthorized -> {
                    _uiState.update {
                        it.copy(
                            explorerState = it.explorerState.copy(
                                isLoadingContents = false,
                                errorMessage = "HTTP 401: Token tidak valid atau kedaluwarsa"
                            )
                        )
                    }
                }
                is NetworkResult.NetworkUnavailable -> {
                    _uiState.update {
                        it.copy(
                            explorerState = it.explorerState.copy(
                                isLoadingContents = false,
                                errorMessage = "Koneksi terputus. Periksa koneksi internet Anda."
                            )
                        )
                    }
                }
            }
        }
    }
}
