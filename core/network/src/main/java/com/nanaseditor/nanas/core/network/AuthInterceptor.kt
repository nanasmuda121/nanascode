package com.nanaseditor.nanas.core.network

import com.nanaseditor.nanas.core.model.GitHubScopeParser
import com.nanaseditor.nanas.core.model.RateLimitInfo
import com.nanaseditor.nanas.core.security.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val networkStateHolder: NetworkStateHolder
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")

        // Jika header Authorization belum ada secara manual, ambil token aktif
        val activeToken = tokenManager.getActiveToken()
        if (originalRequest.header("Authorization") == null && activeToken != null) {
            requestBuilder.header("Authorization", "Bearer ${activeToken.rawValue}")
        }

        val response = chain.proceed(requestBuilder.build())

        // Tangkap info Rate Limit dari header respon
        val limit = response.header("x-ratelimit-limit")
        val remaining = response.header("x-ratelimit-remaining")
        val reset = response.header("x-ratelimit-reset")

        val rateLimitInfo = RateLimitInfo.fromHeaders(limit, remaining, reset)
        if (rateLimitInfo != null) {
            networkStateHolder.updateRateLimit(rateLimitInfo)
        }

        // Tangkap OAuth Scopes jika ada
        val scopeHeader = response.header("x-oauth-scopes")
        if (scopeHeader != null && activeToken != null) {
            val scopeResult = GitHubScopeParser.parseHeader(scopeHeader, activeToken)
            networkStateHolder.updateScopeResult(scopeResult)
        }

        return response
    }
}
