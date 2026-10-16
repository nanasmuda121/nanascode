package com.nanascode.app.core.model

/**
 * Value object wrapping a GitHub Personal Access Token.
 * Prevents raw token leakage by overriding toString().
 */
data class GitHubToken(
    val rawValue: String
) {
    init {
        require(rawValue.isNotBlank()) { "Token tidak boleh kosong" }
    }

    val isClassic: Boolean
        get() = rawValue.startsWith("ghp_") || rawValue.startsWith("github_pat_").not()

    val isFineGrained: Boolean
        get() = rawValue.startsWith("github_pat_")

    /**
     * Menghasilkan representasi token tersensor, misalnya ghp_***1234
     */
    fun masked(): String {
        val trimmed = rawValue.trim()
        return if (trimmed.length <= 8) {
            "********"
        } else {
            val prefix = trimmed.take(4)
            val suffix = trimmed.takeLast(4)
            "$prefix...$suffix"
        }
    }

    override fun toString(): String = masked()
}
