package com.nanascode.app.core.git

import com.nanascode.app.core.common.DefaultDispatcherProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class GitManagerTest {

    private lateinit var tempDir: File
    private lateinit var gitManager: GitManager

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("nanas_git_test_").toFile()
        gitManager = GitManagerImpl(DefaultDispatcherProvider())
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `initRepository initializes git repo successfully`() {
        val result = gitManager.initRepository(tempDir)
        assertTrue(result.isSuccess)
        val gitFolder = File(tempDir, ".git")
        assertTrue(gitFolder.exists() && gitFolder.isDirectory)
    }

    @Test
    fun `commitAll commits tracked and untracked changes`() {
        gitManager.initRepository(tempDir)

        // Create a test file
        val file = File(tempDir, "hello.txt")
        file.writeText("Halo NanasCode 🍍")

        val statusBefore = gitManager.getStatus(tempDir).getOrThrow()
        assertTrue(statusBefore.untracked.contains("hello.txt"))
        assertFalse(statusBefore.isClean)

        // Commit
        val commitResult = gitManager.commitAll(tempDir, "Initial commit", "Tester", "tester@nanascode.app")
        assertTrue(commitResult.isSuccess)
        assertTrue(commitResult.getOrThrow().isNotBlank())

        val statusAfter = gitManager.getStatus(tempDir).getOrThrow()
        assertTrue(statusAfter.isClean)
    }

    @Test
    fun `getStatus detects modified files after commit`() {
        gitManager.initRepository(tempDir)
        val file = File(tempDir, "main.js")
        file.writeText("console.log('v1');")
        gitManager.commitAll(tempDir, "v1", "Dev", "dev@test.com")

        // Modify file
        file.appendText("\nconsole.log('v2');")
        val status = gitManager.getStatus(tempDir).getOrThrow()
        assertFalse(status.isClean)
        assertTrue(status.modified.contains("main.js"))
    }
}
