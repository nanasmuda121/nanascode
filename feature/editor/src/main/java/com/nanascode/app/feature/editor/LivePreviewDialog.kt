package com.nanascode.app.feature.editor

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nanascode.app.core.ui.theme.NanasDarkBg
import com.nanascode.app.core.ui.theme.NanasDarkBorder
import com.nanascode.app.core.ui.theme.NanasDarkSurface
import com.nanascode.app.core.ui.theme.NanasDarkSurfaceLight
import com.nanascode.app.core.ui.theme.NanasError
import com.nanascode.app.core.ui.theme.NanasGreen
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import com.nanascode.app.core.ui.theme.NanasTextSecondary
import com.nanascode.app.core.ui.theme.NanasYellow
import java.io.File

data class WebConsoleLog(
    val message: String,
    val sourceId: String,
    val lineNumber: Int,
    val level: ConsoleMessage.MessageLevel
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePreviewDialog(
    workspaceRoot: String,
    activeFilePath: String?,
    previewUrl: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Tentukan target konten preview (Acode style)
    val active = activeFilePath?.let { File(it) }
    val ext = active?.extension?.lowercase() ?: ""

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var consoleLogs by remember { mutableStateOf<List<WebConsoleLog>>(emptyList()) }
    var isConsoleExpanded by remember { mutableStateOf(false) }
    var replCommand by remember { mutableStateOf("") }

    val errorCount = consoleLogs.count { it.level == ConsoleMessage.MessageLevel.ERROR }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NanasDarkBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    previewUrl != null -> "Preview ($previewUrl)"
                                    ext in listOf("html", "htm") -> "Pratinjau HTML (${active?.name})"
                                    ext == "md" -> "Pratinjau Markdown (${active?.name})"
                                    ext == "js" -> "Pratinjau JS Runner (${active?.name})"
                                    else -> "Pratinjau Web NasCode"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = NanasTextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = NanasTextPrimary)
                        }
                        if (previewUrl != null) {
                            IconButton(onClick = {
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(previewUrl))
                                    context.startActivity(browserIntent)
                                } catch (_: Exception) {}
                            }) {
                                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = "Buka di Browser", tint = NanasTextPrimary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NanasDarkSurface)
                )

                // Area Pratinjau Web (Acode Style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.allowFileAccessFromFileURLs = true
                                settings.allowUniversalAccessFromFileURLs = true

                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                                        if (cm != null) {
                                            consoleLogs = consoleLogs + WebConsoleLog(
                                                message = cm.message() ?: "",
                                                sourceId = cm.sourceId() ?: "",
                                                lineNumber = cm.lineNumber(),
                                                level = cm.messageLevel() ?: ConsoleMessage.MessageLevel.LOG
                                            )
                                        }
                                        return true
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                    }
                                }

                                // Muat berkas sesuai tipe
                                loadPreviewContent(this, workspaceRoot, active, previewUrl)
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp),
                            color = Color.Black
                        )
                    }
                }

                // Panel Console Acode (Log, Warning, Error & REPL)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NanasDarkSurface)
                        .border(1.dp, NanasDarkBorder)
                ) {
                    // Console Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isConsoleExpanded = !isConsoleExpanded }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = if (errorCount > 0) NanasError else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Konsol Pengembang (${consoleLogs.size})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            if (errorCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(NanasError.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(1.dp, NanasError.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "$errorCount Error", color = NanasError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (consoleLogs.isNotEmpty()) {
                                Text(
                                    text = "Bersihkan",
                                    color = NanasTextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clickable { consoleLogs = emptyList() }
                                        .padding(horizontal = 8.dp)
                                )
                            }
                            Icon(
                                imageVector = if (isConsoleExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = NanasTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Console Body & Interactive REPL
                    if (isConsoleExpanded) {
                        HorizontalDivider(color = NanasDarkBorder, thickness = 0.8.dp)

                        if (consoleLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Konsol bersih (belum ada pesan log/error)", color = NanasTextSecondary, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                items(consoleLogs) { log ->
                                    val (icon, color) = when (log.level) {
                                        ConsoleMessage.MessageLevel.ERROR -> Icons.Default.Warning to NanasError
                                        ConsoleMessage.MessageLevel.WARNING -> Icons.Default.Warning to NanasYellow
                                        else -> Icons.Default.Info to NanasGreen
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = log.message,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            if (log.sourceId.isNotBlank()) {
                                                val fileName = log.sourceId.substringAfterLast("/")
                                                Text(
                                                    text = "$fileName:${log.lineNumber}",
                                                    color = NanasTextSecondary,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // REPL Console Input (Acode style eval)
                        HorizontalDivider(color = NanasDarkBorder, thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(">", color = NanasYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedTextField(
                                value = replCommand,
                                onValueChange = { replCommand = it },
                                placeholder = { Text("Ketik JS (contoh: document.title)...", fontSize = 11.sp, color = NanasTextSecondary) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = NanasDarkBg,
                                    unfocusedContainerColor = NanasDarkBg,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (replCommand.isNotBlank()) {
                                        val cmd = replCommand
                                        replCommand = ""
                                        webViewRef?.evaluateJavascript(cmd) { result ->
                                            consoleLogs = consoleLogs + WebConsoleLog(
                                                message = "> $cmd\n← $result",
                                                sourceId = "repl",
                                                lineNumber = 1,
                                                level = ConsoleMessage.MessageLevel.LOG
                                            )
                                        }
                                    }
                                },
                                enabled = replCommand.isNotBlank()
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Jalankan", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Logika pemuatan berkas yang fleksibel (Acode Style)
 */
private fun loadPreviewContent(
    webView: WebView,
    workspaceRoot: String,
    activeFile: File?,
    previewUrl: String?
) {
    if (!previewUrl.isNullOrBlank()) {
        webView.loadUrl(previewUrl)
        return
    }

    val ext = activeFile?.extension?.lowercase() ?: ""

    // 1. Berkas HTML
    if (activeFile != null && activeFile.exists() && ext in listOf("html", "htm")) {
        webView.loadUrl("file://${activeFile.absolutePath}")
        return
    }

    // 2. Berkas Markdown (.md)
    if (activeFile != null && activeFile.exists() && ext == "md") {
        val markdownText = activeFile.readText()
        val html = buildMarkdownHtml(markdownText, activeFile.name)
        webView.loadDataWithBaseURL("file://${activeFile.parentFile?.absolutePath ?: workspaceRoot}/", html, "text/html", "UTF-8", null)
        return
    }

    // 3. Berkas JavaScript (.js)
    if (activeFile != null && activeFile.exists() && ext in listOf("js", "mjs")) {
        val jsContent = activeFile.readText()
        val html = buildJsRunnerHtml(jsContent, activeFile.name)
        webView.loadDataWithBaseURL("file://${activeFile.parentFile?.absolutePath ?: workspaceRoot}/", html, "text/html", "UTF-8", null)
        return
    }

    // 4. index.html di root workspace
    val rootIndex = File(workspaceRoot, "index.html")
    if (rootIndex.exists()) {
        webView.loadUrl("file://${rootIndex.absolutePath}")
        return
    }

    // 5. Fallback informasi
    val emptyHtml = """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"><style>body{background:#121212;color:#eee;font-family:sans-serif;padding:24px;text-align:center;}</style></head>
        <body>
          <h2>Pratinjau Web NasCode</h2>
          <p style="color:#aaa;">Buka atau buat berkas <code>index.html</code> atau <code>.md</code> untuk melihat pratinjau langsung.</p>
        </body>
        </html>
    """.trimIndent()
    webView.loadDataWithBaseURL(null, emptyHtml, "text/html", "UTF-8", null)
}

private fun buildMarkdownHtml(markdown: String, title: String): String {
    // Markdown parser ringan
    val parsed = markdown
        .replace(Regex("(?m)^### (.*)$"), "<h3>$1</h3>")
        .replace(Regex("(?m)^## (.*)$"), "<h2>$1</h2>")
        .replace(Regex("(?m)^# (.*)$"), "<h1>$1</h1>")
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>")
        .replace(Regex("\\*(.*?)\\*"), "<em>$1</em>")
        .replace(Regex("`([^`]+)`"), "<code style=\"background:#27272a;padding:2px 6px;border-radius:4px;\">$1</code>")
        .replace("\n\n", "<p>")

    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>$title</title>
          <style>
            body { background: #09090b; color: #f4f4f5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 20px; line-height: 1.6; }
            h1, h2, h3 { color: #ffffff; border-bottom: 1px solid #27272a; padding-bottom: 8px; }
            code, pre { font-family: monospace; }
          </style>
        </head>
        <body>
          $parsed
        </body>
        </html>
    """.trimIndent()
}

private fun buildJsRunnerHtml(jsCode: String, title: String): String {
    val escapedJs = jsCode.replace("</script>", "<\\/script>")
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>$title</title>
          <style>
            body { background: #121212; color: #fff; font-family: monospace; padding: 16px; }
            #log-box { background: #1e1e1e; border: 1px solid #333; border-radius: 8px; padding: 12px; margin-top: 12px; }
            .log-item { padding: 4px 0; border-bottom: 1px solid #2a2a2a; color: #4ade80; }
          </style>
        </head>
        <body>
          <h3>Menjalankan JavaScript: $title</h3>
          <p style="color:#aaa;">Output dieksekusi di WebView browser. Lihat hasil log di bawah dan di panel konsol:</p>
          <div id="log-box"></div>
          <script>
            const box = document.getElementById('log-box');
            const origLog = console.log;
            console.log = function(...args) {
              origLog.apply(console, args);
              const div = document.createElement('div');
              div.className = 'log-item';
              div.textContent = args.join(' ');
              box.appendChild(div);
            };
            try {
              $escapedJs
            } catch(e) {
              console.error(e);
            }
          </script>
        </body>
        </html>
    """.trimIndent()
}
