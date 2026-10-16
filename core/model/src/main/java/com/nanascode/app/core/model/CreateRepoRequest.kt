package com.nanascode.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateRepoRequest(
    val name: String,
    val description: String? = null,
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("auto_init") val autoInit: Boolean = true,
    @SerialName("gitignore_template") val gitignoreTemplate: String? = null,
    @SerialName("license_template") val licenseTemplate: String? = null
)

enum class RepoFilterType(val labelId: String) {
    ALL("Semua"),
    PUBLIC("Publik"),
    PRIVATE("Private"),
    FORK("Fork")
}

enum class RepoSortType(val value: String, val labelId: String) {
    UPDATED("updated", "Terakhir Diperbarui"),
    CREATED("created", "Terbaru Dibuat"),
    PUSHED("pushed", "Terakhir Dipush"),
    NAME("full_name", "Nama (A-Z)")
}
