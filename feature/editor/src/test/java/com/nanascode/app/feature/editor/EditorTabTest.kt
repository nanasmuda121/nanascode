package com.nanascode.app.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EditorTabTest {

    @Test
    fun `detectLanguage identifies standard web and mobile languages correctly`() {
        assertEquals("html", EditorTab.detectLanguage("index.html"))
        assertEquals("javascript", EditorTab.detectLanguage("main.js"))
        assertEquals("typescript", EditorTab.detectLanguage("App.tsx"))
        assertEquals("typescript", EditorTab.detectLanguage("index.ts"))
        assertEquals("css", EditorTab.detectLanguage("style.css"))
        assertEquals("python", EditorTab.detectLanguage("server.py"))
        assertEquals("kotlin", EditorTab.detectLanguage("MainActivity.kt"))
        assertEquals("json", EditorTab.detectLanguage("package.json"))
        assertEquals("markdown", EditorTab.detectLanguage("README.md"))
        assertEquals("text", EditorTab.detectLanguage("UNKNOWN_FILE"))
    }

    @Test
    fun `EditorTab dirty flag initializes as false and retains metadata`() {
        val tab = EditorTab(
            id = "tab_123",
            name = "App.js",
            path = "/data/user/0/com.nanascode.app/files/workspaces/proj/App.js",
            content = "console.log('hello');",
            isDirty = false,
            language = "javascript"
        )

        assertFalse(tab.isDirty)
        assertEquals("javascript", tab.language)

        val updatedTab = tab.copy(content = "console.log('modified');", isDirty = true)
        assertTrue(updatedTab.isDirty)
        assertEquals(tab.id, updatedTab.id)
    }

    @Test
    fun `FileNode provides proper icons based on file extensions`() {
        val htmlNode = FileNode("index.html", "/path/index.html", isDirectory = false)
        val jsNode = FileNode("script.js", "/path/script.js", isDirectory = false)
        val pyNode = FileNode("app.py", "/path/app.py", isDirectory = false)
        val dirNodeCollapsed = FileNode("src", "/path/src", isDirectory = true, isExpanded = false)
        val dirNodeExpanded = FileNode("src", "/path/src", isDirectory = true, isExpanded = true)

        assertEquals("🌐", htmlNode.icon)
        assertEquals("🟨", jsNode.icon)
        assertEquals("🐍", pyNode.icon)
        assertEquals("📁", dirNodeCollapsed.icon)
        assertEquals("📂", dirNodeExpanded.icon)
    }
}
