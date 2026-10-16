package com.nanascode.app.feature.editor

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nanascode.app.core.ui.theme.NanasDarkBg
import com.nanascode.app.core.ui.theme.NanasDarkSurface
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import com.nanascode.app.core.ui.theme.NanasYellow
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePreviewDialog(
    workspaceRoot: String,
    activeFilePath: String?,
    onDismiss: () -> Unit
) {
    // Tentukan URL/File preview: Prioritaskan active file jika HTML, atau index.html di root
    val previewFile = remember(workspaceRoot, activeFilePath) {
        val active = activeFilePath?.let { File(it) }
        if (active != null && active.exists() && (active.extension.lowercase() in listOf("html", "htm"))) {
            active
        } else {
            val rootIndex = File(workspaceRoot, "index.html")
            if (rootIndex.exists()) rootIndex else active
        }
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }

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
                            Text(text = "🌐", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live Web Preview (${previewFile?.name ?: "No File"})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NanasYellow
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NanasDarkSurface)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    if (previewFile != null && previewFile.exists()) {
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
                                    webChromeClient = WebChromeClient()

                                    loadUrl("file://${previewFile.absolutePath}")
                                    webViewRef = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada file HTML (misal: index.html) di workspace untuk dijalankan.",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (isLoading && previewFile != null) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).size(36.dp),
                            color = NanasYellow
                        )
                    }
                }
            }
        }
    }
}
