package com.nanascode.app.feature.editor

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
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
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalRunnerDialog(
    workspaceDir: File,
    activeFile: File?,
    projectType: ProjectType,
    onOpenLocalhostPreview: (String) -> Unit,
    onProjectRelocated: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isTermuxInstalled by remember { mutableStateOf(TermuxManager.isTermuxInstalled(context)) }
    var hasStoragePermission by remember { mutableStateOf(TermuxManager.hasAllFilesPermission(context)) }
    var currentDir by remember { mutableStateOf(workspaceDir) }
    val isSharedStorage = remember(currentDir) { TermuxManager.isPathInSharedStorage(currentDir.absolutePath) }

    val defaultCommand = remember(projectType, currentDir, activeFile) {
        TermuxManager.buildTermuxCommand(projectType, currentDir, activeFile)
    }

    var customCommand by remember { mutableStateOf(defaultCommand) }
    var executionStatus by remember { mutableStateOf<TermuxExecutionResult?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var isRelocating by remember { mutableStateOf(false) }

    val localhostUrl = when (projectType) {
        ProjectType.REACT_VITE -> "http://localhost:5173"
        ProjectType.NODE_JS -> "http://localhost:3000"
        ProjectType.PYTHON -> "http://localhost:8000"
        else -> "http://localhost:3000"
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
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Eksekusi Runtime (Termux)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = NanasTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NanasDarkSurface)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    // 1. Info Card Proyek & Perintah
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NanasDarkSurface)
                            .border(1.dp, NanasDarkBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "JENIS PROYEK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NanasTextSecondary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF27272A))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = projectType.label,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Perintah Eksekusi:",
                            fontSize = 12.sp,
                            color = NanasTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = customCommand,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Folder Kerja: ${currentDir.absolutePath}",
                            fontSize = 11.sp,
                            color = if (isSharedStorage) NanasTextSecondary else NanasError,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Deteksi Lokasi: Peringatan jika masih di filesDir internal
                    if (!isSharedStorage) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF261C05))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Proyek Berada di Internal Storage",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Termux tidak dapat mengakses folder internal aplikasi (filesDir) karena kebijakan keamanan sandbox Android. Untuk menjalankan kode di Termux, salin atau pindahkan proyek ke Folder Bersama (/storage/emulated/0/NasCode/projects).",
                                fontSize = 12.sp,
                                color = Color(0xFFE4E4E7),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (!hasStoragePermission) {
                                Button(
                                    onClick = {
                                        TermuxManager.requestAllFilesPermission(context)
                                        hasStoragePermission = TermuxManager.hasAllFilesPermission(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Beri Izin Akses Penyimpanan (All Files)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isRelocating = true
                                        val target = File(TermuxManager.getSharedProjectsDirectory(), currentDir.name)
                                        val res = TermuxManager.copyOrMoveToSharedStorage(currentDir, target, deleteSourceAfterCopy = false)
                                        isRelocating = false
                                        if (res.isSuccess) {
                                            currentDir = target
                                            onProjectRelocated(target)
                                            actionMessage = "Proyek berhasil disalin ke: ${target.absolutePath}"
                                        } else {
                                            actionMessage = "Gagal menyalin: ${res.exceptionOrNull()?.message}"
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    enabled = !isRelocating
                                ) {
                                    Text("Salin ke Bersama", fontSize = 12.sp, color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        isRelocating = true
                                        val target = File(TermuxManager.getSharedProjectsDirectory(), currentDir.name)
                                        val res = TermuxManager.copyOrMoveToSharedStorage(currentDir, target, deleteSourceAfterCopy = true)
                                        isRelocating = false
                                        if (res.isSuccess) {
                                            currentDir = target
                                            onProjectRelocated(target)
                                            actionMessage = "Proyek berhasil dipindahkan ke: ${target.absolutePath}"
                                        } else {
                                            actionMessage = "Gagal memindahkan: ${res.exceptionOrNull()?.message}"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    enabled = !isRelocating
                                ) {
                                    Text("Pindahkan", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 3. Status Termux
                    if (!isTermuxInstalled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF281111))
                                .border(1.dp, NanasError.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = NanasError)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Aplikasi Termux Belum Terpasang",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Termux diperlukan untuk mengeksekusi runtime Node.js, Python, atau Vite. Gunakan Termux versi F-Droid atau GitHub (jangan gunakan versi Google Play Store karena sudah usang/tidak didukung).",
                                fontSize = 12.sp,
                                color = Color(0xFFE4E4E7),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TermuxManager.FDROID_TERMUX_URL)))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Unduh F-Droid", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TermuxManager.GITHUB_TERMUX_URL)))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Unduh GitHub", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        // Tombol Eksekusi Termux
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NanasDarkSurface)
                                .border(1.dp, NanasDarkBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "AKSI EKSEKUSI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NanasTextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val res = TermuxManager.executeInTermux(context, currentDir, customCommand)
                                    executionStatus = res
                                },
                                enabled = isSharedStorage,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Jalankan di Termux",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    TermuxManager.openTermux(context)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Buka Aplikasi Termux",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // 4. Hasil Eksekusi / Panduan Error
                    if (executionStatus != null || actionMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        val status = executionStatus
                        if (status is TermuxExecutionResult.Success) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F291E))
                                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Perintah berhasil dikirim ke Termux! Buka aplikasi Termux untuk melihat jalannya proses secara interaktif.",
                                    color = Color(0xFFA7F3D0),
                                    fontSize = 13.sp
                                )
                            }
                        } else if (status is TermuxExecutionResult.Error) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2E1010))
                                    .border(1.dp, NanasError, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = status.message,
                                    color = Color(0xFFFECACA),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (status.isSetupRequired) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Langkah konfigurasi Termux:\n1. Buka Termux\n2. Jalankan: termux-setup-storage\n3. Jalankan: mkdir -p ~/.termux && echo \"allow-external-apps=true\" >> ~/.termux/termux.properties && termux-reload-settings",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFFE4E4E7)
                                    )
                                }
                            }
                        }

                        actionMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = msg, color = NanasTextSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Card Buka Localhost Preview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NanasDarkSurface)
                            .border(1.dp, NanasDarkBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preview Localhost Dev Server",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Jika Anda sudah menjalankan dev server (misal Vite atau Express) di Termux, buka preview pada alamat $localhostUrl.",
                            fontSize = 12.sp,
                            color = NanasTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onOpenLocalhostPreview(localhostUrl)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Buka Preview ($localhostUrl)", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. Panduan Setup Termux (Collapsible / Static)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NanasDarkSurfaceLight)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = NanasTextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Catatan Integrasi & Milestone 4",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NanasTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Terminal bawaan offline (proot + Alpine Linux + Node.js) sedang dikembangkan untuk Milestone 4 agar eksekusi kode dapat berjalan mandiri di dalam aplikasi tanpa bantuan Termux. Saat ini, eksekusi kode dilakukan secara nyata melalui Termux.",
                            fontSize = 11.sp,
                            color = NanasTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
