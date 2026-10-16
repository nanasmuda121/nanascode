package com.nanaseditor.nanas.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimitInfoTest {

    @Test
    fun `fromHeaders parses valid headers successfully`() {
        val rateLimit = RateLimitInfo.fromHeaders(
            limitHeader = "5000",
            remainingHeader = "4980",
            resetHeader = "1729080000"
        )

        assertNotNull(rateLimit)
        assertEquals(5000, rateLimit!!.limit)
        assertEquals(4980, rateLimit.remaining)
        assertEquals(1729080000L, rateLimit.resetEpochSeconds)
        assertFalse(rateLimit.isExhausted)
        assertFalse(rateLimit.isLow)
    }

    @Test
    fun `fromHeaders with null returns null`() {
        val rateLimit = RateLimitInfo.fromHeaders(
            limitHeader = null,
            remainingHeader = "100",
            resetHeader = "1729080000"
        )
        assertNull(rateLimit)
    }

    @Test
    fun `isExhausted is true when remaining is zero`() {
        val rateLimit = RateLimitInfo(limit = 5000, remaining = 0, resetEpochSeconds = 1729080000L)
        assertTrue(rateLimit.isExhausted)
        assertFalse(rateLimit.isLow)
    }

    @Test
    fun `isLow is true when remaining between 1 and 10`() {
        val rateLimit = RateLimitInfo(limit = 5000, remaining = 5, resetEpochSeconds = 1729080000L)
        assertFalse(rateLimit.isExhausted)
        assertTrue(rateLimit.isLow)
    }

    @Test
    fun `formattedResetTime returns non-empty formatted time string`() {
        val rateLimit = RateLimitInfo(limit = 5000, remaining = 5000, resetEpochSeconds = 1729080000L)
        assertTrue(rateLimit.formattedResetTime().isNotBlank())
    }
}
