package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowescape.puzzle.ui.theme.ThemePreset
import com.arrowescape.puzzle.ui.theme.ThemePresets

@Composable
fun ThemeCustomizationDialog(
    activeTheme: ThemePreset,
    onThemeSelect: (ThemePreset) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val dialogWidth = minOf(screenWidth - 32.dp, 440.dp)
    val maxDialogHeight = (screenHeight * 0.88f).coerceAtMost(620.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (activeTheme.isDark) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(
                width = 1.dp,
                color = if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            ),
            shadowElevation = 16.dp,
            modifier = Modifier
                .width(dialogWidth)
                .heightIn(max = maxDialogHeight)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // DIALOG HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Color Themes",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTheme.isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Choose your board & arrow styling",
                            fontSize = 12.sp,
                            color = if (activeTheme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (activeTheme.isDark) Color(0xFFF8FAFC) else Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // THEME SWATCHES LIST
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(ThemePresets.all) { preset ->
                        val isSelected = preset.name == activeTheme.name

                        ThemeSwatchCard(
                            preset = preset,
                            activeTheme = activeTheme,
                            isSelected = isSelected,
                            onClick = {
                                onThemeSelect(preset)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatchCard(
    preset: ThemePreset,
    activeTheme: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        Color(0xFF3B82F6)
    } else {
        if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    }

    val bgColor = if (isSelected) {
        if (activeTheme.isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF)
    } else {
        if (activeTheme.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    }

    val titleColor = if (activeTheme.isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subtitleColor = if (activeTheme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // DUAL-COLOR SWATCH PREVIEW CIRCLE (Left half: background, Right half: arrow)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(
                        1.5.dp,
                        if (activeTheme.isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                        CircleShape
                    )
            ) {
                Canvas(modifier = Modifier.size(42.dp)) {
                    // Left half: Background color
                    drawArc(
                        color = preset.background,
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    // Right half: Arrow color
                    drawArc(
                        color = preset.arrowDefault,
                        startAngle = 270f,
                        sweepAngle = 180f,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    // Center separator line
                    drawLine(
                        color = if (activeTheme.isDark) Color(0x66FFFFFF) else Color(0x33000000),
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 1.2f
                    )
                }
            }

            // THEME NAME & DESCRIPTION
            Column {
                Text(
                    text = preset.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Text(
                    text = getThemeSubtitle(preset.name),
                    fontSize = 12.sp,
                    color = subtitleColor
                )
            }
        }

        // SELECTION BADGE
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getThemeSubtitle(themeName: String): String {
    return when (themeName) {
        "Clean Light" -> "Crisp White & Dark Slate"
        "Warm Sepia" -> "Vintage Cream & Dark Wood"
        "Midnight Dark" -> "Obsidian & Sky Blue"
        "Calm Pastel" -> "Soft Lavender & Purple"
        "Mint Fresh" -> "Sage & Emerald"
        "Sunset Rose" -> "Peach & Rose Crimson"
        else -> "Custom board palette"
    }
}
