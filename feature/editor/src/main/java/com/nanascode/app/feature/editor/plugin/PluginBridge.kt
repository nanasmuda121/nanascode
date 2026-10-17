/**
 * NasCode Sandboxed Plugin Javascript Bridge
 * Implements capability-checked API: editor, file, command palette, notifications
 * Licensed under the MIT License. See LICENSE-ACODE.txt for details.
 */
package com.nanascode.app.feature.editor.plugin

import android.webkit.JavascriptInterface
import com.nanascode.app.core.common.SecureLogger
import java.io.File

class PluginBridge(
    private val workspaceDirProvider: () -> File?,
    private val onGetEditorContent: () -> String,
    private val onInsertEditorText: (String) -> Unit,
    private val onShowToast: (String) -> Unit,
    private val onRegisterCommand: (pluginId: String, commandId: String, title: String) -> Unit
) {
    companion object {
        private const val TAG = "PluginBridge"
    }

    private fun checkPermission(pluginId: String, permission: String): Boolean {
        val plugin = PluginManager.installedPlugins.value.find { it.id == pluginId }
        if (plugin == null || !plugin.isEnabled) {
            SecureLogger.w(TAG, "Plugin $pluginId tidak aktif atau tidak ditemukan")
            return false
        }
        val hasPerm = plugin.permissions.contains(permission)
        if (!hasPerm) {
            SecureLogger.w(TAG, "Plugin $pluginId meminta $permission tanpa izin manifest")
        }
        return hasPerm
    }

    @JavascriptInterface
    fun getEditorText(pluginId: String): String {
        return if (checkPermission(pluginId, PluginPermissions.EDITOR_READ)) {
            onGetEditorContent()
        } else {
            ""
        }
    }

    @JavascriptInterface
    fun insertEditorText(pluginId: String, text: String) {
        if (checkPermission(pluginId, PluginPermissions.EDITOR_WRITE)) {
            onInsertEditorText(text)
        }
    }

    @JavascriptInterface
    fun readFile(pluginId: String, relativePath: String): String {
        if (!checkPermission(pluginId, PluginPermissions.FILE_READ)) return ""
        val wsDir = workspaceDirProvider() ?: return ""
        val target = File(wsDir, relativePath).canonicalFile

        // Sandbox check: ensure file is inside workspace directory
        if (!target.path.startsWith(wsDir.canonicalPath)) {
            SecureLogger.w(TAG, "Plugin $pluginId mencoba mengakses file di luar workspace: $relativePath")
            return ""
        }

        return if (target.exists() && target.isFile) {
            try {
                target.readText()
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Gagal membaca berkas: $relativePath", e)
                ""
            }
        } else ""
    }

    @JavascriptInterface
    fun writeFile(pluginId: String, relativePath: String, content: String): Boolean {
        if (!checkPermission(pluginId, PluginPermissions.FILE_WRITE)) return false
        val wsDir = workspaceDirProvider() ?: return false
        val target = File(wsDir, relativePath).canonicalFile

        // Sandbox check
        if (!target.path.startsWith(wsDir.canonicalPath)) {
            SecureLogger.w(TAG, "Plugin $pluginId mencoba menulis berkas di luar workspace: $relativePath")
            return false
        }

        return try {
            target.parentFile?.mkdirs()
            target.writeText(content)
            true
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Gagal menulis berkas: $relativePath", e)
            false
        }
    }

    @JavascriptInterface
    fun registerCommand(pluginId: String, commandId: String, title: String) {
        if (checkPermission(pluginId, PluginPermissions.COMMAND_REGISTER)) {
            onRegisterCommand(pluginId, commandId, title)
        }
    }

    @JavascriptInterface
    fun showToast(pluginId: String, message: String) {
        if (checkPermission(pluginId, PluginPermissions.NOTIFICATION_SHOW)) {
            onShowToast(message)
        }
    }
}
