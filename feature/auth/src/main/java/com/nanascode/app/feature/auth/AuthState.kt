package com.nanascode.app.feature.auth

import com.nanascode.app.core.model.GitHubUser
import com.nanascode.app.core.model.ScopeCheckResult

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Authenticated(
        val user: GitHubUser,
        val scopeResult: ScopeCheckResult?
    ) : AuthState
    data class Error(
        val message: String,
        val isRateLimit: Boolean = false,
        val resetSeconds: Long? = null
    ) : AuthState
}
