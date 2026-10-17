package com.nanascode.app.core.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nanascode.app.core.ui.R
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import java.io.File

/**
 * Komponen Logo Resmi NasCode.
 * Mengutamakan pemuatan dari file lokal `/storage/emulated/0/Download/nascode.png` atau `/sdcard/Download/nascode.png`,
 * dengan fallback ke drawable resmi yang dibundel di dalam aplikasi.
 */
@Composable
fun NasCodeLogo(
    modifier: Modifier = Modifier.size(48.dp),
    contentDescription: String = "NasCode Logo"
) {
    val localBitmap = remember {
        val candidates = listOf(
            File("/storage/emulated/0/Download/nascode.png"),
            File("/sdcard/Download/nascode.png")
        )
        candidates.firstOrNull { it.exists() && it.canRead() }?.let { file ->
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (_: Throwable) {
                null
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (localBitmap != null) {
            Image(
                bitmap = localBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier
            )
        } else {
            try {
                Image(
                    painter = painterResource(id = R.drawable.nascode_logo),
                    contentDescription = contentDescription,
                    modifier = modifier
                )
            } catch (_: Throwable) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = contentDescription,
                    tint = NanasTextPrimary,
                    modifier = modifier
                )
            }
        }
    }
}
