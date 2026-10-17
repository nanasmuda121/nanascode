package com.nanascode.app.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanascode.app.core.ui.theme.NanasDarkBorder
import com.nanascode.app.core.ui.theme.NanasDarkSurface
import com.nanascode.app.core.ui.theme.NanasDarkSurfaceLight
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import com.nanascode.app.core.ui.theme.NanasTextSecondary
import com.nanascode.app.core.ui.theme.NanasYellow

data class EditorConfig(
    val fontSize: Int = 14,
    val tabSize: Int = 2,
    val wordWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val autoSave: Boolean = false,
    val theme: String = "monochrome"
)

@Composable
fun EditorSettingsDialog(
    initialConfig: EditorConfig,
    onApplyConfig: (EditorConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var fontSize by remember { mutableFloatStateOf(initialConfig.fontSize.toFloat()) }
    var tabSize by remember { mutableIntStateOf(initialConfig.tabSize) }
    var wordWrap by remember { mutableStateOf(initialConfig.wordWrap) }
    var showLineNumbers by remember { mutableStateOf(initialConfig.showLineNumbers) }
    var autoSave by remember { mutableStateOf(initialConfig.autoSave) }
    var selectedTheme by remember { mutableStateOf(initialConfig.theme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pengaturan Editor (Acode Style)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Font Size
                Text(
                    text = "Ukuran Huruf: ${fontSize.toInt()} px",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 10f..24f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = NanasDarkSurfaceLight
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = NanasDarkBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Tab Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ukuran Indentasi Tab", color = Color.White, fontSize = 13.sp)
                    Row {
                        TabOptionBadge(
                            label = "2 Spasi",
                            isSelected = tabSize == 2,
                            onClick = { tabSize = 2 }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TabOptionBadge(
                            label = "4 Spasi",
                            isSelected = tabSize == 4,
                            onClick = { tabSize = 4 }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Word Wrap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Bungkus Baris (Word Wrap)", color = Color.White, fontSize = 13.sp)
                        Text("Bungkus teks panjang ke baris berikutnya", color = NanasTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = wordWrap,
                        onCheckedChange = { wordWrap = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Line Numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Nomor Baris (Gutter)", color = Color.White, fontSize = 13.sp)
                        Text("Tampilkan nomor baris di sisi kiri editor", color = NanasTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = showLineNumbers,
                        onCheckedChange = { showLineNumbers = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Simpan Otomatis", color = Color.White, fontSize = 13.sp)
                        Text("Simpan berkas otomatis saat berpindah tab", color = NanasTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoSave,
                        onCheckedChange = { autoSave = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyConfig(
                        EditorConfig(
                            fontSize = fontSize.toInt(),
                            tabSize = tabSize,
                            wordWrap = wordWrap,
                            showLineNumbers = showLineNumbers,
                            autoSave = autoSave,
                            theme = selectedTheme
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Batal", color = NanasTextPrimary)
            }
        },
        containerColor = NanasDarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun TabOptionBadge(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) Color.White else NanasDarkSurfaceLight,
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (isSelected) Color.White else NanasDarkBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.Black else Color.White
        )
    }
}
