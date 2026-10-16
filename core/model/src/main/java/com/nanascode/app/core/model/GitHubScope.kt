package com.nanascode.app.core.model

data class ScopeCheckResult(
    val scopes: List<String>,
    val hasRepoScope: Boolean,
    val hasUserScope: Boolean,
    val hasWorkflowScope: Boolean,
    val isFineGrained: Boolean,
    val warnings: List<String>
) {
    val isFullyEmpowered: Boolean
        get() = (hasRepoScope && hasWorkflowScope) || isFineGrained
}

object GitHubScopeParser {

    /**
     * Mem-parsing header X-OAuth-Scopes dari response GitHub API.
     * Contoh nilai header: "repo, read:user, workflow"
     */
    fun parseHeader(scopeHeader: String?, token: GitHubToken): ScopeCheckResult {
        val isFineGrained = token.isFineGrained
        val scopes = scopeHeader?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        val hasRepoScope = scopes.any { it == "repo" }
        val hasUserScope = scopes.any { it == "read:user" || it == "user" }
        val hasWorkflowScope = scopes.any { it == "workflow" }

        val warnings = mutableListOf<String>()

        if (!isFineGrained) {
            if (!hasRepoScope) {
                warnings.add("Scope 'repo' tidak ditemukan. Anda tidak akan bisa melihat repo private atau meng-commit file.")
            }
            if (!hasWorkflowScope) {
                warnings.add("Scope 'workflow' tidak ditemukan. Fitur GitHub Actions (CI/CD) tidak dapat dijalankan.")
            }
        } else {
            // Fine-grained token
            warnings.add("Token Fine-grained terdeteksi. Pastikan permission 'Contents: Read and Write' dan 'Workflows: Read and Write' telah aktif pada repositori target.")
        }

        return ScopeCheckResult(
            scopes = scopes,
            hasRepoScope = hasRepoScope,
            hasUserScope = hasUserScope,
            hasWorkflowScope = hasWorkflowScope,
            isFineGrained = isFineGrained,
            warnings = warnings
        )
    }
}
