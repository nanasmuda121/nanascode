package com.nanascode.app.feature.editor

import com.nanascode.app.feature.editor.plugin.NasCodePlugin
import com.nanascode.app.feature.editor.plugin.PluginManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

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
}
