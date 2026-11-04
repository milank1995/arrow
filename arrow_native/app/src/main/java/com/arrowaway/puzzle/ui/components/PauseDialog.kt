package com.arrowaway.puzzle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowaway.puzzle.ui.theme.ThemePreset
import com.arrowaway.puzzle.ui.theme.ThemePresets

/**
 * Dedicated Pause Menu popup.
 * Focused purely on game flow actions: Resume, Restart, Home, and in-game theme switching.
 * Contains zero sound/haptics toggles for a clean, distraction-free experience.
 */
@Composable
fun PauseDialog(
    activeTheme: ThemePreset,
    onThemeSelect: ((ThemePreset) -> Unit)? = null,
    onRestartClick: () -> Unit,
    onHomeClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val dialogWidth = minOf(screenWidth - 36.dp, 360.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (activeTheme.isDark) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(
                width = 1.dp,
                color = if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            ),
            shadowElevation = 18.dp,
            modifier = Modifier
                .width(dialogWidth)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // 1. HEADER: Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Game Paused",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTheme.textPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = activeTheme.textPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. PRIMARY ACTION: RESUME GAME
                AppPrimaryButton(
                    text = "Resume Game",
                    onClick = onDismiss,
                    theme = activeTheme,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. SECONDARY ROW: RESTART & HOME
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppSecondaryButton(
                        text = "Restart",
                        onClick = {
                            onDismiss()
                            onRestartClick()
                        },
                        theme = activeTheme,
                        modifier = Modifier.weight(1f)
                    )

                    AppSecondaryButton(
                        text = "Home",
                        onClick = {
                            onDismiss()
                            onHomeClick()
                        },
                        theme = activeTheme,
                        modifier = Modifier.weight(1f)
                    )
                }


            }
        }
    }
}
