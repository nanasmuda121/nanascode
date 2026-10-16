package com.nanaseditor.nanas.core.network

import com.nanaseditor.nanas.core.model.RateLimitInfo
import com.nanaseditor.nanas.core.model.ScopeCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStateHolder @Inject constructor() {
    private val _rateLimitFlow = MutableStateFlow<RateLimitInfo?>(null)
    val rateLimitFlow: StateFlow<RateLimitInfo?> = _rateLimitFlow.asStateFlow()

    private val _lastScopeResultFlow = MutableStateFlow<ScopeCheckResult?>(null)
    val lastScopeResultFlow: StateFlow<ScopeCheckResult?> = _lastScopeResultFlow.asStateFlow()

    fun updateRateLimit(rateLimit: RateLimitInfo) {
        _rateLimitFlow.value = rateLimit
    }

    fun updateScopeResult(scopeResult: ScopeCheckResult) {
        _lastScopeResultFlow.value = scopeResult
    }
}
