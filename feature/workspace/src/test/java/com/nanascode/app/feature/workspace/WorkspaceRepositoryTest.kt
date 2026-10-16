package com.nanascode.app.feature.workspace

import android.content.Context
import com.nanascode.app.core.common.DefaultDispatcherProvider
import com.nanascode.app.core.git.GitManager
import com.nanascode.app.core.model.NetworkResult
import com.nanascode.app.core.network.GitHubRepository
import com.nanascode.app.core.security.TokenManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WorkspaceRepositoryTest {

    private lateinit var tempBaseDir: File
    private lateinit var mockContext: Context
    private lateinit var mockGitManager: GitManager
    private lateinit var mockTokenManager: TokenManager
    private lateinit var mockGitHubRepository: GitHubRepository
    private lateinit var repository: WorkspaceRepository

    @Before
    fun setup() {
        tempBaseDir = Files.createTempDirectory("nanas_workspaces_test_").toFile()
        mockContext = mockk(relaxed = true)
        mockGitManager = mockk(relaxed = true) {
            every { initRepository(any()) } returns Result.success(Unit)
            every { commitAll(any(), any(), any(), any()) } returns Result.success("abc1234")
        }
        mockTokenManager = mockk(relaxed = true) {
            every { getActiveToken() } returns null // Guest mode by default
        }
        mockGitHubRepository = mockk(relaxed = true)

        repository = WorkspaceRepository(
            context = mockContext,
            gitManager = mockGitManager,
            tokenManager = mockTokenManager,
            gitHubRepository = mockGitHubRepository,
            dispatcherProvider = DefaultDispatcherProvider(),
            customBaseDir = tempBaseDir
        )
    }

    @After
    fun tearDown() {
        tempBaseDir.deleteRecursively()
    }

    @Test
    fun `initial workspaces list is empty`() = runTest {
        repository.refreshWorkspaces()
        val list = repository.workspacesFlow.value
        assertTrue("Workspaces awal harus kosong", list.isEmpty())
    }

    @Test
    fun `createWorkspace creates folder and scaffolding files from template`() = runTest {
        val created = repository.createWorkspace("My Tailwind App", "html-tailwind").getOrThrow()
        assertEquals("My Tailwind App", created.name)

        val folder = File(created.rootPath)
        assertTrue(folder.exists() && folder.isDirectory)

        // Check template files
        val indexHtml = File(folder, "index.html")
        val styleCss = File(folder, "style.css")
        val packageJson = File(folder, "package.json")

        assertTrue(indexHtml.exists())
        assertTrue(indexHtml.readText().contains("NanasCode"))
        assertTrue(styleCss.exists())
        assertTrue(styleCss.readText().contains("@import \"tailwindcss\";"))
        assertTrue(packageJson.exists())

        // Check listing
        val list = repository.workspacesFlow.value
        assertEquals(1, list.size)
        assertEquals("My Tailwind App", list.first().name)
    }

    @Test
    fun `deleteWorkspace removes folder and updates listing`() = runTest {
        val created = repository.createWorkspace("To Delete", "empty").getOrThrow()
        assertEquals(1, repository.workspacesFlow.value.size)

        repository.deleteWorkspace(created.id)
        assertFalse(File(created.rootPath).exists())
        assertEquals(0, repository.workspacesFlow.value.size)
    }

    @Test
    fun `publishToGitHub in guest mode returns 401 error login required`() = runTest {
        val created = repository.createWorkspace("Local App", "empty").getOrThrow()

        val results = mutableListOf<NetworkResult<*>>()
        repository.publishToGitHub(created.id, "my-repo", false, "Test")
            .collect { results.add(it) }

        // Harus ada error login diperlukan (401)
        val errorResult = results.filterIsInstance<NetworkResult.Error>().firstOrNull()
        assertTrue("Harus menghasilkan error login diperlukan", errorResult != null)
        assertEquals(401, errorResult?.code)
        assertTrue(errorResult?.message?.contains("Login diperlukan") == true)
    }
}
