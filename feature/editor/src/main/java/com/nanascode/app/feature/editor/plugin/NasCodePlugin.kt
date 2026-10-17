package com.nanascode.app.feature.editor.plugin

import kotlinx.serialization.Serializable

@Serializable
data class NasCodePlugin(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val icon: String = "extension",
    val category: String = "Utility",
    val isInstalled: Boolean = false,
    val isEnabled: Boolean = true,
    val permissions: List<String> = emptyList(),
    val jsCode: String = "",
    val cssCode: String = "",
    val settingsSchema: String? = null,
    val minAppVersion: String = "0.2.0"
)
