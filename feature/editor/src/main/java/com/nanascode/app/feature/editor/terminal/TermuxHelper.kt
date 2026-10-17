package com.nanascode.app.feature.editor.terminal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nanascode.app.core.common.SecureLogger
import java.io.File

object TermuxHelper {

    private const val TAG = "TermuxHelper"
    const val TERMUX_PACKAGE = "com.termux"
    const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    const val RUN_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val RUN_COMMAND_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val RUN_COMMAND_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val RUN_COMMAND_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openTermux(context: Context): Result<Unit> {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Aplikasi Termux tidak terpasang"))
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Gagal membuka Termux", e)
            Result.failure(e)
        }
    }

    fun runInTermux(
        context: Context,
        command: String,
        workDir: File
    ): Result<Unit> {
        return try {
            val intent = Intent(RUN_COMMAND_ACTION).apply {
                component = ComponentName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE)
                putExtra(RUN_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/bash")
                putExtra(RUN_COMMAND_ARGUMENTS, arrayOf("-c", command))
                putExtra(RUN_COMMAND_WORKDIR, workDir.absolutePath)
                putExtra(RUN_COMMAND_BACKGROUND, false)
            }
            context.startService(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Gagal mengirim RUN_COMMAND ke Termux", e)
            Result.failure(e)
        }
    }

    fun openDownloadPage(context: Context) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/termux/termux-app/releases/latest")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
