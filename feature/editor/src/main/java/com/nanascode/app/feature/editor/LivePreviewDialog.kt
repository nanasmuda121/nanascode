package com.nanascode.app.feature.editor

import android.annotation.SuppressLint
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
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import com.nanascode.app.core.ui.theme.NanasTextSecondary
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
    onOpenTerminalRunner: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isViteOrReactProject = remember(workspaceRoot) {
        val root = File(workspaceRoot)
        File(root, "vite.config.js").exists() ||
        File(root, "vite.config.ts").exists() ||
        File(root, "vite.config.mjs").exists() ||
        File(root, "src/App.tsx").exists() ||
        File(root, "src/App.jsx").exists() ||
        (File(root, "index.html").exists() && File(root, "index.html").readText().contains("/src/main"))
    }

    // Tentukan URL preview:
    // JANGAN PERNAH me-load file:// untuk proyek Vite/React karena akan menghasilkan layar putih!
    val previewTarget = remember(workspaceRoot, activeFilePath, previewUrl, isViteOrReactProject) {
        if (!previewUrl.isNullOrBlank()) {
            return@remember previewUrl
        }
        if (isViteOrReactProject) {
            // Proyek React/Vite tidak boleh dibuka via file://
            return@remember null
        }
        val active = activeFilePath?.let { File(it) }
        if (active != null && active.exists() && active.extension.lowercase() in listOf("html", "htm")) {
            "file://${active.absolutePath}"
        } else {
            val rootIndex = File(workspaceRoot, "index.html")
            if (rootIndex.exists()) {
                "file://${rootIndex.absolutePath}"
            } else {
                null
            }
        }
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var consoleLogs by remember { mutableStateOf<List<WebConsoleLog>>(emptyList()) }
    var isConsoleExpanded by remember { mutableStateOf(false) }

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
                                text = if (previewUrl != null) "Localhost ($previewUrl)" else "Web Preview (HTML)",
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
                        if (previewTarget != null) {
                            IconButton(onClick = { webViewRef?.reload() }) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = NanasTextPrimary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NanasDarkSurface)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                ) {
                    if (previewTarget != null) {
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

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoading = false
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                            if (consoleMessage != null) {
                                                val log = WebConsoleLog(
                                                    message = consoleMessage.message(),
                                                    sourceId = consoleMessage.sourceId() ?: "",
                                                    lineNumber = consoleMessage.lineNumber(),
                                                    level = consoleMessage.messageLevel()
                                                )
                                                consoleLogs = consoleLogs + log
                                            }
                                            return super.onConsoleMessage(consoleMessage)
                                        }
                                    }

                                    loadUrl(previewTarget)
                                    webViewRef = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isViteOrReactProject) {
                        // Layar penjelasan jujur untuk React/Vite (tidak boleh file://)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NanasDarkBg)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "React/Vite Membutuhkan Dev Server",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Proyek Vite menggunakan modul ES (<script type=\"module\">) yang diblokir oleh browser pada protokol file:// (menyebabkan layar putih).\n\nSilakan jalankan 'npm run dev' di Termux atau terminal bawaan, lalu buka melalui Localhost Preview (port 5173).",
                                    color = NanasTextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                if (onOpenTerminalRunner != null) {
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onOpenTerminalRunner()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Jalankan di Termux / Terminal", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // Tidak ada file HTML
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NanasDarkBg)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = NanasTextSecondary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Tidak Ada Berkas Web HTML",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Live preview hanya untuk proyek web HTML statis. Berkas script memerlukan runtime terminal Termux atau Milestone 4.",
                                    color = NanasTextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    if (isLoading && previewTarget != null) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp),
                            color = Color.Black
                        )
                    }
                }

                // Panel Console WebView (Menampilkan Error Console agar penyebab layar putih terlihat)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NanasDarkSurface)
                        .border(1.dp, NanasDarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isConsoleExpanded = !isConsoleExpanded }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WebView Console (${consoleLogs.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (errorCount > 0) NanasError else Color.White
                        )
                        if (errorCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NanasError)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$errorCount ERROR",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (isConsoleExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = NanasTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isConsoleExpanded) {
                        HorizontalDivider(color = NanasDarkBorder)
                        if (consoleLogs.isEmpty()) {
                            Text(
                                text = "Tidak ada log atau pesan error dari WebView.",
                                fontSize = 11.sp,
                                color = NanasTextSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .background(Color.Black)
                                    .padding(8.dp)
                            ) {
                                items(consoleLogs) { log ->
                                    val logColor = when (log.level) {
                                        ConsoleMessage.MessageLevel.ERROR -> NanasError
                                        ConsoleMessage.MessageLevel.WARNING -> Color(0xFFF59E0B)
                                        else -> Color(0xFFE4E4E7)
                                    }
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(
                                            text = "[${log.level.name}] ${log.message}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = logColor
                                        )
                                        if (log.sourceId.isNotEmpty() || log.lineNumber > 0) {
                                            Text(
                                                text = "  at ${log.sourceId}:${log.lineNumber}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = Color(0xFF71717A)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
