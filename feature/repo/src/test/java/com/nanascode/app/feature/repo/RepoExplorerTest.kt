package com.nanascode.app.feature.repo

import com.nanascode.app.core.model.GitHubBranch
import com.nanascode.app.core.model.GitHubContentItem
import com.nanascode.app.core.model.GitHubRepo
import com.nanascode.app.core.network.ApiErrorParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepoExplorerTest {

    @Test
    fun testBreadcrumbPathSegments() {
        val rootState = RepoExplorerState(currentPath = "")
        assertTrue(rootState.pathSegments.isEmpty())

        val nestedState = RepoExplorerState(currentPath = "src/main/assets")
        assertEquals(listOf("src", "main", "assets"), nestedState.pathSegments)
    }

    @Test
    fun testContentSortingDirectoriesFirst() {
        val items = listOf(
            GitHubContentItem(name = "index.html", path = "index.html", type = "file"),
            GitHubContentItem(name = "src", path = "src", type = "dir"),
            GitHubContentItem(name = "package.json", path = "package.json", type = "file"),
            GitHubContentItem(name = "assets", path = "assets", type = "dir")
        )

        val sorted = items.sortedWith(
            compareByDescending<GitHubContentItem> { it.isDirectory }
                .thenBy { it.name.lowercase() }
        )

        assertEquals("assets", sorted[0].name)
        assertTrue(sorted[0].isDirectory)
        assertEquals("src", sorted[1].name)
        assertTrue(sorted[1].isDirectory)
        assertEquals("index.html", sorted[2].name)
        assertFalse(sorted[2].isDirectory)
        assertEquals("package.json", sorted[3].name)
        assertFalse(sorted[3].isDirectory)
    }

    @Test
    fun testApiErrorParserExactCodeAndMessage() {
        val body401 = """{"message":"Bad credentials","documentation_url":"https://docs.github.com"}"""
        val result401 = ApiErrorParser.parseErrorMessage(401, body401, "Unauthorized")
        assertEquals("HTTP 401: Bad credentials", result401)

        val body403 = """{"message":"API rate limit exceeded for user"}"""
        val result403 = ApiErrorParser.parseErrorMessage(403, body403, "Forbidden")
        assertEquals("HTTP 403: API rate limit exceeded for user", result403)
    }

    @Test
    fun testEmptyRepoConflict409Handling() {
        val body409 = """{"message":"Git Repository is empty."}"""
        val result409 = ApiErrorParser.parseErrorMessage(409, body409, "Conflict")
        assertTrue(result409.contains("Repo kosong"))
    }

    @Test
    fun testBranchSelectionState() {
        val branches = listOf(
            GitHubBranch(name = "main"),
            GitHubBranch(name = "dev"),
            GitHubBranch(name = "v1.0")
        )
        val state = RepoExplorerState(
            isOpen = true,
            repo = GitHubRepo(name = "my-project"),
            branches = branches,
            selectedBranch = "dev"
        )

        assertEquals("dev", state.selectedBranch)
        assertEquals(3, state.branches.size)
    }
}
