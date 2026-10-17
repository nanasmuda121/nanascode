package com.nanascode.app.feature.editor.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.FolderZip
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagerDialog(
    onDismiss: () -> Unit,
    onPluginsChanged: () -> Unit
) {
    val installedList by PluginManager.installedPlugins.collectAsState()
    val marketplaceCatalog = remember(installedList) {
        PluginManager.getMarketplaceCatalog()
    }

    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showCustomInstallDialog by remember { mutableStateOf(false) }
    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }
    var pendingManifest by remember { mutableStateOf<NasCodePluginManifest?>(null) }
    var installErrorMessage by remember { mutableStateOf<String?>(null) }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val res = PluginManager.inspectZipManifest(stream)
                    res.onSuccess { manifest ->
                        pendingZipUri = uri
                        pendingManifest = manifest
                    }.onFailure { err ->
                        installErrorMessage = err.message ?: "Berkas ZIP plugin tidak valid"
                    }
                }
            } catch (e: Exception) {
                installErrorMessage = e.message ?: "Gagal membaca berkas ZIP"
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
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Plugin NasCode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Ekstensi & Plugin JavaScript NasCode",
                                    fontSize = 11.sp,
                                    color = NanasTextSecondary
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = NanasTextPrimary
                                )
                            }
                        },
                        actions = {
                            // Pasang dari berkas ZIP lokal
                            IconButton(onClick = { zipPickerLauncher.launch("application/zip") }) {
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = "Pasang dari Berkas ZIP",
                                    tint = Color.White
                                )
                            }
                            // Pasang kustom dari script JS
                            IconButton(onClick = { showCustomInstallDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Pasang Kustom",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = NanasDarkSurface)
                    )

                    // Tab Row
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = NanasDarkSurface,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = Color.White
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Text(
                                    text = "Toko Plugin (${marketplaceCatalog.size})",
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == 0) Color.White else NanasTextSecondary
                                )
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Text(
                                    text = "Terpasang (${installedList.size})",
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == 1) Color.White else NanasTextSecondary
                                )
                            }
                        )
                    }

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Cari nama atau fungsi plugin...", color = NanasTextSecondary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NanasTextSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus", tint = NanasTextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NanasDarkSurface,
                            unfocusedContainerColor = NanasDarkSurface,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = NanasDarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Content List
                    val displayedList = if (selectedTabIndex == 0) {
                        marketplaceCatalog.filter {
                            searchQuery.isBlank() ||
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true) ||
                            it.category.contains(searchQuery, ignoreCase = true)
                        }
                    } else {
                        installedList.filter {
                            searchQuery.isBlank() ||
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (displayedList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = NanasTextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (selectedTabIndex == 0) "Tidak ada plugin yang cocok" else "Belum ada plugin terpasang",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (selectedTabIndex == 0) "Coba kata kunci pencarian lain" else "Buka tab Toko Plugin untuk memasang ekstensi baru",
                                    color = NanasTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        ) {
                            items(displayedList, key = { it.id }) { plugin ->
                                PluginItemCard(
                                    plugin = plugin,
                                    isMarketplace = selectedTabIndex == 0,
                                    onInstall = {
                                        PluginManager.installPlugin(plugin.id)
                                        onPluginsChanged()
                                    },
                                    onUninstall = {
                                        PluginManager.uninstallPlugin(plugin.id)
                                        onPluginsChanged()
                                    },
                                    onToggle = { enabled ->
                                        PluginManager.togglePlugin(plugin.id, enabled)
                                        onPluginsChanged()
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }

                // Floating Action Button: Pasang Kustom
                FloatingActionButton(
                    onClick = { showCustomInstallDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                    containerColor = Color.White,
                    contentColor = Color.Black
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pasang Kustom", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showCustomInstallDialog) {
        CustomPluginInstallDialog(
            onDismiss = { showCustomInstallDialog = false },
            onInstallSuccess = {
                showCustomInstallDialog = false
                onPluginsChanged()
            }
        )
    }

    // Dialog Konfirmasi Izin Plugin dari Berkas ZIP
    pendingManifest?.let { manifest ->
        AlertDialog(
            onDismissRequest = {
                pendingManifest = null
                pendingZipUri = null
            },
            title = {
                Text("Konfirmasi Pemasangan Plugin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(text = "${manifest.name} (v${manifest.version})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (manifest.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = manifest.description, color = NanasTextSecondary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Izin yang diminta:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (manifest.permissions.isEmpty()) {
                        Text(text = "• Plugin ini tidak meminta izin khusus.", color = NanasGreen, fontSize = 12.sp)
                    } else {
                        for (perm in manifest.permissions) {
                            Text(
                                text = "• ${PluginPermissions.describe(perm)}",
                                color = NanasYellow,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingZipUri
                        if (uri != null) {
                            try {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    val res = PluginManager.installFromZip(stream)
                                    res.onSuccess {
                                        onPluginsChanged()
                                    }.onFailure { err ->
                                        installErrorMessage = err.message ?: "Gagal memasang plugin"
                                    }
                                }
                            } catch (e: Exception) {
                                installErrorMessage = e.message ?: "Kesalahan saat membaca ZIP"
                            }
                        }
                        pendingManifest = null
                        pendingZipUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Izinkan & Pasang", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        pendingManifest = null
                        pendingZipUri = null
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Batal", color = NanasTextPrimary)
                }
            },
            containerColor = NanasDarkSurface,
            shape = RoundedCornerShape(14.dp)
        )
    }

    // Dialog Pesan Error Pemasangan
    installErrorMessage?.let { errMsg ->
        AlertDialog(
            onDismissRequest = { installErrorMessage = null },
            title = { Text("Pemasangan Gagal", color = NanasError, fontWeight = FontWeight.Bold) },
            text = { Text(text = errMsg, color = NanasTextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = { installErrorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup")
                }
            },
            containerColor = NanasDarkSurface,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun PluginItemCard(
    plugin: NasCodePlugin,
    isMarketplace: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NanasDarkBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = NanasDarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(NanasDarkSurfaceLight, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconVector: ImageVector = when (plugin.icon) {
                            "format" -> Icons.Default.Brush
                            "code" -> Icons.Default.Code
                            "palette" -> Icons.Default.Palette
                            "terminal" -> Icons.Default.Terminal
                            else -> Icons.Default.Extension
                        }
                        Icon(imageVector = iconVector, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plugin.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "v${plugin.version}",
                                fontSize = 11.sp,
                                color = NanasTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "oleh ${plugin.author}",
                            fontSize = 11.sp,
                            color = NanasTextSecondary
                        )
                    }
                }

                // Category Chip
                Box(
                    modifier = Modifier
                        .background(NanasDarkSurfaceLight, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = plugin.category, fontSize = 11.sp, color = NanasYellow)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = plugin.description,
                fontSize = 12.sp,
                color = NanasTextSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = NanasDarkBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isMarketplace) {
                    if (plugin.isInstalled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = NanasGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Terpasang", color = NanasGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = onUninstall,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NanasError)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus", fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = onInstall,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pasang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Tab Terpasang: Switch Aktif/Nonaktif & Tombol Hapus
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = plugin.isEnabled,
                            onCheckedChange = onToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = NanasTextSecondary,
                                uncheckedTrackColor = NanasDarkSurfaceLight
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (plugin.isEnabled) "Aktif" else "Nonaktif",
                            fontSize = 12.sp,
                            color = if (plugin.isEnabled) Color.White else NanasTextSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = onUninstall,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NanasError)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomPluginInstallDialog(
    onDismiss: () -> Unit,
    onInstallSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var jsCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pasang Plugin Kustom", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text(
                    text = "Tulis atau tempel kode JavaScript plugin untuk CodeMirror/Acode API:",
                    color = NanasTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Plugin", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NanasDarkSurface,
                        unfocusedContainerColor = NanasDarkSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Pembuat (Author)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NanasDarkSurface,
                        unfocusedContainerColor = NanasDarkSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = jsCode,
                    onValueChange = { jsCode = it },
                    label = { Text("Kode JavaScript (Plugin.js)", fontSize = 12.sp) },
                    placeholder = { Text("window.NasCodePlugins.register('my-plugin', { ... });", fontSize = 11.sp, color = NanasTextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NanasDarkSurface,
                        unfocusedContainerColor = NanasDarkSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, color = NanasError, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || jsCode.isBlank()) {
                        errorMessage = "Nama dan kode JavaScript wajib diisi"
                        return@Button
                    }
                    val result = PluginManager.installCustomPlugin(
                        name = name,
                        author = if (author.isBlank()) "Pengguna" else author,
                        description = if (description.isBlank()) "Plugin kustom pengguna" else description,
                        jsCode = jsCode
                    )
                    result.onSuccess {
                        onInstallSuccess()
                    }.onFailure { err ->
                        errorMessage = err.message
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Pasang", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Batal", color = NanasTextPrimary)
            }
        },
        containerColor = NanasDarkSurface,
        shape = RoundedCornerShape(14.dp)
    )
}
