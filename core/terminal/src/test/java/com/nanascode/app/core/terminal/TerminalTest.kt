package com.nanascode.app.core.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TerminalTest {

    @Test
    fun testSupportedAbiResolution() {
        val abi = AlpineEnvironmentManager.getSupportedAbi()
        assertTrue("ABI should be one of valid target architectures", 
            abi in listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
    }

    @Test
    fun testEnvironmentVariableSetup() {
        val env = mutableMapOf<String, String>()
        val dummyPrefix = File("/data/data/com.nascode.nanas/files")
        
        env["PREFIX"] = dummyPrefix.absolutePath
        env["TERM"] = "xterm-256color"
        env["PATH"] = "/bin:/usr/bin:${dummyPrefix.absolutePath}"

        assertEquals("xterm-256color", env["TERM"])
        assertTrue(env["PATH"]?.contains("/usr/bin") == true)
        assertEquals("/data/data/com.nascode.nanas/files", env["PREFIX"])
    }

    @Test
    fun testTerminalBridgeCallback() {
        var receivedInput: String? = null
        var isReady = false
        var resizedCols = 0
        var resizedRows = 0

        val bridge = TerminalBridge(
            onInputReceived = { receivedInput = it },
            onTerminalReady = { isReady = true },
            onTerminalResize = { cols, rows ->
                resizedCols = cols
                resizedRows = rows
            }
        )

        bridge.onReady(80, 24)
        assertTrue(isReady)

        bridge.onInput("ls -la\n")
        assertEquals("ls -la\n", receivedInput)

        bridge.onResize(120, 40)
        assertEquals(120, resizedCols)
        assertEquals(40, resizedRows)
    }
}
