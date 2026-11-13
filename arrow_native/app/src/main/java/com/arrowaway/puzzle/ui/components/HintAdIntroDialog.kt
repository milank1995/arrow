package com.arrowaway.puzzle.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.arrowaway.puzzle.ui.theme.ThemePreset

/**
 * Shown the first time a player taps Hint with no hints left, so they knowingly opt in to the
 * rewarded ad. After this the Hint button (with its AD badge) starts the ad in one tap.
 */
@Composable
fun HintAdIntroDialog(
    theme: ThemePreset,
    isDailyChallenge: Boolean = false,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val dialogWidth = minOf(screenWidth - 36.dp, 360.dp)
    val amber = Color(0xFFD97706)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = if (theme.isDark) Color(0xFF1E293B) else Color.White),
            border = BorderStroke(1.dp, if (theme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = amber,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isDailyChallenge) "DAILY CHALLENGE HINT" else "OUT OF HINTS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isDailyChallenge) {
                        "Watch a short ad to get 1 hint.\nYour saved hints stay safe for normal levels."
                    } else {
                        "Watch a short ad to get 1 free hint.\nNext time, just tap the Hint button."
                    },
                    fontSize = 13.sp,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(22.dp))

                AppPrimaryButton(
                    text = "Watch Ad (+1 Hint)",
                    onClick = onWatchAd,
                    theme = theme,
                    containerColor = amber,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                AppSecondaryButton(
                    text = "Not Now",
                    onClick = onDismiss,
                    theme = theme,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
