package com.nanascode.app.feature.workspace

import android.content.Context
import com.nanascode.app.core.common.DispatcherProvider
import com.nanascode.app.core.common.SecureLogger
import com.nanascode.app.core.git.GitManager
import com.nanascode.app.core.model.CreateRepoRequest
import com.nanascode.app.core.model.GitHubRepo
import com.nanascode.app.core.model.NetworkResult
import com.nanascode.app.core.model.ProjectSource
import com.nanascode.app.core.model.ProjectTemplate
import com.nanascode.app.core.network.GitHubRepository
import com.nanascode.app.core.security.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitManager: GitManager,
    private val tokenManager: TokenManager,
    private val gitHubRepository: GitHubRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    // Secondary constructor untuk unit testing
    constructor(
        context: Context,
        gitManager: GitManager,
        tokenManager: TokenManager,
        gitHubRepository: GitHubRepository,
        dispatcherProvider: DispatcherProvider,
        customBaseDir: File?
    ) : this(context, gitManager, tokenManager, gitHubRepository, dispatcherProvider) {
        this.customBaseDir = customBaseDir
    }

    internal var customBaseDir: File? = null
    private val workspacesDir: File
        get() = customBaseDir ?: File(context.filesDir, "workspaces").apply { mkdirs() }

    private val _workspacesFlow = MutableStateFlow<List<ProjectSource.LocalWorkspace>>(emptyList())
    val workspacesFlow: StateFlow<List<ProjectSource.LocalWorkspace>> = _workspacesFlow.asStateFlow()

    suspend fun refreshWorkspaces() = withContext(dispatcherProvider.io) {
        val list = mutableListOf<ProjectSource.LocalWorkspace>()
        val dirs = workspacesDir.listFiles { f -> f.isDirectory } ?: emptyArray()
        for (dir in dirs) {
            val metaFile = File(dir, ".nanas_meta")
            val name = if (metaFile.exists()) metaFile.readLines().firstOrNull() ?: dir.name else dir.name
            list.add(
                ProjectSource.LocalWorkspace(
                    id = dir.name,
                    name = name,
                    rootPath = dir.absolutePath,
                    lastModifiedAt = dir.lastModified()
                )
            )
        }
        _workspacesFlow.value = list.sortedByDescending { it.lastModifiedAt }
    }

    suspend fun createWorkspace(
        name: String,
        templateId: String? = null
    ): Result<ProjectSource.LocalWorkspace> = withContext(dispatcherProvider.io) {
        runCatching {
            val id = UUID.randomUUID().toString().take(8) + "_" + name.lowercase().replace("\\s+".toRegex(), "_")
            val dir = File(workspacesDir, id)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            // Simpan metadata nama tampilan
            File(dir, ".nanas_meta").writeText(name)

            // Scaffolding template files
            val template = ProjectTemplate.ALL_TEMPLATES.find { it.id == templateId }
                ?: ProjectTemplate.ALL_TEMPLATES.first()

            for ((relPath, content) in template.files) {
                val file = File(dir, relPath)
                file.parentFile?.mkdirs()
                file.writeText(content)
            }

            // Inisialisasi local git repository
            gitManager.initRepository(dir)
            gitManager.commitAll(
                directory = dir,
                message = "feat: initial commit from NanasCode template (${template.name})",
                authorName = "NanasCode Developer",
                authorEmail = "dev@nanascode.app"
            )

            val workspace = ProjectSource.LocalWorkspace(
                id = id,
                name = name,
                rootPath = dir.absolutePath,
                templateId = template.id,
                createdAt = System.currentTimeMillis(),
                lastModifiedAt = dir.lastModified()
            )

            refreshWorkspaces()
            SecureLogger.i(TAG, "Workspace lokal berhasil dibuat: $name di ${dir.absolutePath}")
            workspace
        }
    }

    suspend fun deleteWorkspace(id: String): Result<Unit> = withContext(dispatcherProvider.io) {
        runCatching {
            val dir = File(workspacesDir, id)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
            refreshWorkspaces()
            SecureLogger.i(TAG, "Workspace $id berhasil dihapus")
        }
    }

    suspend fun renameWorkspace(id: String, newName: String): Result<Unit> = withContext(dispatcherProvider.io) {
        runCatching {
            val dir = File(workspacesDir, id)
            if (dir.exists()) {
                File(dir, ".nanas_meta").writeText(newName)
            }
            refreshWorkspaces()
        }
    }

    suspend fun importFromExternalDirectory(sourceDir: File, projectName: String): Result<ProjectSource.LocalWorkspace> = withContext(dispatcherProvider.io) {
        runCatching {
            val id = "imported_" + UUID.randomUUID().toString().take(6)
            val targetDir = File(workspacesDir, id)
            targetDir.mkdirs()

            sourceDir.copyRecursively(targetDir, overwrite = true)
            File(targetDir, ".nanas_meta").writeText(projectName)

            // Pastikan git repository diinisialisasi bila belum ada
            if (!File(targetDir, ".git").exists()) {
                gitManager.initRepository(targetDir)
                gitManager.commitAll(targetDir, "feat: import project into NanasCode", "NanasCode User", "user@nanascode.app")
            }

            val workspace = ProjectSource.LocalWorkspace(
                id = id,
                name = projectName,
                rootPath = targetDir.absolutePath,
                lastModifiedAt = targetDir.lastModified()
            )
            refreshWorkspaces()
            workspace
        }
    }

    fun publishToGitHub(
        workspaceId: String,
        repoName: String,
        isPrivate: Boolean,
        description: String?
    ): Flow<NetworkResult<GitHubRepo>> = flow {
        val token = tokenManager.getActiveToken()
        if (token == null) {
            emit(NetworkResult.Error(code = 401, message = "Login diperlukan untuk mempublikasikan proyek ke GitHub."))
            return@flow
        }

        val dir = File(workspacesDir, workspaceId)
        if (!dir.exists()) {
            emit(NetworkResult.Error(code = 404, message = "Direktori workspace tidak ditemukan."))
            return@flow
        }

        // 1. Buat remote repository di GitHub
        val createRequest = CreateRepoRequest(
            name = repoName,
            description = description,
            isPrivate = isPrivate,
            autoInit = false
        )

        when (val createResult = gitHubRepository.createRepository(createRequest)) {
            is NetworkResult.Success -> {
                val createdRepo = createResult.data
                val targetCloneUrl = createdRepo.cloneUrl
                if (targetCloneUrl.isNullOrBlank()) {
                    emit(NetworkResult.Error(message = "Gagal mendapatkan clone URL dari repositori yang baru dibuat"))
                    return@flow
                }

                // 2. Set remote origin dan push branch lokal ke GitHub
                val pushResult = gitManager.addRemoteAndPush(
                    directory = dir,
                    remoteUrl = targetCloneUrl,
                    token = token.rawValue,
                    branch = "main"
                )

                if (pushResult.isSuccess) {
                    SecureLogger.i(TAG, "Proyek lokal $workspaceId berhasil dipublikasikan ke: $targetCloneUrl")
                    emit(NetworkResult.Success(createdRepo))
                } else {
                    val err = pushResult.exceptionOrNull()?.message ?: "Gagal melakukan push ke remote GitHub"
                    emit(NetworkResult.Error(message = "Repositori dibuat di GitHub namun gagal push kode: $err"))
                }
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(code = createResult.code, message = createResult.message))
            }
            is NetworkResult.Unauthorized -> {
                emit(NetworkResult.Unauthorized(createResult.message))
            }
            is NetworkResult.NetworkUnavailable -> {
                emit(NetworkResult.NetworkUnavailable)
            }
        }
    }

    suspend fun cloneRemoteRepository(repo: GitHubRepo): Result<ProjectSource.LocalWorkspace> = withContext(dispatcherProvider.io) {
        runCatching {
            val id = "gh_" + repo.name.lowercase().replace("[^a-z0-9_]".toRegex(), "_")
            val targetDir = File(workspacesDir, id)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
                val cloneUrl = repo.cloneUrl ?: "https://github.com/${repo.fullName}.git"
                val token = tokenManager.getActiveToken()
                gitManager.cloneRepository(
                    cloneUrl = cloneUrl,
                    destination = targetDir,
                    credentials = token?.let { com.nanascode.app.core.git.GitCredentials.PersonalAccessToken(it.rawValue) }
                )
                File(targetDir, ".nanas_meta").writeText(repo.name)
            }
            val workspace = ProjectSource.LocalWorkspace(
                id = id,
                name = repo.name,
                rootPath = targetDir.absolutePath,
                lastModifiedAt = targetDir.lastModified(),
                source = ProjectSource.GitHubRemote(
                    repoId = repo.id,
                    fullName = repo.fullName,
                    cloneUrl = repo.cloneUrl ?: "",
                    defaultBranch = repo.defaultBranch ?: "main",
                    isPrivate = repo.isPrivate
                )
            )
            refreshWorkspaces()
            workspace
        }
    }

    companion object {
        private const val TAG = "WorkspaceRepository"
    }
}
