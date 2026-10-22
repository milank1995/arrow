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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    onClaimFreeLives: () -> Unit,
    onRestartClick: () -> Unit,
    onHomeClick: () -> Unit
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
                    .padding(horizontal = 22.dp, vertical = 24.dp)
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

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "OUT OF HEARTS!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.textPrimary
                )

                Text(
                    text = "Don't give up! Claim free hearts to continue your puzzle right here, or restart the level.",
                    fontSize = 13.sp,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action: CLAIM 3 FREE LIVES
                Button(
                    onClick = onClaimFreeLives,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE11D48) // Warm vibrant rose/coral matching hearts
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CLAIM 3 FREE LIVES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                // Passive Regen Countdown Pill
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = if (theme.isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "⏱ Passive refill in 15:00",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary Action: RESTART LEVEL
                OutlinedButton(
                    onClick = onRestartClick,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (theme.isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = theme.textPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RESTART LEVEL",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = theme.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tertiary Action: Back to Home
                TextButton(
                    onClick = onHomeClick,
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = "Back to Menu",
                        fontSize = 13.sp,
                        color = theme.textSecondary
                    )
                }
            }
        }
    }
}
