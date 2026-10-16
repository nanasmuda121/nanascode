package com.nanascode.app.core.network

import com.nanascode.app.core.model.GitHubApiError
import kotlinx.serialization.json.Json

object ApiErrorParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseErrorMessage(code: Int, rawBody: String?, responseMessage: String = ""): String {
        if (code == 409 || (rawBody != null && rawBody.contains("empty", ignoreCase = true))) {
            return "Repo kosong: Git Repository is empty"
        }
        val apiMsg = try {
            if (!rawBody.isNullOrBlank()) {
                json.decodeFromString<GitHubApiError>(rawBody).message
            } else null
        } catch (_: Exception) {
            null
        }

        val messageText = when {
            !apiMsg.isNullOrBlank() -> apiMsg
            responseMessage.isNotBlank() -> responseMessage
            !rawBody.isNullOrBlank() -> rawBody.take(120)
            else -> "Terjadi kesalahan pada server GitHub"
        }

        return "HTTP $code: $messageText"
    }
}
