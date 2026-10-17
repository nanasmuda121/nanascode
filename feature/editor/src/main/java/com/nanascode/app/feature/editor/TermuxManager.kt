package com.nanascode.app.feature.editor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File

sealed class TermuxExecutionResult {
    data object Success : TermuxExecutionResult()
    data object NotInstalled : TermuxExecutionResult()
    data class InInternalStorage(val currentPath: String, val sharedPath: String) : TermuxExecutionResult()
    data class PermissionRequired(val message: String) : TermuxExecutionResult()
    data class Error(val message: String, val isSetupRequired: Boolean = false) : TermuxExecutionResult()
}

object TermuxManager {

    const val TERMUX_PACKAGE = "com.termux"
    const val TERMUX_SERVICE = "com.termux.app.RunCommandService"
    const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    const val PERMISSION_RUN_COMMAND = "com.termux.permission.RUN_COMMAND"

    const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"

    const val FDROID_TERMUX_URL = "https://f-droid.org/packages/com.termux/"
    const val GITHUB_TERMUX_URL = "https://github.com/termux/termux-app/releases"

    /**
     * Folder proyek bersama di external storage agar Termux bisa membaca dan menulis kode.
     */
    fun getSharedProjectsDirectory(): File {
        val baseDir = try {
            Environment.getExternalStorageDirectory()
        } catch (_: Throwable) {
            null
        } ?: File("/storage/emulated/0")
        // Prioritaskan NasCode/projects, buat jika belum ada
        val dir = File(baseDir, "NasCode/projects")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Memeriksa apakah berkas berada di dalam penyimpanan bersama (eksternal)
     */
    fun isPathInSharedStorage(path: String): Boolean {
        val externalPath = try {
            Environment.getExternalStorageDirectory()?.absolutePath
        } catch (_: Throwable) {
            null
        } ?: "/storage/emulated/0"
        return path.startsWith(externalPath) || path.startsWith("/sdcard") || path.startsWith("/storage/emulated/0")
    }

    /**
     * Memeriksa apakah Termux terpasang pada perangkat
     */
    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Buka aplikasi Termux langsung
     */
    fun openTermux(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    /**
     * Cek izin akses semua berkas (MANAGE_EXTERNAL_STORAGE)
     */
    fun hasAllFilesPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Buka layar pengaturan izin penyimpanan
     */
    fun requestAllFilesPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    /**
     * Salin atau pindahkan proyek dari filesDir internal ke folder bersama.
     */
    fun copyOrMoveToSharedStorage(
        sourceDir: File,
        targetDir: File,
        deleteSourceAfterCopy: Boolean = false
    ): Result<File> = runCatching {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        sourceDir.copyRecursively(targetDir, overwrite = true)
        if (deleteSourceAfterCopy) {
            sourceDir.deleteRecursively()
        }
        targetDir
    }

    /**
     * Susun perintah otomatis Termux berdasarkan jenis proyek
     */
    fun buildTermuxCommand(projectType: ProjectType, workspaceDir: File, activeFile: File?): String {
        return when (projectType) {
            ProjectType.REACT_VITE -> "npm install && npm run dev -- --host"
            ProjectType.NODE_JS -> {
                val activeExt = activeFile?.extension?.lowercase() ?: ""
                val target = if (activeFile != null && activeExt in listOf("js", "ts", "mjs", "cjs")) {
                    activeFile.name
                } else if (File(workspaceDir, "index.js").exists()) {
                    "index.js"
                } else if (File(workspaceDir, "server.js").exists()) {
                    "server.js"
                } else if (File(workspaceDir, "app.js").exists()) {
                    "app.js"
                } else {
                    "index.js"
                }
                "node $target"
            }
            ProjectType.PYTHON -> {
                val target = if (activeFile != null && activeFile.extension.lowercase() == "py") {
                    activeFile.name
                } else if (File(workspaceDir, "main.py").exists()) {
                    "main.py"
                } else if (File(workspaceDir, "app.py").exists()) {
                    "app.py"
                } else {
                    "main.py"
                }
                "python3 $target"
            }
            ProjectType.HTML_STATIC -> "npx -y serve . -p 3000"
            ProjectType.UNKNOWN -> "bash"
        }
    }

    /**
     * Kirim intent RUN_COMMAND ke Termux RunCommandService
     */
    fun executeInTermux(
        context: Context,
        workspaceDir: File,
        command: String
    ): TermuxExecutionResult {
        if (!isTermuxInstalled(context)) {
            return TermuxExecutionResult.NotInstalled
        }

        // Cek apakah proyek berada di penyimpanan internal
        if (!isPathInSharedStorage(workspaceDir.absolutePath)) {
            val sharedDest = File(getSharedProjectsDirectory(), workspaceDir.name).absolutePath
            return TermuxExecutionResult.InInternalStorage(
                currentPath = workspaceDir.absolutePath,
                sharedPath = sharedDest
            )
        }

        return try {
            val intent = Intent().apply {
                action = ACTION_RUN_COMMAND
                component = ComponentName(TERMUX_PACKAGE, TERMUX_SERVICE)
                putExtra(EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/bash")
                putExtra(EXTRA_ARGUMENTS, arrayOf("-c", command))
                putExtra(EXTRA_WORKDIR, workspaceDir.absolutePath)
                putExtra(EXTRA_BACKGROUND, false)
                putExtra(EXTRA_SESSION_ACTION, "0")
            }

            context.startService(intent)
            TermuxExecutionResult.Success
        } catch (e: SecurityException) {
            TermuxExecutionResult.Error(
                message = "Izin Termux RUN_COMMAND ditolak. Pastikan 'allow-external-apps=true' telah diaktifkan di ~/.termux/termux.properties.",
                isSetupRequired = true
            )
        } catch (e: Exception) {
            TermuxExecutionResult.Error(
                message = "Gagal mengirim perintah ke Termux: ${e.message}",
                isSetupRequired = false
            )
        }
    }
}
