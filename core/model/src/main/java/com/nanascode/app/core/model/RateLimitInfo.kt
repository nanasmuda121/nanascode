package com.nanascode.app.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RateLimitInfo(
    val limit: Int,
    val remaining: Int,
    val resetEpochSeconds: Long
) {
    val isExhausted: Boolean
        get() = remaining <= 0

    val isLow: Boolean
        get() = remaining in 1..10

    /**
     * Format jam & menit kapan kuota reset (contoh: "18:45:00")
     */
    fun formattedResetTime(): String {
        return try {
            val date = Date(resetEpochSeconds * 1000)
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(date)
        } catch (e: Exception) {
            "$resetEpochSeconds"
        }
    }

    /**
     * Hitung sisa menit sampai reset
     */
    fun minutesUntilReset(): Long {
        val nowSeconds = System.currentTimeMillis() / 1000
        val diff = resetEpochSeconds - nowSeconds
        return if (diff > 0) (diff / 60) + 1 else 0
    }

    companion object {
        fun fromHeaders(limitHeader: String?, remainingHeader: String?, resetHeader: String?): RateLimitInfo? {
            val limit = limitHeader?.toIntOrNull() ?: return null
            val remaining = remainingHeader?.toIntOrNull() ?: return null
            val reset = resetHeader?.toLongOrNull() ?: return null
            return RateLimitInfo(limit, remaining, reset)
        }
    }
}
