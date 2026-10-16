package com.nanascode.app.feature.editor

import android.webkit.JavascriptInterface

class EditorJsBridge(
    private val onContentChange: (String) -> Unit,
    private val onCursorMove: (Int, Int) -> Unit,
    private val onReady: () -> Unit
) {
    @JavascriptInterface
    fun onContentChanged(content: String) {
        onContentChange(content)
    }

    @JavascriptInterface
    fun onCursorMoved(line: Int, column: Int) {
        onCursorMove(line, column)
    }

    @JavascriptInterface
    fun onEditorReady() {
        onReady()
    }
}
