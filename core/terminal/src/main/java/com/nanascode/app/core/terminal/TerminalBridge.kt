package com.nanascode.app.core.terminal

import android.webkit.JavascriptInterface

class TerminalBridge(
    private val onInputReceived: (String) -> Unit,
    private val onTerminalReady: () -> Unit = {},
    private val onTerminalResize: (Int, Int) -> Unit = { _, _ -> }
) {
    @JavascriptInterface
    fun onInput(data: String) {
        onInputReceived(data)
    }

    @JavascriptInterface
    fun onReady(cols: Int, rows: Int) {
        onTerminalReady()
    }

    @JavascriptInterface
    fun onResize(cols: Int, rows: Int) {
        onTerminalResize(cols, rows)
    }
}
