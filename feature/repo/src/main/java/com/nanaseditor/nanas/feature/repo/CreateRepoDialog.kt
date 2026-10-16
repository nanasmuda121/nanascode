package com.nanaseditor.nanas.feature.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanaseditor.nanas.core.ui.theme.NanasDarkBorder
import com.nanaseditor.nanas.core.ui.theme.NanasDarkSurface
import com.nanaseditor.nanas.core.ui.theme.NanasDarkSurfaceLight
import com.nanaseditor.nanas.core.ui.theme.NanasTextPrimary
import com.nanaseditor.nanas.core.ui.theme.NanasTextSecondary
import com.nanaseditor.nanas.core.ui.theme.NanasYellow

@Composable
fun CreateRepoDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String?, isPrivate: Boolean, autoInit: Boolean, gitignore: String?, license: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var autoInit by remember { mutableStateOf(true) }
    var selectedGitignore by remember { mutableStateOf("Node") }
    var selectedLicense by remember { mutableStateOf("MIT") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Buat Repositori Baru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NanasYellow
                )
                IconButton(onClick = onDismiss, enabled = !isLoading) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = NanasTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Input Nama
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replace(" ", "-") },
                    label = { Text("Nama Repositori *") },
                    placeholder = { Text("contoh-aplikasi-web") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NanasYellow,
                        unfocusedBorderColor = NanasDarkBorder,
                        focusedContainerColor = NanasDarkSurfaceLight,
                        unfocusedContainerColor = NanasDarkSurfaceLight,
                        focusedTextColor = NanasTextPrimary,
                        unfocusedTextColor = NanasTextPrimary
                    )
                )

                // Input Deskripsi
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NanasYellow,
                        unfocusedBorderColor = NanasDarkBorder,
                        focusedContainerColor = NanasDarkSurfaceLight,
                        unfocusedContainerColor = NanasDarkSurfaceLight,
                        focusedTextColor = NanasTextPrimary,
                        unfocusedTextColor = NanasTextPrimary
                    )
                )

                // Pilihan Visibilitas (Publik / Private)
                Text(
                    text = "Visibilitas",
                    style = MaterialTheme.typography.labelMedium,
                    color = NanasTextSecondary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isPrivate) NanasDarkSurfaceLight else Color.Transparent)
                        .clickable { isPrivate = false }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !isPrivate,
                        onClick = { isPrivate = false },
                        colors = RadioButtonDefaults.colors(selectedColor = NanasYellow)
                    )
                    Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Publik", color = NanasTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Dapat dilihat oleh siapa saja (fitur Actions aktif).", color = NanasTextSecondary, fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPrivate) NanasDarkSurfaceLight else Color.Transparent)
                        .clickable { isPrivate = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isPrivate,
                        onClick = { isPrivate = true },
                        colors = RadioButtonDefaults.colors(selectedColor = NanasYellow)
                    )
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Private", color = NanasTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Hanya Anda yang dapat mengakses kode repositori.", color = NanasTextSecondary, fontSize = 11.sp)
                    }
                }

                // Opsi Inisialisasi README
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { autoInit = !autoInit }
                ) {
                    Checkbox(
                        checked = autoInit,
                        onCheckedChange = { autoInit = it },
                        colors = CheckboxDefaults.colors(checkedColor = NanasYellow, checkmarkColor = Color.Black)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Inisialisasi dengan berkas README.md",
                        color = NanasTextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(name, description, isPrivate, autoInit, selectedGitignore, selectedLicense)
                },
                enabled = name.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NanasYellow,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Buat Repositori", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Batal", color = NanasTextSecondary)
            }
        },
        containerColor = NanasDarkSurface,
        shape = RoundedCornerShape(12.dp)
    )
}
