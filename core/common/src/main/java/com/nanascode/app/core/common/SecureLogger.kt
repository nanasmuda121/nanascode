package com.nanascode.app.core.common

import android.util.Log

object SecureLogger {
    private const val TAG = "NasCode"

    private val TOKEN_REGEX = Regex("(ghp_[a-zA-Z0-9]{20,}|github_pat_[a-zA-Z0-9_]{20,}|Bearer\\s+[a-zA-Z0-9_.-]+)")

    fun sanitize(message: String): String {
        return TOKEN_REGEX.replace(message) { matchResult ->
            val value = matchResult.value
            if (value.startsWith("Bearer ")) {
                "Bearer [REDACTED_TOKEN]"
            } else {
                "[REDACTED_TOKEN]"
            }
        }
    }

    fun d(tag: String = TAG, message: String) {
        try {
            Log.d(tag, sanitize(message))
        } catch (_: Throwable) {
            println("[$tag] ${sanitize(message)}")
        }
    }

    fun i(tag: String = TAG, message: String) {
        try {
            Log.i(tag, sanitize(message))
        } catch (_: Throwable) {
            println("[$tag] ${sanitize(message)}")
        }
    }

    fun w(tag: String = TAG, message: String, throwable: Throwable? = null) {
        try {
            Log.w(tag, sanitize(message), throwable)
        } catch (_: Throwable) {
            println("[$tag] WARN: ${sanitize(message)}")
        }
    }

    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, sanitize(message), throwable)
        } catch (_: Throwable) {
            println("[$tag] ERROR: ${sanitize(message)}")
        }
    }
}
