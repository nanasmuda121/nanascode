package com.nanascode.app.core.network

import com.nanascode.app.core.model.CreateRepoRequest
import com.nanascode.app.core.model.GitHubRepo
import com.nanascode.app.core.model.GitHubScopeParser
import com.nanascode.app.core.model.GitHubToken
import com.nanascode.app.core.model.GitHubUser
import com.nanascode.app.core.model.NetworkResult
import com.nanascode.app.core.model.RateLimitInfo
import com.nanascode.app.core.model.RepoFilterType
import com.nanascode.app.core.model.RepoSortType
import com.nanascode.app.core.model.ScopeCheckResult
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
