package com.nanascode.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubTokenTest {

    @Test
    fun `masked hides middle characters of classic token`() {
        val token = GitHubToken("ghp_1234567890abcdefghijklmnopqrstuvwxyz")
        assertEquals("ghp_...wxyz", token.masked())
        // toString must never leak raw token
        assertEquals("ghp_...wxyz", token.toString())
    }

    @Test
    fun `masked hides middle characters of fine-grained token`() {
        val token = GitHubToken("github_pat_11AAAAAAA_abcdefghijklmnop9999")
        assertEquals("gith...9999", token.masked())
        assertEquals("gith...9999", token.toString())
    }

    @Test
    fun `isClassic detects classic token correctly`() {
        val classicToken = GitHubToken("ghp_1234567890abcdef")
        assertTrue(classicToken.isClassic)
        assertFalse(classicToken.isFineGrained)
    }

    @Test
    fun `isFineGrained detects fine-grained token correctly`() {
        val fineGrainedToken = GitHubToken("github_pat_1234567890abcdef")
        assertTrue(fineGrainedToken.isFineGrained)
        assertFalse(fineGrainedToken.isClassic)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty token throws IllegalArgumentException`() {
        GitHubToken("   ")
    }
}
