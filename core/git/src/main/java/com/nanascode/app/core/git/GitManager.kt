package com.nanascode.app.core.git

import java.io.File

data class GitStatusInfo(
    val branch: String,
    val modified: Set<String>,
    val untracked: Set<String>,
    val added: Set<String>,
    val removed: Set<String>,
    val isClean: Boolean
)

interface GitManager {
    fun initRepository(directory: File): Result<Unit>
    fun cloneRepository(remoteUrl: String, destinationDir: File, token: String? = null): Result<Unit>
    fun commitAll(directory: File, message: String, authorName: String = "NanasCode User", authorEmail: String = "user@nanascode.app"): Result<String>
    fun getStatus(directory: File): Result<GitStatusInfo>
    fun getBranches(directory: File): Result<List<String>>
    fun addRemoteAndPush(directory: File, remoteUrl: String, token: String, branch: String = "main"): Result<Unit>
}
