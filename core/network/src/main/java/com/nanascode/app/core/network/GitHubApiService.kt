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
        @Query("visibility") visibility: String = "all",
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member",
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
