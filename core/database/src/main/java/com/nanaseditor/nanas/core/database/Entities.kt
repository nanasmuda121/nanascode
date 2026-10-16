package com.nanaseditor.nanas.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nanaseditor.nanas.core.model.GitHubRepo
import com.nanaseditor.nanas.core.model.GitHubUser
import com.nanaseditor.nanas.core.model.RepoOwner

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val login: String,
    val id: Long,
    val avatarUrl: String?,
    val name: String?,
    val company: String?,
    val email: String?,
    val bio: String?,
    val publicRepos: Int,
    val totalPrivateRepos: Int,
    val followers: Int,
    val following: Int,
    val createdAt: String?
) {
    fun toDomain(): GitHubUser = GitHubUser(
        id = id,
        login = login,
        avatarUrl = avatarUrl,
        name = name,
        company = company,
        email = email,
        bio = bio,
        publicRepos = publicRepos,
        totalPrivateRepos = totalPrivateRepos,
        followers = followers,
        following = following,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(user: GitHubUser): UserEntity = UserEntity(
            login = user.login,
            id = user.id,
            avatarUrl = user.avatarUrl,
            name = user.name,
            company = user.company,
            email = user.email,
            bio = user.bio,
            publicRepos = user.publicRepos,
            totalPrivateRepos = user.totalPrivateRepos,
            followers = user.followers,
            following = user.following,
            createdAt = user.createdAt
        )
    }
}

@Entity(tableName = "repos")
data class RepoEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val fullName: String,
    val description: String?,
    val isPrivate: Boolean,
    val isFork: Boolean,
    val htmlUrl: String?,
    val defaultBranch: String,
    val language: String?,
    val stargazersCount: Int,
    val forksCount: Int,
    val openIssuesCount: Int,
    val updatedAt: String?,
    val pushedAt: String?,
    val ownerLogin: String?,
    val ownerAvatar: String?
) {
    fun toDomain(): GitHubRepo = GitHubRepo(
        id = id,
        name = name,
        fullName = fullName,
        description = description,
        isPrivate = isPrivate,
        isFork = isFork,
        htmlUrl = htmlUrl,
        defaultBranch = defaultBranch,
        language = language,
        stargazersCount = stargazersCount,
        forksCount = forksCount,
        openIssuesCount = openIssuesCount,
        updatedAt = updatedAt,
        pushedAt = pushedAt,
        owner = ownerLogin?.let { RepoOwner(0, it, ownerAvatar) }
    )

    companion object {
        fun fromDomain(repo: GitHubRepo): RepoEntity = RepoEntity(
            id = repo.id,
            name = repo.name,
            fullName = repo.fullName,
            description = repo.description,
            isPrivate = repo.isPrivate,
            isFork = repo.isFork,
            htmlUrl = repo.htmlUrl,
            defaultBranch = repo.defaultBranch,
            language = repo.language,
            stargazersCount = repo.stargazersCount,
            forksCount = repo.forksCount,
            openIssuesCount = repo.openIssuesCount,
            updatedAt = repo.updatedAt,
            pushedAt = repo.pushedAt,
            ownerLogin = repo.owner?.login,
            ownerAvatar = repo.owner?.avatarUrl
        )
    }
}
