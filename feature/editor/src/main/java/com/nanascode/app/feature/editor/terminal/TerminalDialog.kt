/**
 * NasCode Multi-Tab Terminal Dialog with xterm.js & Alpine PRoot
 * Inspired by Acode terminal architecture (https://github.com/acode-foundation/acode)
 * Licensed under the MIT License. See LICENSE-ACODE.txt for details.
 */
package com.nanascode.app.feature.editor.terminal

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nanascode.app.core.terminal.AlpineEnvironmentManager
import com.nanascode.app.core.terminal.TerminalBridge
import com.nanascode.app.core.terminal.TerminalManager
import com.nanascode.app.core.ui.theme.NanasDarkBg
import com.nanascode.app.core.ui.theme.NanasDarkBorder
import com.nanascode.app.core.ui.theme.NanasDarkSurface
import com.nanascode.app.core.ui.theme.NanasDarkSurfaceLight
import com.nanascode.app.core.ui.theme.NanasError
import com.nanascode.app.core.ui.theme.NanasGreen
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import com.nanascode.app.core.ui.theme.NanasTextSecondary
import com.nanascode.app.core.ui.theme.NanasYellow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

private val VIRTUAL_KEYS = listOf(
    "Esc" to "\u001B",
    "Tab" to "\t",
    "Ctrl+C" to "\u0003",
    "Ctrl+D" to "\u0004",
    "Ctrl+Z" to "\u001A",
    "↑" to "\u001B[A",
    "↓" to "\u001B[B",
    "←" to "\u001B[D",
    "→" to "\u001B[C",
    "Enter" to "\r"
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TerminalDialog(
    workspaceDir: File,
    initialCommand: String? = null,
    initialTitle: String? = null,
    onDismiss: () -> Unit,
    onOpenLocalhostPreview: (url: String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sessions by TerminalManager.sessionsList.collectAsState()
    val activeSessionId by TerminalManager.activeSessionId.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isTerminalReady by remember { mutableStateOf(false) }
    var isPreparingAlpine by remember { mutableStateOf(false) }
    var alpineStatusText by remember { mutableStateOf("Menyiapkan Alpine Linux...") }
    var showTermuxNotInstalledDialog by remember { mutableStateOf(false) }

    val activeSession = remember(sessions, activeSessionId) {
        sessions.find { it.id == activeSessionId }
    }

    // Auto-create initial session if empty
    LaunchedEffect(Unit) {
        if (sessions.isEmpty()) {
            if (!AlpineEnvironmentManager.isAlpineInstalled(context)) {
                isPreparingAlpine = true
                scope.launch {
                    AlpineEnvironmentManager.prepareEnvironment(context) { status ->
                        alpineStatusText = status
                    }
                    isPreparingAlpine = false
                    TerminalManager.startSession(
                        context = context,
                        title = initialTitle ?: "Terminal",
                        workDir = workspaceDir,
                        initialCommand = initialCommand,
                        useAlpine = true
                    )
                }
            } else {
                TerminalManager.startSession(
                    context = context,
                    title = initialTitle ?: "Terminal",
                    workDir = workspaceDir,
                    initialCommand = initialCommand,
                    useAlpine = true
                )
            }
        }
    }

    // Stream output of active session to xterm.js
    LaunchedEffect(activeSession?.id, isTerminalReady) {
        if (activeSession != null && isTerminalReady && webViewRef != null) {
            // Restore previous history
            val history = activeSession.getHistory()
            if (history.isNotEmpty()) {
                val escaped = history
                    .replace("\\", "\\\\")
                    .replace("`", "\\`")
                    .replace("$", "\\$")
                webViewRef?.evaluateJavascript("window.clearTerminal(); window.writeOutput(`$escaped`);", null)
            } else {
                webViewRef?.evaluateJavascript("window.clearTerminal();", null)
            }

            // Stream new output chunks
            activeSession.outputFlow.collectLatest { chunk ->
                val escapedChunk = chunk
                    .replace("\\", "\\\\")
                    .replace("`", "\\`")
                    .replace("$", "\\$")
                webViewRef?.evaluateJavascript("window.writeOutput(`$escapedChunk`);", null)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NanasDarkBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Terminal Alpine PRoot",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (activeSession?.isRunning == true) "Aktif (OOM-Protected Service)" else "Selesai",
                                    fontSize = 10.sp,
                                    color = if (activeSession?.isRunning == true) NanasGreen else NanasTextSecondary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = NanasTextPrimary)
                        }
                    },
                    actions = {
                        // Tombol Pratinjau Localhost (Vite / React)
                        IconButton(onClick = { onOpenLocalhostPreview("http://localhost:5173") }) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = "Pratinjau Localhost", tint = Color.White)
                        }

                        // Tombol Jalankan di Termux (Alternatif)
                        IconButton(onClick = {
                            val cmd = initialCommand ?: "bash"
                            if (TermuxHelper.isTermuxInstalled(context)) {
                                TermuxHelper.runInTermux(context, cmd, workspaceDir)
                            } else {
                                showTermuxNotInstalledDialog = true
                            }
                        }) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Jalankan di Termux", tint = NanasYellow)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NanasDarkSurface)
                )

                // Multi-Tab Bar Sesi Terminal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NanasDarkSurface)
                        .border(width = 0.5.dp, color = NanasDarkBorder)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (session in sessions) {
                        val isSelected = session.id == activeSessionId
                        Row(
                            modifier = Modifier
                                .background(if (isSelected) NanasDarkBg else NanasDarkSurface)
                                .clickable { TerminalManager.setActiveSession(session.id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (session.isRunning) NanasGreen else NanasTextSecondary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = session.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else NanasTextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { TerminalManager.closeSession(session.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup Tab",
                                    tint = NanasTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    // Tambah Tab Baru
                    IconButton(
                        onClick = {
                            TerminalManager.startSession(
                                context = context,
                                title = "Tab ${sessions.size + 1}",
                                workDir = workspaceDir,
                                initialCommand = "sh",
                                useAlpine = true
                            )
                        },
                        modifier = Modifier.size(32.dp).padding(horizontal = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Tab Terminal", tint = Color.White)
                    }
                }

                // Area xterm.js WebView
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(NanasDarkBg)
                ) {
                    if (isPreparingAlpine) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = alpineStatusText, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    } else {
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

                                    addJavascriptInterface(
                                        TerminalBridge(
                                            onInputReceived = { input ->
                                                TerminalManager.writeToActive(input)
                                            },
                                            onTerminalReady = {
                                                isTerminalReady = true
                                            }
                                        ),
                                        "TerminalBridge"
                                    )

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isTerminalReady = true
                                            view?.evaluateJavascript("window.fitTerminal(); window.focusTerminal();", null)
                                        }
                                    }

                                    loadUrl("file:///android_asset/terminal/xterm/terminal.html")
                                    webViewRef = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Virtual Keybar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NanasDarkSurface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for ((label, code) in VIRTUAL_KEYS) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NanasDarkSurfaceLight)
                                .border(1.dp, NanasDarkBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    TerminalManager.writeToActive(code)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (label.startsWith("Ctrl")) NanasYellow else Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog Termux Belum Terpasang
    if (showTermuxNotInstalledDialog) {
        AlertDialog(
            onDismissRequest = { showTermuxNotInstalledDialog = false },
            title = { Text("Termux Tidak Ditemukan", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Aplikasi Termux belum terpasang di perangkat Anda. Anda dapat mengunduhnya secara gratis dari GitHub Releases atau F-Droid.",
                    color = NanasTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        TermuxHelper.openDownloadPage(context)
                        showTermuxNotInstalledDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Unduh Termux (GitHub)")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTermuxNotInstalledDialog = false }) {
                    Text("Batal", color = NanasTextPrimary)
                }
            },
            containerColor = NanasDarkSurface
        )
    }
}
