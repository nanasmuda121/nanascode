package com.nanascode.app.feature.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanascode.app.core.model.GitHubRepo
import com.nanascode.app.core.model.NetworkResult
import com.nanascode.app.core.model.ProjectSource
import com.nanascode.app.core.security.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PublishState {
    data object Idle : PublishState
    data object Loading : PublishState
    data class Success(val repo: GitHubRepo) : PublishState
    data class Error(val message: String) : PublishState
}

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    val workspaces: StateFlow<List<ProjectSource.LocalWorkspace>> =
        workspaceRepository.workspacesFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val activeToken = tokenManager.activeTokenFlow

    private val _publishState = MutableStateFlow<PublishState>(PublishState.Idle)
    val publishState: StateFlow<PublishState> = _publishState.asStateFlow()

    private val _showLoginRequiredDialog = MutableStateFlow(false)
    val showLoginRequiredDialog: StateFlow<Boolean> = _showLoginRequiredDialog.asStateFlow()

    init {
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        viewModelScope.launch {
            workspaceRepository.refreshWorkspaces()
        }
    }

    fun createWorkspace(name: String, templateId: String?) {
        viewModelScope.launch {
            workspaceRepository.createWorkspace(name, templateId)
        }
    }

    fun deleteWorkspace(id: String) {
        viewModelScope.launch {
            workspaceRepository.deleteWorkspace(id)
        }
    }

    fun renameWorkspace(id: String, newName: String) {
        viewModelScope.launch {
            workspaceRepository.renameWorkspace(id, newName)
        }
    }

    fun onPublishRequested(workspaceId: String, repoName: String, isPrivate: Boolean, description: String?) {
        val currentToken = tokenManager.getActiveToken()
        if (currentToken == null) {
            _showLoginRequiredDialog.value = true
            return
        }

        viewModelScope.launch {
            _publishState.value = PublishState.Loading
            workspaceRepository.publishToGitHub(workspaceId, repoName, isPrivate, description)
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> _publishState.value = PublishState.Success(result.data)
                        is NetworkResult.Error -> _publishState.value = PublishState.Error(result.message)
                        is NetworkResult.Unauthorized -> _publishState.value = PublishState.Error(result.message)
                        is NetworkResult.NetworkUnavailable -> _publishState.value = PublishState.Error("Tidak ada koneksi internet.")
                    }
                }
        }
    }

    fun dismissLoginRequiredDialog() {
        _showLoginRequiredDialog.value = false
    }

    fun resetPublishState() {
        _publishState.value = PublishState.Idle
    }

    fun getActiveUsername(): String? {
        return tokenManager.getActiveUsername()
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearAll()
            loadWorkspaces()
        }
    }
}
