/**
 * Adapted from Acode (https://github.com/acode-foundation/acode)
 * Copyright (c) 2020 Foxdebug (Ajit Kumar)
 * Licensed under the MIT License. See LICENSE-ACODE.txt for details.
 */
package com.nanascode.app.core.terminal

import android.content.Context
import android.os.Build
import com.nanascode.app.core.common.SecureLogger
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.TimeZone
import java.util.zip.GZIPInputStream

object AlpineEnvironmentManager {

    private const val TAG = "AlpineEnvironment"

    fun getPrefixDir(context: Context): File = context.filesDir

    fun getAlpineDir(context: Context): File = File(context.filesDir, "alpine")

    fun isAlpineInstalled(context: Context): Boolean {
        val alpineDir = getAlpineDir(context)
        val shFile = File(alpineDir, "bin/sh")
        return shFile.exists() && shFile.length() > 0
    }

    /**
     * Determines best architecture matching device ABI
     */
    fun getSupportedAbi(): String {
        val abis = Build.SUPPORTED_ABIS ?: emptyArray()
        for (abi in abis) {
            when {
                abi.startsWith("arm64") -> return "arm64-v8a"
                abi.startsWith("armeabi") -> return "armeabi-v7a"
                abi.startsWith("x86_64") -> return "x86_64"
            }
        }
        return "arm64-v8a"
    }

    /**
     * Prepares scripts and rootfs
     */
    fun prepareEnvironment(
        context: Context,
        onProgress: (String) -> Unit = {}
    ): Result<Unit> {
        val prefix = getPrefixDir(context)
        if (!prefix.exists()) prefix.mkdirs()

        // 1. Extract shell scripts
        try {
            onProgress("Menyiapkan skrip sandboxing...")
            val scripts = listOf("init-sandbox.sh", "init-alpine.sh", "rm-wrapper.sh")
            for (script in scripts) {
                val destFile = File(prefix, script)
                val assetPath = "terminal/scripts/$script"
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.setExecutable(true, false)
                destFile.setReadable(true, false)
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Gagal mengekstrak skrip sandbox", e)
            return Result.failure(e)
        }

        // 2. Extract alpine rootfs if not present
        val alpineDir = getAlpineDir(context)
        if (!isAlpineInstalled(context)) {
            try {
                onProgress("Mengekstrak Alpine Linux rootfs (ini hanya dilakukan sekali)...")
                val abi = getSupportedAbi()
                val assetRootfs = "terminal/rootfs/$abi/alpine.rootfs"

                if (!alpineDir.exists()) alpineDir.mkdirs()

                // Extract tar.gz into alpine directory
                context.assets.open(assetRootfs).use { rawIn ->
                    extractTarGz(rawIn, alpineDir)
                }
                onProgress("Alpine Linux siap.")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Gagal menginstal rootfs Alpine", e)
                return Result.failure(e)
            }
        }

        // 3. Ensure tmp and public directories exist
        File(prefix, "tmp").mkdirs()
        File(prefix, "public").mkdirs()
        File(alpineDir, "tmp").mkdirs()

        return Result.success(Unit)
    }

    /**
     * Set up environment variables matching PRoot & sandbox requirements
     */
    fun setupEnvironment(context: Context, env: MutableMap<String, String>) {
        val prefix = getPrefixDir(context)
        val nativeDir = context.applicationInfo.nativeLibraryDir

        env["PREFIX"] = prefix.absolutePath
        env["NATIVE_DIR"] = nativeDir
        env["ANDROID_TZ"] = TimeZone.getDefault().id
        env["HOME"] = File(prefix, "public").absolutePath
        env["TMPDIR"] = File(prefix, "tmp").absolutePath
        env["TERM"] = "xterm-256color"
        env["PATH"] = "/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:$prefix"
    }

    /**
     * Builds command line string for running inside Alpine via PRoot
     */
    fun buildProotCommand(context: Context, command: String): Array<String> {
        val prefix = getPrefixDir(context)
        val scriptPath = File(prefix, "init-sandbox.sh").absolutePath
        val fullCmd = "source $scriptPath $command"
        return arrayOf("sh", "-c", fullCmd)
    }

    /**
     * Lightweight tar.gz extractor
     */
    private fun extractTarGz(inputStream: InputStream, targetDir: File) {
        GZIPInputStream(inputStream).use { gzipIn ->
            val buffer = ByteArray(8192)
            val header = ByteArray(512)

            while (true) {
                val bytesRead = readFully(gzipIn, header, 512)
                if (bytesRead < 512) break

                // Check for null blocks at end of tar
                if (header.all { it == 0.toByte() }) {
                    break
                }

                // Parse Tar header
                val name = String(header, 0, 100).trim { it <= ' ' || it == '\u0000' }
                if (name.isEmpty()) break

                val sizeStr = String(header, 124, 12).trim { it <= ' ' || it == '\u0000' }
                val size = try {
                    java.lang.Long.parseLong(sizeStr, 8)
                } catch (_: Exception) {
                    0L
                }
                val typeFlag = header[156]

                val outputFile = File(targetDir, name)

                if (typeFlag == '5'.toByte() || name.endsWith("/")) {
                    // Directory
                    outputFile.mkdirs()
                } else {
                    // Regular file
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { out ->
                        var remaining = size
                        while (remaining > 0) {
                            val toRead = minOf(remaining, buffer.size.toLong()).toInt()
                            val read = gzipIn.read(buffer, 0, toRead)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            remaining -= read
                        }
                    }
                    outputFile.setReadable(true, false)
                    // If under bin/ or sbin/, make executable
                    if (name.contains("bin/")) {
                        outputFile.setExecutable(true, false)
                    }
                }

                // TAR blocks are padded to 512 bytes
                val padding = (512 - (size % 512)) % 512
                if (padding > 0) {
                    gzipIn.skip(padding)
                }
            }
        }
    }

    private fun readFully(inStream: InputStream, target: ByteArray, length: Int): Int {
        var count = 0
        while (count < length) {
            val r = inStream.read(target, count, length - count)
            if (r < 0) break
            count += r
        }
        return count
    }
}
