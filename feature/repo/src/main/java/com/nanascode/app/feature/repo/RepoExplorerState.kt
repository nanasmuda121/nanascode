package com.nanascode.app.feature.repo

import com.nanascode.app.core.model.GitHubBranch
import com.nanascode.app.core.model.GitHubContentItem
import com.nanascode.app.core.model.GitHubRepo

data class RepoExplorerState(
    val isOpen: Boolean = false,
    val repo: GitHubRepo? = null,
    val branches: List<GitHubBranch> = emptyList(),
    val selectedBranch: String = "main",
    val currentPath: String = "",
    val contents: List<GitHubContentItem> = emptyList(),
    val isLoadingBranches: Boolean = false,
    val isLoadingContents: Boolean = false,
    val isRepoEmpty: Boolean = false,
    val errorMessage: String? = null
) {
    val pathSegments: List<String>
        get() = if (currentPath.isBlank()) emptyList() else currentPath.split("/").filter { it.isNotBlank() }
}
