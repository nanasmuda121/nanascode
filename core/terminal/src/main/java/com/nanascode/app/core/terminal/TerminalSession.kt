/**
 * Adapted from Acode (https://github.com/acode-foundation/acode)
 * Copyright (c) 2020 Foxdebug (Ajit Kumar)
 * Licensed under the MIT License. See LICENSE-ACODE.txt for details.
 */
package com.nanascode.app.core.terminal

import com.nanascode.app.core.common.SecureLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class TerminalSession(
    val id: String,
    val title: String,
    val workDir: File,
    private val process: Process
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val outputStream: OutputStream = process.outputStream
    private val inputStream: InputStream = process.inputStream
    private val errorStream: InputStream = process.errorStream

    private val _outputFlow = MutableSharedFlow<String>(replay = 50)
    val outputFlow: SharedFlow<String> = _outputFlow.asSharedFlow()

    private val historyBuilder = StringBuilder()
    private val historyLock = Any()

    @Volatile
    var isRunning: Boolean = true
        private set

    init {
        // Read stdout
        scope.launch {
            val buffer = ByteArray(2048)
            try {
                while (isActive && isRunning) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    val chunk = String(buffer, 0, read, StandardCharsets.UTF_8)
                    appendHistory(chunk)
                    _outputFlow.emit(chunk)
                }
            } catch (e: Exception) {
                SecureLogger.d("TerminalSession", "Stdout stream ended: ${e.message}")
            } finally {
                checkExit()
            }
        }

        // Read stderr
        scope.launch {
            val buffer = ByteArray(2048)
            try {
                while (isActive && isRunning) {
                    val read = errorStream.read(buffer)
                    if (read == -1) break
                    val chunk = String(buffer, 0, read, StandardCharsets.UTF_8)
                    appendHistory(chunk)
                    _outputFlow.emit(chunk)
                }
            } catch (e: Exception) {
                SecureLogger.d("TerminalSession", "Stderr stream ended: ${e.message}")
            }
        }

        // Process exit monitor
        scope.launch {
            try {
                val exitCode = process.waitFor()
                isRunning = false
                val exitMessage = "\r\n[Proses selesai dengan kode $exitCode]\r\n"
                appendHistory(exitMessage)
                _outputFlow.emit(exitMessage)
            } catch (_: Exception) {
                isRunning = false
            }
        }
    }

    fun write(data: String) {
        if (!isRunning) return
        scope.launch {
            try {
                outputStream.write(data.toByteArray(StandardCharsets.UTF_8))
                outputStream.flush()
            } catch (e: Exception) {
                SecureLogger.e("TerminalSession", "Gagal menulis ke terminal", e)
            }
        }
    }

    fun getHistory(): String {
        synchronized(historyLock) {
            return historyBuilder.toString()
        }
    }

    private fun appendHistory(chunk: String) {
        synchronized(historyLock) {
            historyBuilder.append(chunk)
            // Limit buffer to 100,000 characters
            if (historyBuilder.length > 100000) {
                historyBuilder.delete(0, 30000)
            }
        }
    }

    private fun checkExit() {
        try {
            process.exitValue()
            isRunning = false
        } catch (_: IllegalThreadStateException) {
            // Still running
        }
    }

    fun terminate() {
        isRunning = false
        try {
            process.destroy()
        } catch (_: Exception) {}
    }
}
