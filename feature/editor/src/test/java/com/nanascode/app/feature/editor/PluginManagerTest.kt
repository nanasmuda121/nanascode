package com.nanascode.app.feature.editor

import com.nanascode.app.feature.editor.plugin.NasCodePlugin
import com.nanascode.app.feature.editor.plugin.NasCodePluginManifest
import com.nanascode.app.feature.editor.plugin.PluginBridge
import com.nanascode.app.feature.editor.plugin.PluginManager
import com.nanascode.app.feature.editor.plugin.PluginPermissions
import com.nanascode.app.feature.editor.plugin.PluginZipInstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PluginManagerTest {

    @Before
    fun setUp() {
        PluginManager.resetForTesting()
    }

    @Test
    fun testMarketplaceCatalogContainsEssentialPlugins() {
        val catalog = PluginManager.getMarketplaceCatalog()
        assertTrue("Katalog harus berisi lebih dari 5 plugin", catalog.size >= 5)

        val prettier = catalog.find { it.id == "prettier-formatter" }
        assertNotNull(prettier)
        assertEquals("Prettier Code Formatter", prettier?.name)
        assertTrue(prettier?.isInstalled == true)

        val emmet = catalog.find { it.id == "emmet-tools" }
        assertNotNull(emmet)
        assertEquals("Emmet HTML/CSS Expander", emmet?.name)
    }

    @Test
    fun testInstallAndUninstallPlugin() {
        val result = PluginManager.installPlugin("emmet-tools")
        assertTrue(result.isSuccess)

        val installedList = PluginManager.installedPlugins.value
        val emmetInstalled = installedList.find { it.id == "emmet-tools" }
        assertNotNull(emmetInstalled)
        assertTrue(emmetInstalled?.isEnabled == true)

        // Uninstall
        val uninstallResult = PluginManager.uninstallPlugin("emmet-tools")
        assertTrue(uninstallResult.isSuccess)
        assertFalse(PluginManager.installedPlugins.value.any { it.id == "emmet-tools" })
    }

    @Test
    fun testTogglePlugin() {
        PluginManager.togglePlugin("prettier-formatter", false)
        val prettier = PluginManager.installedPlugins.value.find { it.id == "prettier-formatter" }
        assertFalse(prettier?.isEnabled == true)

        PluginManager.togglePlugin("prettier-formatter", true)
        val prettierReenabled = PluginManager.installedPlugins.value.find { it.id == "prettier-formatter" }
        assertTrue(prettierReenabled?.isEnabled == true)
    }

    @Test
    fun testInstallCustomPlugin() {
        val result = PluginManager.installCustomPlugin(
            name = "Test Logger",
            author = "Tester",
            description = "Custom console logger",
            jsCode = "console.log('test plugin active');"
        )
        assertTrue(result.isSuccess)
        val custom = result.getOrThrow()
        assertEquals("Test Logger", custom.name)

        val activeJs = PluginManager.getActivePluginsJs()
        assertTrue(activeJs.contains("test plugin active"))
    }

    @Test
    fun testZipPluginInspectionAndInstallation() {
        // Create an in-memory ZIP with plugin.json and main.js
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // Add plugin.json
            zos.putNextEntry(ZipEntry("plugin.json"))
            val manifestJson = """
                {
                    "id": "com.test.linter",
                    "name": "Test Linter",
                    "version": "1.2.0",
                    "main": "main.js",
                    "description": "Linting plugin test",
                    "author": "Test Author",
                    "permissions": ["editor:read", "notification:show"]
                }
            """.trimIndent()
            zos.write(manifestJson.toByteArray())
            zos.closeEntry()

            // Add main.js
            zos.putNextEntry(ZipEntry("main.js"))
            val mainJs = "function init(ctx) { ctx.notification.showToast('Linter Ready'); }"
            zos.write(mainJs.toByteArray())
            zos.closeEntry()
        }

        val zipBytes = baos.toByteArray()

        // 1. Inspect manifest
        val inspectResult = PluginZipInstaller.inspectManifest(ByteArrayInputStream(zipBytes))
        assertTrue(inspectResult.isSuccess)
        val manifest = inspectResult.getOrThrow()
        assertEquals("com.test.linter", manifest.id)
        assertEquals("Test Linter", manifest.name)
        assertEquals("1.2.0", manifest.version)
        assertEquals(listOf("editor:read", "notification:show"), manifest.permissions)

        // 2. Install to temp dir
        val tempDir = File.createTempFile("test_plugins", "").apply { delete(); mkdirs() }
        try {
            val installResult = PluginZipInstaller.installFromZip(ByteArrayInputStream(zipBytes), tempDir)
            assertTrue(installResult.isSuccess)
            val installed = installResult.getOrThrow()
            assertEquals("com.test.linter", installed.id)
            assertTrue(installed.jsCode.contains("Linter Ready"))
            assertEquals(listOf("editor:read", "notification:show"), installed.permissions)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testPluginBridgePermissionEnforcement() {
        // Configure installed plugin with only EDITOR_READ permission
        val plugin = NasCodePlugin(
            id = "test-plugin-perms",
            name = "Perms Test",
            version = "1.0.0",
            author = "Test",
            description = "Test",
            isInstalled = true,
            isEnabled = true,
            permissions = listOf(PluginPermissions.EDITOR_READ)
        )
        PluginManager.resetForTesting(listOf(plugin))

        var editorContent = "Hello NasCode World"
        var writtenText = ""
        var toastShown = ""

        val tempWorkspace = File.createTempFile("workspace_test", "").apply { delete(); mkdirs() }

        try {
            val bridge = PluginBridge(
                workspaceDirProvider = { tempWorkspace },
                onGetEditorContent = { editorContent },
                onInsertEditorText = { writtenText = it },
                onShowToast = { toastShown = it },
                onRegisterCommand = { _, _, _ -> }
            )

            // EDITOR_READ should succeed because it was granted
            val readText = bridge.getEditorText("test-plugin-perms")
            assertEquals("Hello NasCode World", readText)

            // EDITOR_WRITE should be blocked (permission not granted)
            bridge.insertEditorText("test-plugin-perms", "Attempted write")
            assertEquals("", writtenText)

            // NOTIFICATION_SHOW should be blocked
            bridge.showToast("test-plugin-perms", "Unauthorized notification")
            assertEquals("", toastShown)

            // FILE_READ & FILE_WRITE should be blocked
            val readAttempt = bridge.readFile("test-plugin-perms", "test.txt")
            assertEquals("", readAttempt)

            val writeAttempt = bridge.writeFile("test-plugin-perms", "test.txt", "content")
            assertFalse(writeAttempt)
        } finally {
            tempWorkspace.deleteRecursively()
        }
    }

    @Test
    fun testPluginBridgeSandboxDirectoryTraversingBlocked() {
        val plugin = NasCodePlugin(
            id = "test-sandbox",
            name = "Sandbox Test",
            version = "1.0.0",
            author = "Test",
            description = "Test",
            isInstalled = true,
            isEnabled = true,
            permissions = listOf(PluginPermissions.FILE_READ, PluginPermissions.FILE_WRITE)
        )
        PluginManager.resetForTesting(listOf(plugin))

        val tempWorkspace = File.createTempFile("workspace_sandbox", "").apply { delete(); mkdirs() }
        val secretOutside = File.createTempFile("secret_outside", ".txt").apply { writeText("TOP_SECRET") }

        try {
            val bridge = PluginBridge(
                workspaceDirProvider = { tempWorkspace },
                onGetEditorContent = { "" },
                onInsertEditorText = {},
                onShowToast = {},
                onRegisterCommand = { _, _, _ -> }
            )

            // Normal file inside workspace should succeed
            val writeSuccess = bridge.writeFile("test-sandbox", "project.json", "{\"key\":\"val\"}")
            assertTrue(writeSuccess)
            val readSuccess = bridge.readFile("test-sandbox", "project.json")
            assertEquals("{\"key\":\"val\"}", readSuccess)

            // Directory traversal (path traversal) outside workspace MUST be blocked
            val traverseAttempt = bridge.readFile("test-sandbox", "../../${secretOutside.name}")
            assertEquals("", traverseAttempt)

            val traverseWriteAttempt = bridge.writeFile("test-sandbox", "../../../evil.txt", "evil")
            assertFalse(traverseWriteAttempt)
        } finally {
            tempWorkspace.deleteRecursively()
            secretOutside.delete()
        }
    }
}
