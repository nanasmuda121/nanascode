/**
 * NasCode Plugin Manifest & Permissions
 * Inspired by Acode plugin architecture (https://github.com/acode-foundation/acode)
 * Licensed under the MIT License. See LICENSE-ACODE.txt for details.
 */
package com.nanascode.app.feature.editor.plugin

import kotlinx.serialization.Serializable

@Serializable
data class NasCodePluginManifest(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val versionCode: Int = 1,
    val main: String = "main.js",
    val description: String = "",
    val author: String = "NasCode Developer",
    val permissions: List<String> = emptyList(),
    val icon: String = "extension",
    val minAppVersion: String = "0.2.0"
)

object PluginPermissions {
    const val EDITOR_READ = "editor:read"
    const val EDITOR_WRITE = "editor:write"
    const val FILE_READ = "file:read"
    const val FILE_WRITE = "file:write"
    const val COMMAND_REGISTER = "command:register"
    const val NOTIFICATION_SHOW = "notification:show"

    val ALL_PERMISSIONS = listOf(
        EDITOR_READ,
        EDITOR_WRITE,
        FILE_READ,
        FILE_WRITE,
        COMMAND_REGISTER,
        NOTIFICATION_SHOW
    )

    fun describe(permission: String): String {
        return when (permission) {
            EDITOR_READ -> "Membaca teks dari editor yang sedang aktif"
            EDITOR_WRITE -> "Menyisipkan atau mengubah teks di editor"
            FILE_READ -> "Membaca berkas di dalam folder proyek yang dibuka"
            FILE_WRITE -> "Menulis atau mengubah berkas di folder proyek"
            COMMAND_REGISTER -> "Menambahkan perintah baru ke Command Palette"
            NOTIFICATION_SHOW -> "Menampilkan notifikasi atau pesan toast"
            else -> "Izin tambahan: $permission"
        }
    }
}
