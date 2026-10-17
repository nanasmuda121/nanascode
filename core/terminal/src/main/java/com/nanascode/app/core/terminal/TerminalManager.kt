/**
 * Adapted from Acode (https://github.com/acode-foundation/acode)
 * Copyright (c) 2020 Foxdebug (Ajit Kumar)
 * Licensed under the MIT License. See LICENSE-ACODE.txt for details.
 */
package com.nanascode.app.core.terminal

import android.content.Context
import com.nanascode.app.core.common.SecureLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object TerminalManager {

    private const val TAG = "TerminalManager"

    private val sessionsMap = ConcurrentHashMap<String, TerminalSession>()

    private val _sessionsList = MutableStateFlow<List<TerminalSession>>(emptyList())
    val sessionsList: StateFlow<List<TerminalSession>> = _sessionsList.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    fun getActiveSession(): TerminalSession? {
        val id = _activeSessionId.value ?: return null
        return sessionsMap[id]
    }

    fun setActiveSession(id: String) {
        if (sessionsMap.containsKey(id)) {
            _activeSessionId.value = id
        }
    }

    /**
     * Start a new terminal session (Alpine PRoot or native shell)
     */
    fun startSession(
        context: Context,
        title: String,
        workDir: File,
        initialCommand: String? = null,
        useAlpine: Boolean = true
    ): Result<TerminalSession> {
        return try {
            // Ensure service is running
            TerminalService.start(context)

            val sessionId = UUID.randomUUID().toString().take(8)

            val processBuilder: ProcessBuilder
            if (useAlpine && AlpineEnvironmentManager.isAlpineInstalled(context)) {
                val cmd = initialCommand ?: "sh"
                val cmdArray = AlpineEnvironmentManager.buildProotCommand(context, cmd)
                processBuilder = ProcessBuilder(*cmdArray)
                val env = processBuilder.environment()
                AlpineEnvironmentManager.setupEnvironment(context, env)
            } else {
                // Fallback to local sh
                val cmd = initialCommand ?: "sh"
                processBuilder = ProcessBuilder("sh", "-c", cmd)
                val env = processBuilder.environment()
                env["TERM"] = "xterm-256color"
                env["HOME"] = context.filesDir.absolutePath
            }

            if (workDir.exists() && workDir.isDirectory) {
                processBuilder.directory(workDir)
            } else {
                processBuilder.directory(context.filesDir)
            }

            val process = processBuilder.start()
            val session = TerminalSession(
                id = sessionId,
                title = title,
                workDir = workDir,
                process = process
            )

            sessionsMap[sessionId] = session
            updateSessionsList()
            _activeSessionId.value = sessionId

            Result.success(session)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Gagal membuat sesi terminal", e)
            Result.failure(e)
        }
    }

    fun writeToActive(data: String) {
        getActiveSession()?.write(data)
    }

    fun closeSession(sessionId: String) {
        val session = sessionsMap.remove(sessionId)
        session?.terminate()
        updateSessionsList()

        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = sessionsMap.keys.firstOrNull()
        }

        if (sessionsMap.isEmpty()) {
            _activeSessionId.value = null
        }
    }

    fun terminateAll() {
        for (session in sessionsMap.values) {
            session.terminate()
        }
        sessionsMap.clear()
        updateSessionsList()
        _activeSessionId.value = null
    }

    private fun updateSessionsList() {
        _sessionsList.value = sessionsMap.values.toList()
    }
}
