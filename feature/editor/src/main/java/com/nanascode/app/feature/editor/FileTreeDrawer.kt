package com.nanascode.app.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanascode.app.core.ui.NasCodeLogo
import com.nanascode.app.core.ui.theme.NanasDarkBorder
import com.nanascode.app.core.ui.theme.NanasDarkSurface
import com.nanascode.app.core.ui.theme.NanasDarkSurfaceLight
import com.nanascode.app.core.ui.theme.NanasError
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import com.nanascode.app.core.ui.theme.NanasTextSecondary
import com.nanascode.app.core.ui.theme.NanasYellow
import java.io.File

@Composable
fun FileTreeDrawer(
    rootNode: FileNode?,
    activeFilePath: String?,
    onFileClick: (File) -> Unit,
    onFolderToggle: (String) -> Unit,
    onCreateFile: (File, String) -> Unit,
    onCreateFolder: (File, String) -> Unit,
    onDeleteFile: (File) -> Unit,
    onOpenFolderSaf: () -> Unit = {},
    onOpenTemplates: () -> Unit = {},
    onOpenPlugins: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenGitHub: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var targetParentDir by remember { mutableStateOf<File?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(NanasDarkSurface)
            .padding(vertical = 8.dp)
    ) {
        // App Branding Header (Acode Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NasCodeLogo(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "NasCode",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Editor",
                fontSize = 13.sp,
                color = NanasTextSecondary
            )
        }

        HorizontalDivider(color = NanasDarkBorder, thickness = 0.8.dp)

        // Section: File Explorer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BERKAS PROYEK",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = NanasYellow,
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    targetParentDir = rootNode?.let { File(it.path) }
                    showCreateFileDialog = true
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = "File Baru",
                    tint = NanasTextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }

            IconButton(
                onClick = {
                    targetParentDir = rootNode?.let { File(it.path) }
                    showCreateFolderDialog = true
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = "Folder Baru",
                    tint = NanasTextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // File Tree List
        if (rootNode == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Memuat file...", color = NanasTextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item {
                    FileTreeItem(
                        node = rootNode,
                        depth = 0,
                        activeFilePath = activeFilePath,
                        onFileClick = onFileClick,
                        onFolderToggle = onFolderToggle,
                        onDeleteClick = { fileToDelete = File(it.path) }
                    )
                }
            }
        }

        // Sidebar Navigation Actions (Acode Style Menu)
        HorizontalDivider(color = NanasDarkBorder, thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(4.dp))

        DrawerMenuItem(
            icon = Icons.Default.FolderOpen,
            label = "Buka Folder...",
            onClick = onOpenFolderSaf
        )
        DrawerMenuItem(
            icon = Icons.Default.Dashboard,
            label = "Template Proyek",
            onClick = onOpenTemplates
        )
        DrawerMenuItem(
            icon = Icons.Default.Extension,
            label = "Toko Plugin",
            onClick = onOpenPlugins
        )
        DrawerMenuItem(
            icon = Icons.Default.Settings,
            label = "Pengaturan Editor",
            onClick = onOpenSettings
        )
        DrawerMenuItem(
            icon = Icons.Default.Cloud,
            label = "GitHub Repositori",
            onClick = onOpenGitHub
        )
    }

    // Dialog Create File
    if (showCreateFileDialog && targetParentDir != null) {
        var inputName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Buat File Baru", color = NanasTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    placeholder = { Text("index.html, script.js, style.css...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NanasYellow,
                        focusedTextColor = NanasTextPrimary,
                        unfocusedTextColor = NanasTextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = inputName.trim()
                        if (name.isNotBlank()) {
                            onCreateFile(targetParentDir!!, name)
                        }
                        showCreateFileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NanasYellow, contentColor = Color.Black)
                ) {
                    Text("Buat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Batal", color = NanasTextSecondary)
                }
            },
            containerColor = NanasDarkSurface
        )
    }

    // Dialog Create Folder
    if (showCreateFolderDialog && targetParentDir != null) {
        var inputFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Buat Folder Baru", color = NanasTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = inputFolderName,
                    onValueChange = { inputFolderName = it },
                    placeholder = { Text("src, assets, css, components...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NanasYellow,
                        focusedTextColor = NanasTextPrimary,
                        unfocusedTextColor = NanasTextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = inputFolderName.trim()
                        if (name.isNotBlank()) {
                            onCreateFolder(targetParentDir!!, name)
                        }
                        showCreateFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NanasYellow, contentColor = Color.Black)
                ) {
                    Text("Buat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Batal", color = NanasTextSecondary)
                }
            },
            containerColor = NanasDarkSurface
        )
    }

    // Dialog Delete File Confirmation
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Hapus ${if (file.isDirectory) "Folder" else "File"}?", color = NanasError, fontWeight = FontWeight.Bold) },
            text = {
                Text("Yakin ingin menghapus '${file.name}'? Tindakan ini tidak dapat dibatalkan.", color = NanasTextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(file)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NanasError, contentColor = Color.White)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Batal", color = NanasTextSecondary)
                }
            },
            containerColor = NanasDarkSurface
        )
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FileTreeItem(
    node: FileNode,
    depth: Int,
    activeFilePath: String?,
    onFileClick: (File) -> Unit,
    onFolderToggle: (String) -> Unit,
    onDeleteClick: (FileNode) -> Unit
) {
    val isSelected = activeFilePath == node.path

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) NanasDarkSurfaceLight else Color.Transparent)
            .clickable {
                if (node.isDirectory) {
                    onFolderToggle(node.path)
                } else {
                    onFileClick(File(node.path))
                }
            }
            .padding(start = (depth * 14 + 10).dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = node.icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = node.name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) NanasYellow else NanasTextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        if (depth > 0) {
            IconButton(
                onClick = { onDeleteClick(node) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = NanasTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    if (node.isDirectory && node.isExpanded) {
        for (child in node.children) {
            FileTreeItem(
                node = child,
                depth = depth + 1,
                activeFilePath = activeFilePath,
                onFileClick = onFileClick,
                onFolderToggle = onFolderToggle,
                onDeleteClick = onDeleteClick
            )
        }
    }
}
