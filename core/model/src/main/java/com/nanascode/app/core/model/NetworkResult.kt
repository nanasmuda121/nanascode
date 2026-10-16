package com.nanascode.app.core.model

sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T, val rateLimit: RateLimitInfo? = null) : NetworkResult<T>
    data class Error(
        val code: Int = -1,
        val message: String,
        val isRateLimit: Boolean = false,
        val rateLimitInfo: RateLimitInfo? = null
    ) : NetworkResult<Nothing>
    data class Unauthorized(val message: String = "Token tidak valid atau kedaluwarsa") : NetworkResult<Nothing>
    data object NetworkUnavailable : NetworkResult<Nothing>
}
