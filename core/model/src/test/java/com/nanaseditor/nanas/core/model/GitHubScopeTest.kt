package com.nanaseditor.nanas.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubScopeTest {

    @Test
    fun `parseHeader with full classic scopes has no warnings`() {
        val token = GitHubToken("ghp_validtoken1234567890")
        val header = "repo, read:user, workflow"
        val result = GitHubScopeParser.parseHeader(header, token)

        assertTrue(result.hasRepoScope)
        assertTrue(result.hasUserScope)
        assertTrue(result.hasWorkflowScope)
        assertTrue(result.warnings.isEmpty())
        assertTrue(result.isFullyEmpowered)
    }

    @Test
    fun `parseHeader missing repo scope generates warning`() {
        val token = GitHubToken("ghp_validtoken1234567890")
        val header = "read:user, workflow"
        val result = GitHubScopeParser.parseHeader(header, token)

        assertFalse(result.hasRepoScope)
        assertTrue(result.hasUserScope)
        assertTrue(result.hasWorkflowScope)
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings[0].contains("Scope 'repo' tidak ditemukan"))
    }

    @Test
    fun `parseHeader missing workflow scope generates warning`() {
        val token = GitHubToken("ghp_validtoken1234567890")
        val header = "repo, read:user"
        val result = GitHubScopeParser.parseHeader(header, token)

        assertTrue(result.hasRepoScope)
        assertFalse(result.hasWorkflowScope)
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings[0].contains("Scope 'workflow' tidak ditemukan"))
    }

    @Test
    fun `parseHeader with fine grained token adds informative reminder`() {
        val token = GitHubToken("github_pat_11AAAAAAA_test123456789")
        val result = GitHubScopeParser.parseHeader(null, token)

        assertTrue(result.isFineGrained)
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings[0].contains("Token Fine-grained terdeteksi"))
    }
}
