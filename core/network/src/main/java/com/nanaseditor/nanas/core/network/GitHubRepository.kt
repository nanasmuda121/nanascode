package com.nanaseditor.nanas.core.network

import com.nanaseditor.nanas.core.model.CreateRepoRequest
import com.nanaseditor.nanas.core.model.GitHubRepo
import com.nanaseditor.nanas.core.model.GitHubScopeParser
import com.nanaseditor.nanas.core.model.GitHubToken
import com.nanaseditor.nanas.core.model.GitHubUser
import com.nanaseditor.nanas.core.model.NetworkResult
import com.nanaseditor.nanas.core.model.RateLimitInfo
import com.nanaseditor.nanas.core.model.RepoFilterType
import com.nanaseditor.nanas.core.model.RepoSortType
import com.nanaseditor.nanas.core.model.ScopeCheckResult
import kotlinx.coroutines.flow.StateFlow

interface GitHubRepository {
    val rateLimitFlow: StateFlow<RateLimitInfo?>
    val lastScopeResultFlow: StateFlow<ScopeCheckResult?>

    suspend fun validateToken(token: GitHubToken): NetworkResult<Pair<GitHubUser, ScopeCheckResult>>
    suspend fun getUserRepositories(
        filter: RepoFilterType = RepoFilterType.ALL,
        sort: RepoSortType = RepoSortType.UPDATED,
        page: Int = 1
    ): NetworkResult<List<GitHubRepo>>
    suspend fun createRepository(request: CreateRepoRequest): NetworkResult<GitHubRepo>
    suspend fun refreshRateLimit(): NetworkResult<RateLimitInfo>
}
