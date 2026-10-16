package com.nanascode.app.feature.repo

import com.nanascode.app.core.model.GitHubRepo
import com.nanascode.app.core.model.RepoFilterType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RepoFilterTest {

    private val sampleRepos = listOf(
        GitHubRepo(
            id = 1,
            name = "nanas-editor",
            fullName = "octocat/nanas-editor",
            description = "Mobile code editor like VS Code",
            isPrivate = false,
            isFork = false
        ),
        GitHubRepo(
            id = 2,
            name = "secret-backend",
            fullName = "octocat/secret-backend",
            description = "Internal payment processing API",
            isPrivate = true,
            isFork = false
        ),
        GitHubRepo(
            id = 3,
            name = "react-forked",
            fullName = "octocat/react-forked",
            description = "My fork of React library",
            isPrivate = false,
            isFork = true
        )
    )

    @Test
    fun `search filter matches repo name regardless of case`() {
        val state = RepoListState(
            repos = sampleRepos,
            searchQuery = "NANAS"
        )
        val filtered = state.filteredRepos

        assertEquals(1, filtered.size)
        assertEquals("nanas-editor", filtered[0].name)
    }

    @Test
    fun `search filter matches repo description`() {
        val state = RepoListState(
            repos = sampleRepos,
            searchQuery = "payment"
        )
        val filtered = state.filteredRepos

        assertEquals(1, filtered.size)
        assertEquals("secret-backend", filtered[0].name)
    }

    @Test
    fun `empty search query returns all repos`() {
        val state = RepoListState(
            repos = sampleRepos,
            searchQuery = ""
        )
        assertEquals(3, state.filteredRepos.size)
    }

    @Test
    fun `github repo json parser handles null default_branch and missing fields without crashing`() {
        val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        val jsonString = """
            {
                "id": 999999,
                "name": "empty-new-repo",
                "full_name": "nanasmuda121/empty-new-repo",
                "private": false,
                "default_branch": null,
                "description": null,
                "owner": {
                    "login": "nanasmuda121"
                }
            }
        """.trimIndent()

        val repo = jsonParser.decodeFromString<GitHubRepo>(jsonString)
        assertEquals(999999L, repo.id)
        assertEquals("empty-new-repo", repo.name)
        assertNull(repo.defaultBranch)
        assertNotNull(repo.owner)
        assertEquals("nanasmuda121", repo.owner?.login)
    }
}
