package com.arrowescape.puzzle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowescape.puzzle.ui.theme.ThemePreset

@Composable
fun GameOverDialog(
    theme: ThemePreset,
    hasGlobalLives: Boolean = false,
    onClaimLives: () -> Unit,
    onRestartClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val dialogWidth = minOf(screenWidth - 36.dp, 360.dp)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = if (theme.isDark) Color(0xFF1E293B) else Color.White),
            border = BorderStroke(
                width = 1.dp,
                color = if (theme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            ),
            elevation = CardDefaults.cardElevation(18.dp),
            modifier = Modifier
                .width(dialogWidth)
                .padding(vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 26.dp)
            ) {
                // Heart Broken Icon with ambient soft glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.HeartBroken,
                        contentDescription = "Out of Lives",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "OUT OF LIVES",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.textPrimary
                )

                Text(
                    text = "Get extra lives to keep solving or restart the level.",
                    fontSize = 13.sp,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 6.dp, end = 6.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Action: Claim 3 Lives or Watch Ad
                AppPrimaryButton(
                    text = "Watch Ad for +3 Lives",
                    onClick = onClaimLives,
                    theme = theme,
                    containerColor = Color(0xFFEF4444),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action: Restart Level
                AppSecondaryButton(
                    text = "Restart Level",
                    onClick = onRestartClick,
                    theme = theme,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun activeThemePrimary(theme: ThemePreset): Color {
    return if (theme.isDark) Color(0xFF38BDF8) else theme.arrowDefault
}
