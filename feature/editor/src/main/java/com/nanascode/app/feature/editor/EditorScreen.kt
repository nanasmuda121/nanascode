package com.nanascode.app.feature.editor

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.nanascode.app.core.ui.theme.NanasDarkBg
import com.nanascode.app.core.ui.theme.NanasDarkBorder
import com.nanascode.app.core.ui.theme.NanasDarkSurface
import com.nanascode.app.core.ui.theme.NanasDarkSurfaceLight
import com.nanascode.app.core.ui.theme.NanasError
import com.nanascode.app.core.ui.theme.NanasGreen
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import com.nanascode.app.core.ui.theme.NanasTextSecondary
import com.nanascode.app.core.ui.theme.NanasYellow
import kotlinx.coroutines.launch

private val QUICK_SYMBOLS = listOf(
    "Tab", "{", "}", "(", ")", "[", "]", "<", ">", "=", ";", ":",
    "\"", "'", "`", "/", "\\", "|", "&", "!", "?", "_", "-", "+", "*", "$"
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val workspacePath by viewModel.workspacePath.collectAsState()
    val workspaceName by viewModel.workspaceName.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val fileTree by viewModel.fileTree.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val unsavedTabToClose by viewModel.showUnsavedCloseDialog.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isEditorReady by remember { mutableStateOf(false) }
    var showLivePreview by remember { mutableStateOf(false) }

    val activeTab = remember(tabs, activeTabId) {
        tabs.find { it.id == activeTabId }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Sinkronisasi tab aktif ke WebView CodeMirror
    LaunchedEffect(activeTab?.id, isEditorReady) {
        if (isEditorReady && activeTab != null && webViewRef != null) {
            val safeContent = activeTab.content
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
            val jsCode = "window.setEditorContent(`$safeContent`, '${activeTab.language}');"
            webViewRef?.evaluateJavascript(jsCode, null)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = NanasDarkSurface,
                modifier = Modifier.width(300.dp)
            ) {
                FileTreeDrawer(
                    rootNode = fileTree,
                    activeFilePath = activeTab?.path,
                    onFileClick = { file ->
                        viewModel.openFile(file)
                        scope.launch { drawerState.close() }
                    },
                    onFolderToggle = viewModel::toggleFolder,
                    onCreateFile = viewModel::createFile,
                    onCreateFolder = viewModel::createFolder,
                    onDeleteFile = viewModel::deleteFile
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize().imePadding(),
            containerColor = NanasDarkBg,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = workspaceName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NanasYellow
                                )
                                if (activeTab?.isDirty == true) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(NanasYellow, CircleShape)
                                    )
                                }
                            }
                            Text(
                                text = activeTab?.name ?: "Pilih file...",
                                fontSize = 11.sp,
                                color = NanasTextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Buka File Explorer", tint = NanasTextPrimary)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showLivePreview = true },
                            enabled = workspacePath != null
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Live Preview", tint = NanasGreen)
                        }
                        IconButton(
                            onClick = viewModel::saveActiveFile,
                            enabled = activeTab?.isDirty == true
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Simpan File",
                                tint = if (activeTab?.isDirty == true) NanasYellow else NanasTextSecondary
                            )
                        }
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali", tint = NanasTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NanasDarkSurface)
                )
            },
            bottomBar = {
                // Mobile Accessory Bar (di atas keyboard touch)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NanasDarkSurface)
                ) {
                    // Status Bar: Posisi kursor & Bahasa
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (activeTab != null) "Ln ${activeTab.cursorLine}, Col ${activeTab.cursorColumn}" else "--:--",
                            fontSize = 11.sp,
                            color = NanasTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = activeTab?.language?.uppercase() ?: "TEXT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NanasYellow
                        )
                    }

                    // Baris Tombol Simbol Cepat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Undo / Redo
                        IconButton(
                            onClick = { webViewRef?.evaluateJavascript("window.undo();", null) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo", tint = NanasTextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { webViewRef?.evaluateJavascript("window.redo();", null) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo", tint = NanasTextPrimary, modifier = Modifier.size(16.dp))
                        }

                        // Simbol
                        QUICK_SYMBOLS.forEach { sym ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NanasDarkSurfaceLight)
                                    .clickable {
                                        val escaped = if (sym == "\"") "\\\"" else if (sym == "\\") "\\\\" else sym
                                        webViewRef?.evaluateJavascript("window.insertSymbol('$escaped');", null)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sym,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (sym == "Tab") NanasYellow else NanasTextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Multi-Tab Scrollable Bar
                if (tabs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NanasDarkSurfaceLight)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            val isSelected = tab.id == activeTabId
                            Row(
                                modifier = Modifier
                                    .background(if (isSelected) NanasDarkBg else NanasDarkSurface)
                                    .clickable { viewModel.selectTab(tab.id) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (tab.isDirty) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(NanasYellow, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = tab.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NanasYellow else NanasTextSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup Tab",
                                    tint = NanasTextSecondary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.requestCloseTab(tab.id) }
                                )
                            }
                        }
                    }
                }

                // Editor WebView
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(NanasDarkBg)
                ) {
                    if (activeTab != null) {
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

                                    addJavascriptInterface(
                                        EditorJsBridge(
                                            onContentChange = { newContent ->
                                                viewModel.updateActiveTabContent(newContent)
                                            },
                                            onCursorMove = { line, col ->
                                                viewModel.updateCursor(line, col)
                                            },
                                            onReady = {
                                                isEditorReady = true
                                            }
                                        ),
                                        "AndroidBridge"
                                    )

                                    webViewClient = WebViewClient()
                                    loadUrl("file:///android_asset/codemirror/index.html")
                                    webViewRef = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Empty editor state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📝", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Tidak Ada File Terbuka",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = NanasTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Buka menu drawer di pojok kiri atas untuk memilih atau membuat file baru.",
                                    fontSize = 13.sp,
                                    color = NanasTextSecondary,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Text("Buka Penjelajah File", color = NanasYellow)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Live Web Preview
    if (showLivePreview && workspacePath != null) {
        LivePreviewDialog(
            workspaceRoot = workspacePath!!,
            activeFilePath = activeTab?.path,
            onDismiss = { showLivePreview = false }
        )
    }

    // Dialog: Konfirmasi Unsaved Tab
    unsavedTabToClose?.let { tab ->
        AlertDialog(
            onDismissRequest = viewModel::dismissUnsavedDialog,
            title = { Text("Simpan Perubahan?", color = NanasTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "File '${tab.name}' memiliki perubahan yang belum disimpan. Yakin ingin menutup tanpa menyimpan?",
                    color = NanasTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveActiveFile()
                        viewModel.forceCloseTab(tab.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NanasYellow, contentColor = Color.Black)
                ) {
                    Text("Simpan & Tutup")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.forceCloseTab(tab.id) }) {
                    Text("Tutup Tanpa Menyimpan", color = NanasError)
                }
            },
            containerColor = NanasDarkSurface
        )
    }
}
