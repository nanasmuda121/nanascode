package com.nanascode.app.feature.workspace

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.nanascode.app.core.model.ProjectTemplate
import com.nanascode.app.core.ui.theme.NanasDarkBorder
import com.nanascode.app.core.ui.theme.NanasDarkSurface
import com.nanascode.app.core.ui.theme.NanasDarkSurfaceLight
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import com.nanascode.app.core.ui.theme.NanasTextSecondary
import com.nanascode.app.core.ui.theme.NanasYellow

@Composable
fun CreateWorkspaceDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, templateId: String?) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var selectedTemplateId by remember { mutableStateOf("empty") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Buat Proyek Baru",
                fontWeight = FontWeight.Bold,
                color = NanasTextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Nama Proyek") },
                    placeholder = { Text("misal: aplikasi-katalog") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NanasYellow,
                        unfocusedBorderColor = NanasDarkBorder,
                        focusedLabelColor = NanasYellow,
                        cursorColor = NanasYellow,
                        focusedTextColor = NanasTextPrimary,
                        unfocusedTextColor = NanasTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pilih Template:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NanasTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ProjectTemplate.ALL_TEMPLATES) { template ->
                        val isSelected = template.id == selectedTemplateId
                        val icon = when (template.iconName) {
                            "brush" -> Icons.Default.Brush
                            "code" -> Icons.Default.Code
                            "web" -> Icons.Default.Web
                            "terminal" -> Icons.Default.Terminal
                            else -> Icons.Default.Folder
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) NanasDarkSurfaceLight else NanasDarkSurface,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NanasYellow else NanasDarkBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTemplateId = template.id }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) NanasYellow else NanasTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = template.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) NanasYellow else NanasTextPrimary
                                )
                                Text(
                                    text = template.description,
                                    fontSize = 11.sp,
                                    color = NanasTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectName.isNotBlank()) {
                        onCreate(projectName.trim(), selectedTemplateId)
                    }
                },
                enabled = projectName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NanasYellow,
                    contentColor = Color.Black
                )
            ) {
                Text("Buat Proyek")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = NanasTextSecondary)
            }
        },
        containerColor = NanasDarkSurface
    )
}
