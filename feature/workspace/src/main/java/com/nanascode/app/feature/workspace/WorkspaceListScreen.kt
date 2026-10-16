package com.nanascode.app.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanascode.app.core.model.ProjectSource
import com.nanascode.app.core.ui.theme.NanasDarkBg
import com.nanascode.app.core.ui.theme.NanasDarkBorder
import com.nanascode.app.core.ui.theme.NanasDarkSurface
import com.nanascode.app.core.ui.theme.NanasDarkSurfaceLight
import com.nanascode.app.core.ui.theme.NanasError
import com.nanascode.app.core.ui.theme.NanasGreen
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import com.nanascode.app.core.ui.theme.NanasTextSecondary
import com.nanascode.app.core.ui.theme.NanasYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceListScreen(
    viewModel: WorkspaceViewModel,
    onOpenWorkspace: (ProjectSource.LocalWorkspace) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToGitHubRepos: () -> Unit,
    onImportFolderSaf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val workspaces by viewModel.workspaces.collectAsState()
    val activeToken by viewModel.activeToken.collectAsState()
    val showLoginDialog by viewModel.showLoginRequiredDialog.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedWorkspaceForPublish by remember { mutableStateOf<ProjectSource.LocalWorkspace?>(null) }
    var workspaceToDelete by remember { mutableStateOf<ProjectSource.LocalWorkspace?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🍍", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Workspace NanasCode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = NanasTextPrimary
                        )
                    }
                },
                actions = {
                    if (activeToken != null) {
                        IconButton(onClick = onNavigateToGitHubRepos) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Akun GitHub",
                                tint = NanasYellow
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onNavigateToLogin,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Masuk",
                                fontSize = 12.sp,
                                color = NanasYellow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NanasDarkSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = NanasYellow,
                contentColor = Color.Black
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Proyek Baru")
            }
        },
        containerColor = NanasDarkBg,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Action banner: Import folder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onImportFolderSaf() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NanasDarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = NanasYellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Buka Folder dari Perangkat (SAF)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = NanasTextPrimary
                        )
                        Text(
                            text = "Impor proyek yang sudah ada dari storage Android",
                            fontSize = 12.sp,
                            color = NanasTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Proyek Lokal (${workspaces.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NanasTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (workspaces.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📁", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada Proyek Lokal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NanasTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tekan tombol + di bawah untuk membuat proyek baru dari template Tailwind, React, Node.js, atau proyek kosong.",
                            fontSize = 13.sp,
                            color = NanasTextSecondary,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(workspaces, key = { it.id }) { workspace ->
                        WorkspaceItemCard(
                            workspace = workspace,
                            onOpen = { onOpenWorkspace(workspace) },
                            onPublish = {
                                if (activeToken == null) {
                                    viewModel.onPublishRequested(workspace.id, workspace.name, false, null)
                                } else {
                                    selectedWorkspaceForPublish = workspace
                                }
                            },
                            onDelete = { workspaceToDelete = workspace }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Create New Workspace
    if (showCreateDialog) {
        CreateWorkspaceDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, templateId ->
                viewModel.createWorkspace(name, templateId)
                showCreateDialog = false
            }
        )
    }

    // Dialog: Login Required
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLoginRequiredDialog() },
            title = {
                Text(
                    text = "Login GitHub Diperlukan",
                    fontWeight = FontWeight.Bold,
                    color = NanasTextPrimary
                )
            },
            text = {
                Text(
                    text = "Aksi ini memerlukan autentikasi GitHub. Masuk dengan Personal Access Token untuk mempublikasikan proyek, membuat repo remote, dan menjalankan GitHub Actions.",
                    color = NanasTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissLoginRequiredDialog()
                        onNavigateToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NanasYellow, contentColor = Color.Black)
                ) {
                    Text(text = "Masuk Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLoginRequiredDialog() }) {
                    Text(text = "Nanti Saja", color = NanasTextSecondary)
                }
            },
            containerColor = NanasDarkSurface
        )
    }

    // Dialog: Delete Workspace Confirmation
    workspaceToDelete?.let { ws ->
        AlertDialog(
            onDismissRequest = { workspaceToDelete = null },
            title = { Text(text = "Hapus Proyek?", color = NanasTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus folder proyek '${ws.name}'? Seluruh file dan riwayat git lokal di dalamnya akan dihapus.",
                    color = NanasTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkspace(ws.id)
                        workspaceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NanasError, contentColor = Color.White)
                ) {
                    Text(text = "Hapus Proyek")
                }
            },
            dismissButton = {
                TextButton(onClick = { workspaceToDelete = null }) {
                    Text(text = "Batal", color = NanasTextSecondary)
                }
            },
            containerColor = NanasDarkSurface
        )
    }
}

@Composable
fun WorkspaceItemCard(
    workspace: ProjectSource.LocalWorkspace,
    onOpen: () -> Unit,
    onPublish: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(workspace.lastModifiedAt) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(workspace.lastModifiedAt))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NanasDarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(NanasDarkSurfaceLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = NanasYellow,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workspace.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NanasTextPrimary
                    )
                    Text(
                        text = "Diubah: $dateStr",
                        fontSize = 11.sp,
                        color = NanasTextSecondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = NanasTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPublish,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = NanasYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Publikasi ke GitHub", fontSize = 12.sp, color = NanasYellow)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NanasYellow, contentColor = Color.Black)
                ) {
                    Text(text = "Buka", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
