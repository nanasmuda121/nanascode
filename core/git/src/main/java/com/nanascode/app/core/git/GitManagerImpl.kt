package com.nanascode.app.core.git

import com.nanascode.app.core.common.DispatcherProvider
import com.nanascode.app.core.common.SecureLogger
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitManagerImpl @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) : GitManager {

    override fun initRepository(directory: File): Result<Unit> = runCatching {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        Git.init().setDirectory(directory).call().use {
            SecureLogger.i(TAG, "Git repository berhasil diinisialisasi di: ${directory.absolutePath}")
        }
    }

    override fun cloneRepository(remoteUrl: String, destinationDir: File, token: String?): Result<Unit> = runCatching {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        val cloneCommand = Git.cloneRepository()
            .setURI(remoteUrl)
            .setDirectory(destinationDir)
            .setCloneAllBranches(true)

        if (!token.isNullOrBlank()) {
            cloneCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
        }

        cloneCommand.call().use {
            SecureLogger.i(TAG, "Clone berhasil dari $remoteUrl ke ${destinationDir.absolutePath}")
        }
    }

    override fun commitAll(
        directory: File,
        message: String,
        authorName: String,
        authorEmail: String
    ): Result<String> = runCatching {
        Git.open(directory).use { git ->
            // Stage all changes
            git.add().addFilepattern(".").call()
            // Also stage deleted files
            git.add().addFilepattern(".").setUpdate(true).call()

            val revCommit = git.commit()
                .setMessage(message)
                .setAuthor(authorName, authorEmail)
                .call()

            val commitId = revCommit.id.name
            SecureLogger.i(TAG, "Commit berhasil dibuat: $commitId - $message")
            commitId
        }
    }

    override fun getStatus(directory: File): Result<GitStatusInfo> = runCatching {
        Git.open(directory).use { git ->
            val status = git.status().call()
            val branch = git.repository.branch ?: "HEAD"
            GitStatusInfo(
                branch = branch,
                modified = status.modified,
                untracked = status.untracked,
                added = status.added,
                removed = status.removed,
                isClean = status.isClean
            )
        }
    }

    override fun getBranches(directory: File): Result<List<String>> = runCatching {
        Git.open(directory).use { git ->
            git.branchList().call().map { it.name.substringAfterLast("/") }
        }
    }

    override fun addRemoteAndPush(
        directory: File,
        remoteUrl: String,
        token: String,
        branch: String
    ): Result<Unit> = runCatching {
        Git.open(directory).use { git ->
            val config = git.repository.config
            config.setString("remote", "origin", "url", remoteUrl)
            config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
            config.save()

            val credentials = UsernamePasswordCredentialsProvider(token, "")
            git.push()
                .setRemote("origin")
                .setRefSpecs(RefSpec("refs/heads/$branch:refs/heads/$branch"))
                .setCredentialsProvider(credentials)
                .call()

            SecureLogger.i(TAG, "Push ke remote origin ($remoteUrl) berhasil!")
        }
    }

    companion object {
        private const val TAG = "GitManager"
    }
}
