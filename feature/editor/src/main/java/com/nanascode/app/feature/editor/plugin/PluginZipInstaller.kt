/**
 * NasCode Local ZIP Plugin Installer
 * Inspired by Acode plugin architecture (https://github.com/acode-foundation/acode)
 * Licensed under the MIT License. See LICENSE-ACODE.txt for details.
 */
package com.nanascode.app.feature.editor.plugin

import com.nanascode.app.core.common.SecureLogger
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object PluginZipInstaller {

    private const val TAG = "PluginZipInstaller"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Inspects a plugin ZIP input stream to read its manifest before installing
     */
    fun inspectManifest(zipStream: InputStream): Result<NasCodePluginManifest> {
        return try {
            ZipInputStream(zipStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.replace('\\', '/')
                    if (name.endsWith("plugin.json") && !name.contains("__MACOSX")) {
                        val bytes = readEntryBytes(zis)
                        val content = String(bytes, StandardCharsets.UTF_8)
                        val manifest = json.decodeFromString<NasCodePluginManifest>(content)
                        return Result.success(manifest)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.failure(IllegalArgumentException("Berkas 'plugin.json' tidak ditemukan di dalam arsip ZIP"))
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Gagal membaca manifest plugin dari ZIP", e)
            Result.failure(e)
        }
    }

    /**
     * Installs a plugin from a ZIP input stream into the target plugins directory
     */
    fun installFromZip(
        zipStream: InputStream,
        pluginsDir: File
    ): Result<NasCodePlugin> {
        return try {
            val entriesMap = mutableMapOf<String, ByteArray>()

            ZipInputStream(zipStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.replace('\\', '/')
                    if (!entry.isDirectory && !name.contains("__MACOSX")) {
                        entriesMap[name] = readEntryBytes(zis)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Find plugin.json
            val manifestEntryKey = entriesMap.keys.find { it.endsWith("plugin.json") }
                ?: return Result.failure(IllegalArgumentException("Berkas 'plugin.json' tidak ditemukan di dalam arsip ZIP"))

            val manifestBytes = entriesMap[manifestEntryKey]!!
            val manifestStr = String(manifestBytes, StandardCharsets.UTF_8)
            val manifest = json.decodeFromString<NasCodePluginManifest>(manifestStr)

            if (manifest.id.isBlank() || manifest.name.isBlank()) {
                return Result.failure(IllegalArgumentException("Manifest 'plugin.json' tidak valid (id dan name wajib diisi)"))
            }

            // Strip prefix path if zipped inside a subfolder
            val prefix = if (manifestEntryKey.contains("/")) {
                manifestEntryKey.substringBeforeLast("/") + "/"
            } else ""

            val targetDir = File(pluginsDir, manifest.id)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            targetDir.mkdirs()

            // Extract all files
            var mainJsCode = ""
            var cssCode = ""

            for ((key, bytes) in entriesMap) {
                val relPath = if (prefix.isNotEmpty() && key.startsWith(prefix)) {
                    key.removePrefix(prefix)
                } else key

                val targetFile = File(targetDir, relPath)
                targetFile.parentFile?.mkdirs()
                FileOutputStream(targetFile).use { it.write(bytes) }

                if (relPath == manifest.main) {
                    mainJsCode = String(bytes, StandardCharsets.UTF_8)
                } else if (relPath.endsWith(".css")) {
                    cssCode += String(bytes, StandardCharsets.UTF_8) + "\n"
                }
            }

            if (mainJsCode.isBlank()) {
                // Fallback to searching any main.js
                val fallbackMain = File(targetDir, "main.js")
                if (fallbackMain.exists()) {
                    mainJsCode = fallbackMain.readText(StandardCharsets.UTF_8)
                }
            }

            val installedPlugin = NasCodePlugin(
                id = manifest.id,
                name = manifest.name,
                version = manifest.version,
                author = manifest.author,
                description = manifest.description,
                icon = manifest.icon,
                category = "Terpasang",
                isInstalled = true,
                isEnabled = true,
                permissions = manifest.permissions,
                jsCode = mainJsCode,
                cssCode = cssCode.trim(),
                minAppVersion = manifest.minAppVersion
            )

            Result.success(installedPlugin)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Gagal menginstal plugin dari ZIP", e)
            Result.failure(e)
        }
    }

    private fun readEntryBytes(zis: ZipInputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var count = zis.read(buffer)
        while (count != -1) {
            baos.write(buffer, 0, count)
            count = zis.read(buffer)
        }
        return baos.toByteArray()
    }
}
