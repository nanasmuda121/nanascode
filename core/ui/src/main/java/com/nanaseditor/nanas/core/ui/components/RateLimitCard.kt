package com.nanaseditor.nanas.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanaseditor.nanas.core.model.RateLimitInfo
import com.nanaseditor.nanas.core.ui.theme.NanasError
import com.nanaseditor.nanas.core.ui.theme.NanasWarning

@Composable
fun RateLimitCard(
    rateLimit: RateLimitInfo,
    modifier: Modifier = Modifier
) {
    // Tampilkan jika kuota tersisa rendah atau habis
    if (!rateLimit.isLow && !rateLimit.isExhausted) return

    val bgColor = if (rateLimit.isExhausted) Color(0xFF331414) else Color(0xFF2C2411)
    val borderColor = if (rateLimit.isExhausted) NanasError else NanasWarning
    val icon = if (rateLimit.isExhausted) Icons.Default.Warning else Icons.Default.Info

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Rate limit",
            tint = borderColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = if (rateLimit.isExhausted) {
                "Rate limit habis! Reset pada ${rateLimit.formattedResetTime()} (${rateLimit.minutesUntilReset()} mnt lagi)."
            } else {
                "Sisa kuota API: ${rateLimit.remaining}/${rateLimit.limit}. Reset ${rateLimit.formattedResetTime()}."
            },
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
