package com.nascode.nanas

import com.nanascode.app.core.model.GitHubBranch
import com.nanascode.app.core.model.GitHubContentItem
import com.nanascode.app.core.model.GitHubRepo
import com.nanascode.app.core.model.ProjectSource
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSmokeTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun testAppClassInstantiation() {
        val app = NasCodeApplication()
        assertNotNull(app)
        assertEquals("com.nascode.nanas", app::class.java.`package`?.name)
    }

    @Test
    fun testR8SerializationRetainsModels() {
        val repo = GitHubRepo(
            id = 42L,
            name = "nascode",
            fullName = "nascode/nascode",
            isPrivate = false,
            defaultBranch = "main"
        )
        val encodedRepo = json.encodeToString(repo)
        val decodedRepo = json.decodeFromString<GitHubRepo>(encodedRepo)
        assertEquals("nascode", decodedRepo.name)
        assertEquals("main", decodedRepo.defaultBranch)

        val item = GitHubContentItem(
            name = "App.tsx",
            path = "src/App.tsx",
            type = "file",
            size = 2048
        )
        val encodedItem = json.encodeToString(item)
        val decodedItem = json.decodeFromString<GitHubContentItem>(encodedItem)
        assertEquals("App.tsx", decodedItem.name)
        assertEquals("2.0 KB", decodedItem.formattedSize)

        val branch = GitHubBranch(name = "main")
        val encodedBranch = json.encodeToString(branch)
        val decodedBranch = json.decodeFromString<GitHubBranch>(encodedBranch)
        assertEquals("main", decodedBranch.name)
    }

    @Test
    fun testJGitClassPresence() {
        val gitClass = Class.forName("org.eclipse.jgit.api.Git")
        assertNotNull(gitClass)
    }
}
