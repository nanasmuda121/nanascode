package com.nanascode.app.feature.editor

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
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
import com.nanascode.app.feature.editor.plugin.PluginManager
import com.nanascode.app.feature.editor.plugin.PluginManagerDialog
import kotlinx.coroutines.launch

private val QUICK_SYMBOLS = listOf(
    "Tab", "Undo", "Redo", "{", "}", "(", ")", "[", "]", "<", ">", "=", ";", ":",
    "\"", "'", "`", "/", "\\", "|", "&", "!", "?", "_", "-", "+", "*", "$"
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onOpenSafFolder: () -> Unit = {},
    onOpenWorkspaces: () -> Unit = {},
    onOpenGitHub: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
    var showPluginDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var editorConfig by remember { mutableStateOf(EditorConfig()) }

    val activeTab = remember(tabs, activeTabId) {
        tabs.find { it.id == activeTabId }
    }

    // Inisialisasi Plugin Manager
    LaunchedEffect(Unit) {
        PluginManager.init(context)
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    fun injectPluginsAndConfig(wv: WebView?) {
        if (wv == null || !isEditorReady) return
        val pluginsJs = PluginManager.getActivePluginsJs()
        if (pluginsJs.isNotBlank()) {
            wv.evaluateJavascript(pluginsJs, null)
        }
        wv.evaluateJavascript("window.setFontSize(${editorConfig.fontSize});", null)
        wv.evaluateJavascript("window.setTabSize(${editorConfig.tabSize});", null)
        wv.evaluateJavascript("window.setWordWrap(${editorConfig.wordWrap});", null)
        wv.evaluateJavascript("window.setShowLineNumbers(${editorConfig.showLineNumbers});", null)
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
            injectPluginsAndConfig(webViewRef)
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
                    onDeleteFile = viewModel::deleteFile,
                    onOpenFolderSaf = {
                        scope.launch { drawerState.close() }
                        onOpenSafFolder()
                    },
                    onOpenTemplates = {
                        scope.launch { drawerState.close() }
                        onOpenWorkspaces()
                    },
                    onOpenPlugins = {
                        scope.launch { drawerState.close() }
                        showPluginDialog = true
                    },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        showSettingsDialog = true
                    },
                    onOpenGitHub = {
                        scope.launch { drawerState.close() }
                        onOpenGitHub()
                    }
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
                                    color = Color.White
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
                        // Play Button: Pratinjau Web Langsung (Acode Style)
                        IconButton(
                            onClick = { showLivePreview = true },
                            enabled = workspacePath != null
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Pratinjau Web", tint = Color.White)
                        }

                        // Tombol Toko Plugin
                        IconButton(onClick = { showPluginDialog = true }) {
                            Icon(imageVector = Icons.Default.Extension, contentDescription = "Plugin", tint = Color.White)
                        }

                        // Tombol Simpan
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

                        // Overflow Menu
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                modifier = Modifier.background(NanasDarkSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Format Kode (Prettier)", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignLeft, contentDescription = null, tint = Color.White) },
                                    onClick = {
                                        showOverflowMenu = false
                                        webViewRef?.evaluateJavascript("window.formatCode();", null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Toko Plugin", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null, tint = Color.White) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showPluginDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pengaturan Editor", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showSettingsDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NanasDarkSurface)
                )
            },
            bottomBar = {
                // Mobile Accessory Bar (Acode Style di atas keyboard touch)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NanasDarkSurface)
                ) {
                    // Status Bar: Posisi kursor & Bahasa
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeTab != null) "Baris ${activeTab.cursorLine}, Kolom ${activeTab.cursorColumn}" else "--:--",
                            fontSize = 11.sp,
                            color = NanasTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeTab?.language?.uppercase() ?: "PLAIN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            if (activeTab?.isDirty == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(diubah)",
                                    fontSize = 10.sp,
                                    color = NanasYellow
                                )
                            }
                        }
                    }

                    // Horizontal Quick Coding Accessory Bar (Acode Style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (sym in QUICK_SYMBOLS) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NanasDarkSurfaceLight)
                                    .border(1.dp, NanasDarkBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        when (sym) {
                                            "Undo" -> webViewRef?.evaluateJavascript("window.undo();", null)
                                            "Redo" -> webViewRef?.evaluateJavascript("window.redo();", null)
                                            else -> {
                                                val safeSym = sym.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
                                                webViewRef?.evaluateJavascript("window.insertSymbol('$safeSym');", null)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 11.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (sym) {
                                    "Undo" -> Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo", tint = Color.White, modifier = Modifier.size(14.dp))
                                    "Redo" -> Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo", tint = Color.White, modifier = Modifier.size(14.dp))
                                    else -> Text(
                                        text = sym,
                                        color = if (sym == "Tab") NanasYellow else Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                // Horizontal Tab Bar (Acode Style)
                if (tabs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NanasDarkSurface)
                            .border(width = 0.5.dp, color = NanasDarkBorder)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        for (tab in tabs) {
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
                                    color = if (isSelected) Color.White else NanasTextSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.requestCloseTab(tab.id) },
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
                    }
                }

                // Area Editor CodeMirror WebView
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
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.allowFileAccess = true
                                    settings.allowContentAccess = true

                                    addJavascriptInterface(
                                        EditorJsBridge(
                                            onContentChange = viewModel::updateActiveTabContent,
                                            onCursorMove = viewModel::updateCursor,
                                            onReady = {
                                                isEditorReady = true
                                                injectPluginsAndConfig(this)
                                            }
                                        ),
                                        "AndroidBridge"
                                    )

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isEditorReady = true
                                            injectPluginsAndConfig(this@apply)
                                        }
                                    }

                                    loadUrl("file:///android_asset/codemirror/index.html")
                                    webViewRef = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Empty Workspace / Scratchpad Prompt
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NanasDarkBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Tidak Ada Berkas yang Dibuka",
                                    color = NanasTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Buka berkas dari panel samping atau pilih folder proyek.",
                                    color = NanasTextSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Text("Buka Penjelajah Berkas", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Live Web Preview (Acode Style)
    if (showLivePreview && workspacePath != null) {
        LivePreviewDialog(
            workspaceRoot = workspacePath!!,
            activeFilePath = activeTab?.path,
            onDismiss = { showLivePreview = false }
        )
    }

    // Dialog: Toko & Pengelola Plugin
    if (showPluginDialog) {
        PluginManagerDialog(
            onDismiss = { showPluginDialog = false },
            onPluginsChanged = {
                injectPluginsAndConfig(webViewRef)
            }
        )
    }

    // Dialog: Pengaturan Editor
    if (showSettingsDialog) {
        EditorSettingsDialog(
            initialConfig = editorConfig,
            onApplyConfig = { newConfig ->
                editorConfig = newConfig
                injectPluginsAndConfig(webViewRef)
            },
            onDismiss = { showSettingsDialog = false }
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
