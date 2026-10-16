package com.nanaseditor.nanas.core.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanaseditor.nanas.core.model.GitHubRepo
import com.nanaseditor.nanas.core.ui.theme.NanasDarkBorder
import com.nanaseditor.nanas.core.ui.theme.NanasDarkSurface
import com.nanaseditor.nanas.core.ui.theme.NanasDarkSurfaceLight
import com.nanaseditor.nanas.core.ui.theme.NanasGreen
import com.nanaseditor.nanas.core.ui.theme.NanasTextPrimary
import com.nanaseditor.nanas.core.ui.theme.NanasTextSecondary
import com.nanaseditor.nanas.core.ui.theme.NanasYellow

@Composable
fun RepoItemCard(
    repo: GitHubRepo,
    onClick: () -> Unit,
    onActionsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NanasDarkSurface)
            .border(1.dp, NanasDarkBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        // Baris Header: Nama Repo & Badge Visibilitas
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = NanasYellow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (repo.isFork) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = "Fork",
                        tint = NanasTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Badge Private / Public
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (repo.isPrivate) Color(0xFF382314) else Color(0xFF1B381E))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (repo.isPrivate) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Private",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (repo.isPrivate) "Private" else "Public",
                        fontSize = 11.sp,
                        color = if (repo.isPrivate) Color(0xFFFFB74D) else Color(0xFF81C784),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Deskripsi Repo
        if (!repo.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = repo.description,
                style = MaterialTheme.typography.bodySmall,
                color = NanasTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Baris Footer: Bahasa, Stars, Forks, & Tombol Actions (F6)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bahasa utama
                if (!repo.language.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(getLanguageColor(repo.language))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = repo.language,
                            fontSize = 12.sp,
                            color = NanasTextPrimary
                        )
                    }
                }

                // Stars
                if (repo.stargazersCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Stars",
                            tint = NanasYellow,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${repo.stargazersCount}",
                            fontSize = 12.sp,
                            color = NanasTextSecondary
                        )
                    }
                }

                // Default branch
                Text(
                    text = repo.defaultBranch,
                    fontSize = 11.sp,
                    color = NanasTextSecondary.copy(alpha = 0.8f)
                )
            }

            // F6: Tombol Tab Actions (Hanya Repo Publik)
            if (onActionsClick != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (repo.isPrivate) Color(0xFF282828) else NanasGreen.copy(alpha = 0.2f))
                        .clickable(enabled = !repo.isPrivate, onClick = onActionsClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (repo.isPrivate) "Actions (Publik Saja)" else "⚡ Actions",
                        fontSize = 11.sp,
                        color = if (repo.isPrivate) NanasTextSecondary.copy(alpha = 0.6f) else NanasGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun getLanguageColor(language: String): Color {
    return when (language.lowercase()) {
        "kotlin" -> Color(0xFFA97BFF)
        "javascript" -> Color(0xFFF1E05A)
        "typescript" -> Color(0xFF3178C6)
        "python" -> Color(0xFF3572A5)
        "html" -> Color(0xFFE34C26)
        "css" -> Color(0xFF563D7C)
        "java" -> Color(0xFFB07219)
        "rust" -> Color(0xFFDEA584)
        "go" -> Color(0xFF00ADD8)
        else -> Color(0xFF8B949E)
    }
}
