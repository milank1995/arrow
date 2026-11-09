package com.arrowaway.puzzle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowaway.puzzle.ui.theme.ThemePreset

@Composable
fun NotificationPrePromptDialog(
    theme: ThemePreset,
    isStreakPrompt: Boolean = false,
    streakDays: Int = 3,
    onTurnOnClick: () -> Unit,
    onNotNowClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onNotNowClick,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.surface,
            border = BorderStroke(1.2.dp, theme.gridLine),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bell Icon inside circular background
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(theme.arrowHint.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notification Prompt",
                        tint = theme.arrowHint,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isStreakPrompt) "Protect Your Streak! \uD83D\uDD25" else "Daily Challenge Reminders",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isStreakPrompt) {
                        "You've built a $streakDays-day streak! Get a reminder each evening so you never lose your progress."
                    } else {
                        "Get a friendly reminder when each new Daily Challenge is ready at 7:30 PM."
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Turn On Button
                AppPrimaryButton(
                    text = "Turn On",
                    onClick = onTurnOnClick,
                    theme = theme,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Not Now Button
                TextButton(
                    onClick = onNotNowClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "Not Now",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.textSecondary
                    )
                }
            }
        }
    }
}
