package com.nanascode.app.core.model

import kotlinx.serialization.Serializable

/**
 * Representasi sumber proyek di NanasCode.
 * Memisahkan proyek lokal (offline-first) dan remote GitHub sehingga editor & terminal
 * dapat beroperasi penuh tanpa bergantung pada login akun.
 */
@Serializable
sealed interface ProjectSource {
    val id: String
    val name: String
    val rootPath: String
    val description: String?

    @Serializable
    data class LocalWorkspace(
        override val id: String,
        override val name: String,
        override val rootPath: String,
        override val description: String? = null,
        val templateId: String? = null,
        val createdAt: Long = System.currentTimeMillis(),
        val lastModifiedAt: Long = System.currentTimeMillis(),
        val linkedGitHubRepo: String? = null
    ) : ProjectSource

    @Serializable
    data class GitHubRemote(
        override val id: String,
        override val name: String,
        override val rootPath: String,
        override val description: String? = null,
        val owner: String,
        val repo: String,
        val defaultBranch: String = "main",
        val isPrivate: Boolean = false,
        val cloneUrl: String? = null
    ) : ProjectSource
}
