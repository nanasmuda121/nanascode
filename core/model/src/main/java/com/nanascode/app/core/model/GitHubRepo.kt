package com.nanascode.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRepo(
    val id: Long = 0L,
    val name: String = "",
    @SerialName("full_name") val fullName: String = "",
    val description: String? = null,
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("fork") val isFork: Boolean = false,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("clone_url") val cloneUrl: String? = null,
    @SerialName("default_branch") val defaultBranch: String? = "main",
    val language: String? = null,
    @SerialName("stargazers_count") val stargazersCount: Int = 0,
    @SerialName("forks_count") val forksCount: Int = 0,
    @SerialName("open_issues_count") val openIssuesCount: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("pushed_at") val pushedAt: String? = null,
    val owner: RepoOwner? = null
)

@Serializable
data class RepoOwner(
    val id: Long = 0L,
    val login: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null
)
