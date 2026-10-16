package com.nanaseditor.nanas.feature.repo

import com.nanaseditor.nanas.core.model.GitHubRepo
import com.nanaseditor.nanas.core.model.RateLimitInfo
import com.nanaseditor.nanas.core.model.RepoFilterType
import com.nanaseditor.nanas.core.model.RepoSortType

data class RepoListState(
    val isLoading: Boolean = false,
    val isCreatingRepo: Boolean = false,
    val repos: List<GitHubRepo> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: RepoFilterType = RepoFilterType.ALL,
    val activeSort: RepoSortType = RepoSortType.UPDATED,
    val errorMessage: String? = null,
    val rateLimitInfo: RateLimitInfo? = null,
    val isCreateSuccess: Boolean = false
) {
    val filteredRepos: List<GitHubRepo>
        get() {
            var list = repos
            if (searchQuery.isNotBlank()) {
                val query = searchQuery.trim().lowercase()
                list = list.filter {
                    it.name.lowercase().contains(query) ||
                    (it.description?.lowercase()?.contains(query) == true)
                }
            }
            return list
        }
}
