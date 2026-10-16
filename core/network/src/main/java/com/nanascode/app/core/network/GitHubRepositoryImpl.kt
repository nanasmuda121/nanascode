package com.nanascode.app.core.network

import com.nanascode.app.core.common.SecureLogger
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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepositoryImpl @Inject constructor(
    private val apiService: GitHubApiService,
    private val networkStateHolder: NetworkStateHolder
) : GitHubRepository {

    override val rateLimitFlow: StateFlow<RateLimitInfo?> = networkStateHolder.rateLimitFlow
    override val lastScopeResultFlow: StateFlow<ScopeCheckResult?> = networkStateHolder.lastScopeResultFlow

    override suspend fun validateToken(token: GitHubToken): NetworkResult<Pair<GitHubUser, ScopeCheckResult>> {
        return try {
            val response = apiService.getAuthenticatedUser("Bearer ${token.rawValue}")
            val code = response.code()

            // Ekstrak info rate limit
            val headers = response.headers()
            val rateLimit = RateLimitInfo.fromHeaders(
                headers["x-ratelimit-limit"],
                headers["x-ratelimit-remaining"],
                headers["x-ratelimit-reset"]
            )?.also { networkStateHolder.updateRateLimit(it) }

            if (response.isSuccessful && response.body() != null) {
                val scopeHeader = headers["x-oauth-scopes"]
                val scopeResult = GitHubScopeParser.parseHeader(scopeHeader, token)
                networkStateHolder.updateScopeResult(scopeResult)
                NetworkResult.Success(Pair(response.body()!!, scopeResult), rateLimit)
            } else if (code == 401) {
                NetworkResult.Unauthorized("Token tidak valid atau telah kedaluwarsa")
            } else if (code == 403 || code == 429) {
                val isRateLimited = rateLimit?.isExhausted == true
                val msg = if (isRateLimited) {
                    "Batas permintaan GitHub (rate limit) telah tercapai. Reset pada ${rateLimit?.formattedResetTime()}."
                } else {
                    "Akses ditolak (HTTP 403): ${response.message()}"
                }
                NetworkResult.Error(code, msg, isRateLimit = isRateLimited, rateLimitInfo = rateLimit)
            } else {
                NetworkResult.Error(code, "Gagal validasi akun: ${response.message()}")
            }
        } catch (e: IOException) {
            SecureLogger.e("Network", "Koneksi terputus saat validasi token", e)
            NetworkResult.NetworkUnavailable
        } catch (e: Exception) {
            SecureLogger.e("Network", "Kesalahan tak terduga", e)
            NetworkResult.Error(message = e.localizedMessage ?: "Terjadi kesalahan sistem")
        }
    }

    override suspend fun getUserRepositories(
        filter: RepoFilterType,
        sort: RepoSortType,
        page: Int
    ): NetworkResult<List<GitHubRepo>> {
        return try {
            val response = apiService.getUserRepositories(
                visibility = "all",
                sort = sort.value,
                perPage = 100,
                page = page
            )
            val code = response.code()

            val repoHeaders = response.headers()
            val rateLimit = RateLimitInfo.fromHeaders(
                repoHeaders["x-ratelimit-limit"],
                repoHeaders["x-ratelimit-remaining"],
                repoHeaders["x-ratelimit-reset"]
            )?.also { networkStateHolder.updateRateLimit(it) }

            if (response.isSuccessful && response.body() != null) {
                val allRepos = response.body()!!
                val filteredRepos = when (filter) {
                    RepoFilterType.ALL -> allRepos
                    RepoFilterType.PUBLIC -> allRepos.filter { !it.isPrivate }
                    RepoFilterType.PRIVATE -> allRepos.filter { it.isPrivate }
                    RepoFilterType.FORK -> allRepos.filter { it.isFork }
                }
                NetworkResult.Success(filteredRepos, rateLimit)
            } else if (code == 401) {
                NetworkResult.Unauthorized()
            } else if (code == 403 || code == 429) {
                val isRateLimited = rateLimit?.isExhausted == true
                val msg = if (isRateLimited) {
                    "Rate limit tercapai. Reset pada ${rateLimit?.formattedResetTime()}."
                } else {
                    "Akses ditolak (HTTP 403)"
                }
                NetworkResult.Error(code, msg, isRateLimit = isRateLimited, rateLimitInfo = rateLimit)
            } else {
                NetworkResult.Error(code, "Gagal mengambil daftar repositori: ${response.message()}")
            }
        } catch (e: IOException) {
            SecureLogger.e("Network", "Koneksi terputus saat mengambil repo", e)
            NetworkResult.NetworkUnavailable
        } catch (e: Exception) {
            SecureLogger.e("Network", "Kesalahan saat memuat repositori", e)
            NetworkResult.Error(message = e.localizedMessage ?: "Terjadi kesalahan")
        }
    }

    override suspend fun createRepository(request: CreateRepoRequest): NetworkResult<GitHubRepo> {
        return try {
            val response = apiService.createRepository(request)
            val code = response.code()

            val createHeaders = response.headers()
            val rateLimit = RateLimitInfo.fromHeaders(
                createHeaders["x-ratelimit-limit"],
                createHeaders["x-ratelimit-remaining"],
                createHeaders["x-ratelimit-reset"]
            )?.also { networkStateHolder.updateRateLimit(it) }

            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!, rateLimit)
            } else if (code == 401) {
                NetworkResult.Unauthorized()
            } else if (code == 422) {
                NetworkResult.Error(code, "Nama repositori sudah digunakan atau tidak valid.")
            } else {
                NetworkResult.Error(code, "Gagal membuat repositori (${response.code()}): ${response.message()}")
            }
        } catch (e: IOException) {
            NetworkResult.NetworkUnavailable
        } catch (e: Exception) {
            NetworkResult.Error(message = e.localizedMessage ?: "Gagal membuat repositori")
        }
    }

    override suspend fun refreshRateLimit(): NetworkResult<RateLimitInfo> {
        return try {
            val response = apiService.getRateLimit()
            if (response.isSuccessful && response.body() != null) {
                val core = response.body()!!.resources.core
                val rateLimit = RateLimitInfo(core.limit, core.remaining, core.reset)
                networkStateHolder.updateRateLimit(rateLimit)
                NetworkResult.Success(rateLimit)
            } else {
                NetworkResult.Error(response.code(), "Gagal mengecek kuota rate limit")
            }
        } catch (e: Exception) {
            NetworkResult.Error(message = e.localizedMessage ?: "Gagal koneksi")
        }
    }
}
