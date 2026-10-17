package com.nanascode.app.feature.repo

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nanascode.app.core.model.GitHubContentItem
import com.nanascode.app.core.model.GitHubRepo
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
fun RepoExplorerDialog(
    state: RepoExplorerState,
    onDismiss: () -> Unit,
    onSelectBranch: (String) -> Unit,
    onFolderClick: (GitHubContentItem) -> Unit,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onCloneAndOpen: (GitHubRepo) -> Unit
) {
    val repo = state.repo ?: return
    var isBranchDropdownExpanded by remember { mutableStateOf(false) }

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
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = repo.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = NanasYellow,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (repo.isPrivate) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Private",
                                        tint = Color(0xFFFFB74D),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(
                                text = repo.fullName,
                                fontSize = 11.sp,
                                color = NanasTextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = NanasTextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = NanasTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NanasDarkSurface)
                )

                // Branch Picker & Breadcrumb Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NanasDarkSurfaceLight)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Branch Picker Button
                        Box {
                            OutlinedButton(
                                onClick = { isBranchDropdownExpanded = true },
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "branch: ${state.selectedBranch}",
                                    fontSize = 12.sp,
                                    color = NanasTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            DropdownMenu(
                                expanded = isBranchDropdownExpanded,
                                onDismissRequest = { isBranchDropdownExpanded = false },
                                modifier = Modifier.background(NanasDarkSurface)
                            ) {
                                if (state.branches.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(state.selectedBranch, color = NanasTextPrimary, fontSize = 13.sp) },
                                        onClick = { isBranchDropdownExpanded = false }
                                    )
                                } else {
                                    state.branches.forEach { branch ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = branch.name,
                                                    color = if (branch.name == state.selectedBranch) NanasYellow else NanasTextPrimary,
                                                    fontWeight = if (branch.name == state.selectedBranch) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                            },
                                            onClick = {
                                                isBranchDropdownExpanded = false
                                                onSelectBranch(branch.name)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Path / Breadcrumb display
                        Text(
                            text = if (state.currentPath.isBlank()) "/" else "/${state.currentPath}",
                            fontSize = 12.sp,
                            color = NanasTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    // Up Navigation Button if nested
                    if (state.currentPath.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NanasDarkSurface)
                                .clickable(onClick = onNavigateUp)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke folder atas",
                                tint = NanasTextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ke folder induk",
                                fontSize = 12.sp,
                                color = NanasTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                HorizontalDivider(color = NanasDarkBorder)

                // Main Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    when {
                        state.isLoadingContents -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = NanasYellow, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Memuat daftar berkas...", color = NanasTextSecondary, fontSize = 13.sp)
                            }
                        }

                        state.isRepoEmpty -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                                    .align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = NanasTextSecondary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Repo Kosong",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = NanasTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Repositori ini belum memiliki berkas atau commit pada branch '${state.selectedBranch}'.",
                                    color = NanasTextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        state.errorMessage != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NanasDarkSurface)
                                    .border(1.dp, NanasError.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(20.dp)
                                    .align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = NanasError,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Gagal Membaca Berkas Repositori",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NanasTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.errorMessage,
                                    color = NanasTextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onRefresh,
                                    colors = ButtonDefaults.buttonColors(containerColor = NanasYellow)
                                ) {
                                    Text("Coba Lagi", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        state.contents.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Folder ini kosong", color = NanasTextSecondary, fontSize = 13.sp)
                            }
                        }

                        else -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(state.contents, key = { it.sha.ifBlank { it.path } }) { item ->
                                    RepoContentRow(
                                        item = item,
                                        onClick = {
                                            if (item.isDirectory) {
                                                onFolderClick(item)
                                            }
                                        }
                                    )
                                    HorizontalDivider(color = NanasDarkBorder.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                // Footer: Tombol Buka / Clone ke Editor
                Surface(
                    color = NanasDarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Buka di Editor NasCode",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = NanasTextPrimary
                            )
                            Text(
                                text = "Clone repositori ini ke penyimpanan workspace lokal",
                                fontSize = 11.sp,
                                color = NanasTextSecondary
                            )
                        }

                        Button(
                            onClick = { onCloneAndOpen(repo) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Buka di Editor", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoContentRow(
    item: GitHubContentItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = if (item.isDirectory) "Folder" else "File",
            tint = if (item.isDirectory) NanasYellow else NanasTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
                color = if (item.isDirectory) NanasTextPrimary else NanasTextPrimary.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!item.isDirectory && item.size > 0) {
            Text(
                text = item.formattedSize,
                fontSize = 11.sp,
                color = NanasTextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
