path = 'arrow_native/app/src/main/java/com/arrowescape/puzzle/ui/components/GameHeader.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

# Replace padding in Row
old_row = """            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {"""

new_row = """            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {"""

# Replace Left Pod
old_left = """                // LEFT: Clean, minimal HUD pods (Pause || + Dual-Action Sound 🔊/🔇)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Pause button inside rounded tactile pod
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = theme.boosterBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = effectivePause)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause Game",
                                tint = theme.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 2. Dual-action Sound button: Quick tap toggles audio, Hold for settings
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = theme.boosterBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = effectiveSoundToggle,
                                onLongClick = effectiveSoundLongPress
                            )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isSoundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                                contentDescription = if (isSoundOn) "Mute Sound (Hold for Settings)" else "Unmute Sound (Hold for Settings)",
                                tint = if (isSoundOn) theme.textPrimary else theme.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Optional Dev Theme button (shown only in test mode)
                    if (isTestMode && onThemeClick != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = theme.boosterBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onThemeClick)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Themes",
                                    tint = theme.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }"""

new_left = """                // LEFT: Clean, minimal HUD pods (Pause || + Dual-Action Sound 🔊/🔇)
                val podSize = if (isTestMode) 36.dp else 40.dp
                val iconSize = if (isTestMode) 18.dp else 20.dp
                val podCorner = if (isTestMode) 12.dp else 14.dp

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isTestMode) 6.dp else 8.dp)
                ) {
                    // 1. Pause button inside rounded tactile pod
                    Surface(
                        shape = RoundedCornerShape(podCorner),
                        color = theme.boosterBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(podSize)
                            .clip(RoundedCornerShape(podCorner))
                            .clickable(onClick = effectivePause)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause Game",
                                tint = theme.textPrimary,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }

                    // 2. Dual-action Sound button: Quick tap toggles audio, Hold for settings
                    Surface(
                        shape = RoundedCornerShape(podCorner),
                        color = theme.boosterBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(podSize)
                            .clip(RoundedCornerShape(podCorner))
                            .combinedClickable(
                                onClick = effectiveSoundToggle,
                                onLongClick = effectiveSoundLongPress
                            )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isSoundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                                contentDescription = if (isSoundOn) "Mute Sound (Hold for Settings)" else "Unmute Sound (Hold for Settings)",
                                tint = if (isSoundOn) theme.textPrimary else theme.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }

                    // Optional Dev Theme button (shown only in test mode)
                    if (isTestMode && onThemeClick != null) {
                        Surface(
                            shape = RoundedCornerShape(podCorner),
                            color = theme.boosterBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(podSize)
                                .clip(RoundedCornerShape(podCorner))
                                .clickable(onClick = onThemeClick)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Themes",
                                    tint = theme.textPrimary,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                        }
                    }
                }"""

# Replace Center level text
old_center_level = """                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = theme.boosterBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                                modifier = Modifier.clickable(enabled = onLevelNumberClick != null) {
                                    onLevelNumberClick?.invoke()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Level $levelNumber",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimary
                                    )
                                    if (onLevelNumberClick != null) {
                                        Text(
                                            text = "▾",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.textSecondary
                                        )
                                    }
                                }
                            }"""

new_center_level = """                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = theme.boosterBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                                modifier = Modifier.clickable(enabled = onLevelNumberClick != null) {
                                    onLevelNumberClick?.invoke()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Level $levelNumber",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    if (onLevelNumberClick != null) {
                                        Text(
                                            text = "▾",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.textSecondary
                                        )
                                    }
                                }
                            }"""

# Replace Right badge
old_badge = """                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = theme.boosterBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .height(40.dp)
                        .scale(badgeScale.value)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        // Bold solid directional arrow icon matching Screenshot 1
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(15.dp)) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * 0.95f, size.height * 0.05f)
                                lineTo(size.width * 0.95f, size.height * 0.65f)
                                lineTo(size.width * 0.72f, size.height * 0.42f)
                                lineTo(size.width * 0.22f, size.height * 0.92f)
                                lineTo(size.width * 0.08f, size.height * 0.78f)
                                lineTo(size.width * 0.58f, size.height * 0.28f)
                                lineTo(size.width * 0.35f, size.height * 0.05f)
                                close()
                            }
                            drawPath(path = path, color = theme.textPrimary)
                        }

                        Text(
                            text = "$remainingArrowsCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary
                        )
                    }
                }"""

new_badge = """                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = theme.boosterBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .height(38.dp)
                        .scale(badgeScale.value)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        // Bold solid directional arrow icon matching Screenshot 1
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * 0.95f, size.height * 0.05f)
                                lineTo(size.width * 0.95f, size.height * 0.65f)
                                lineTo(size.width * 0.72f, size.height * 0.42f)
                                lineTo(size.width * 0.22f, size.height * 0.92f)
                                lineTo(size.width * 0.08f, size.height * 0.78f)
                                lineTo(size.width * 0.58f, size.height * 0.28f)
                                lineTo(size.width * 0.35f, size.height * 0.05f)
                                close()
                            }
                            drawPath(path = path, color = theme.textPrimary)
                        }

                        Text(
                            text = "$remainingArrowsCount",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }"""

c_norm = c.replace('\r\n', '\n')
for old, new, desc in [
    (old_row, new_row, "Row padding"),
    (old_left, new_left, "Left pod"),
    (old_center_level, new_center_level, "Center level"),
    (old_badge, new_badge, "Badge")
]:
    old_norm = old.replace('\r\n', '\n')
    new_norm = new.replace('\r\n', '\n')
    if old_norm in c_norm:
        c_norm = c_norm.replace(old_norm, new_norm)
        print(f"Applied: {desc}")
    else:
        print(f"FAILED to find: {desc}")

with open(path, 'w', encoding='utf-8', newline='\n') as f:
    f.write(c_norm)
print("Finished updating GameHeader.kt")
