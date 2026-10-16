package com.nanascode.app.core.network

import com.nanascode.app.core.model.CreateRepoRequest
import com.nanascode.app.core.model.GitHubRepo
import com.nanascode.app.core.model.GitHubUser
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") authHeader: String? = null
    ): Response<GitHubUser>

    @GET("user/repos")
    suspend fun getUserRepositories(
        @Header("Authorization") authHeader: String? = null,
        @Query("visibility") visibility: String? = null,
        @Query("affiliation") affiliation: String? = null,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): Response<List<GitHubRepo>>

    @POST("user/repos")
    suspend fun createRepository(
        @Body request: CreateRepoRequest
    ): Response<GitHubRepo>

    @GET("rate_limit")
    suspend fun getRateLimit(): Response<RateLimitResponse>

    @GET("repos/{owner}/{repo}/branches")
    suspend fun getBranches(
        @retrofit2.http.Path("owner") owner: String,
        @retrofit2.http.Path("repo") repo: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<List<com.nanascode.app.core.model.GitHubBranch>>

    @GET("repos/{owner}/{repo}/contents")
    suspend fun getRootContents(
        @retrofit2.http.Path("owner") owner: String,
        @retrofit2.http.Path("repo") repo: String,
        @Query("ref") ref: String? = null,
        @Header("Authorization") authHeader: String? = null
    ): Response<List<com.nanascode.app.core.model.GitHubContentItem>>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getPathContents(
        @retrofit2.http.Path("owner") owner: String,
        @retrofit2.http.Path("repo") repo: String,
        @retrofit2.http.Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String? = null,
        @Header("Authorization") authHeader: String? = null
    ): Response<List<com.nanascode.app.core.model.GitHubContentItem>>
}

@kotlinx.serialization.Serializable
data class RateLimitResponse(
    val resources: RateLimitResources
)

@kotlinx.serialization.Serializable
data class RateLimitResources(
    val core: RateLimitCore
)

@kotlinx.serialization.Serializable
data class RateLimitCore(
    val limit: Int,
    val remaining: Int,
    val reset: Long
)
