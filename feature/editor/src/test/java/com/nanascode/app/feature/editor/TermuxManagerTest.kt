package com.nanascode.app.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TermuxManagerTest {

    @Test
    fun `buildTermuxCommand builds correct commands for project types`() {
        val dummyDir = File("/storage/emulated/0/NasCode/projects/my-app")

        // 1. React Vite
        val viteCmd = TermuxManager.buildTermuxCommand(ProjectType.REACT_VITE, dummyDir, null)
        assertEquals("npm install && npm run dev -- --host", viteCmd)

        // 2. Python
        val pyFile = File(dummyDir, "test.py")
        val pyCmd = TermuxManager.buildTermuxCommand(ProjectType.PYTHON, dummyDir, pyFile)
        assertEquals("python3 test.py", pyCmd)

        // 3. Node.js
        val jsFile = File(dummyDir, "server.js")
        val nodeCmd = TermuxManager.buildTermuxCommand(ProjectType.NODE_JS, dummyDir, jsFile)
        assertEquals("node server.js", nodeCmd)
    }

    @Test
    fun `isPathInSharedStorage correctly identifies shared vs internal storage`() {
        assertTrue(TermuxManager.isPathInSharedStorage("/storage/emulated/0/NasCode/projects/app"))
        assertTrue(TermuxManager.isPathInSharedStorage("/sdcard/Download/nascode.png"))
        assertFalse(TermuxManager.isPathInSharedStorage("/data/user/0/com.nascode.nanas/files/workspaces/proj"))
    }
}
