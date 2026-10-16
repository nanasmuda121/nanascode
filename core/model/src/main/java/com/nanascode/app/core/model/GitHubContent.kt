package com.nanascode.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubBranch(
    val name: String = "",
    val commit: BranchCommit? = null,
    val `protected`: Boolean = false
)

@Serializable
data class BranchCommit(
    val sha: String = "",
    val url: String = ""
)

@Serializable
data class GitHubContentItem(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val size: Long = 0L,
    val type: String = "file", // "dir" or "file"
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null
) {
    val isDirectory: Boolean
        get() = type.equals("dir", ignoreCase = true)

    val formattedSize: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", size / 1024.0)
            else -> String.format(java.util.Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
        }
}

@Serializable
data class GitHubApiError(
    val message: String = "",
    @SerialName("documentation_url") val documentationUrl: String? = null
)
