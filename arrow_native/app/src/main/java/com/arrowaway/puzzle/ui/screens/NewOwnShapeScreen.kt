package com.arrowaway.puzzle.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowaway.puzzle.core.engine.LevelLoader
import com.arrowaway.puzzle.ui.theme.ThemePreset

/**
 * Dedicated development & testing screen for custom shape levels
 * placed inside the assets/New Own Shape folder.
 * Gated exclusively for development; not exposed in production builds.
 */
@Composable
fun NewOwnShapeScreen(
    theme: ThemePreset,
    onBackClick: () -> Unit,
    onLevelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler(onBack = onBackClick)

    val customLevels = remember {
        LevelLoader.listCustomShapeLevels(context)
    }

    Surface(
        color = theme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
        ) {
            // TOP APP BAR: Circular Back Button + Title + Spacer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = theme.surface,
                    border = BorderStroke(1.2.dp, theme.gridLine),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "New Own Shape",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "Custom Dev Levels (${customLevels.size})",
                        fontSize = 11.5.sp,
                        color = theme.textSecondary
                    )
                }

                // Balance Spacer
                Spacer(modifier = Modifier.size(42.dp))
            }

            if (customLevels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🎨", fontSize = 48.sp)
                        Text(
                            text = "No Levels Found",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary
                        )
                        Text(
                            text = "Place your handcrafted JSON levels into assets/New Own Shape/ to view them here.",
                            fontSize = 13.sp,
                            color = theme.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(customLevels, key = { it.fileName }) { levelItem ->
                        CustomShapeCard(
                            item = levelItem,
                            theme = theme,
                            onClick = { onLevelSelected(levelItem.fileName) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomShapeCard(
    item: LevelLoader.CustomShapeItem,
    theme: ThemePreset,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = theme.surface,
        border = BorderStroke(1.2.dp, theme.gridLine),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Icon pod
            val iconEmoji = when {
                item.displayName.contains("Glass", ignoreCase = true) -> "👓"
                item.displayName.contains("Car", ignoreCase = true) -> "🚗"
                item.displayName.contains("Ghost", ignoreCase = true) -> "👻"
                item.displayName.contains("Pot", ignoreCase = true) -> "🏺"
                item.displayName.contains("Telephone", ignoreCase = true) || item.displayName.contains("Phone", ignoreCase = true) -> "☎️"
                item.displayName.contains("Android", ignoreCase = true) || item.displayName.contains("Robot", ignoreCase = true) -> "🤖"
                item.displayName.contains("Headphone", ignoreCase = true) -> "🎧"
                item.displayName.contains("Recycle", ignoreCase = true) -> "♻️"
                item.displayName.contains("Rocket", ignoreCase = true) -> "🚀"
                item.displayName.contains("Teddy", ignoreCase = true) || item.displayName.contains("Bear", ignoreCase = true) -> "🧸"
                item.displayName.contains("Frog", ignoreCase = true) -> "🐸"
                item.displayName.contains("Pig", ignoreCase = true) -> "🐷"
                item.displayName.contains("Puppy", ignoreCase = true) || item.displayName.contains("Dog", ignoreCase = true) -> "🐶"
                item.displayName.contains("Mask", ignoreCase = true) -> "😷"
                item.displayName.contains("Woman", ignoreCase = true) -> "👩"
                item.displayName.contains("Cartoon", ignoreCase = true) -> "🎭"
                item.displayName.contains("Duck", ignoreCase = true) -> "🦆"
                item.displayName.contains("Snake", ignoreCase = true) -> "🐍"
                item.displayName.contains("Sun", ignoreCase = true) && item.displayName.contains("Flower", ignoreCase = true) -> "🌻"
                item.displayName.contains("Flower", ignoreCase = true) -> "🌸"
                item.displayName.contains("Drum", ignoreCase = true) -> "🥁"
                item.displayName.contains("Om", ignoreCase = true) -> "🕉️"
                item.displayName.contains("Scooter", ignoreCase = true) -> "🛵"
                item.displayName.contains("Train", ignoreCase = true) -> "🚂"
                item.displayName.contains("Cat", ignoreCase = true) -> "🐱"
                item.displayName.contains("Bird", ignoreCase = true) -> "🐤"
                item.displayName.contains("Turtle", ignoreCase = true) -> "🐢"
                item.displayName.contains("Warrior", ignoreCase = true) -> "🥷"
                item.displayName.contains("Paint", ignoreCase = true) -> "🖌️"
                item.displayName.contains("Planet", ignoreCase = true) -> "🪐"
                item.displayName.contains("Screw", ignoreCase = true) -> "🪛"
                item.displayName.contains("Skull", ignoreCase = true) -> "💀"
                item.displayName.contains("2026") -> "🎆"
                item.displayName.contains("Basket", ignoreCase = true) -> "🧺"
                item.displayName.contains("Chair", ignoreCase = true) -> "🪑"
                item.displayName.contains("Guitar", ignoreCase = true) -> "🎸"
                item.displayName.contains("Sound", ignoreCase = true) -> "🔊"
                item.displayName.contains("Animal", ignoreCase = true) -> "🦁"
                item.displayName.contains("Deer", ignoreCase = true) -> "🦌"
                item.displayName.contains("JCB", ignoreCase = true) -> "🚜"
                item.displayName.contains("Piano", ignoreCase = true) -> "🎹"
                item.displayName.contains("Trophy", ignoreCase = true) -> "🏆"
                item.displayName.contains("Dragon", ignoreCase = true) -> "🐉"
                item.displayName.contains("House", ignoreCase = true) -> "🏠"
                item.displayName.contains("Ice", ignoreCase = true) || item.displayName.contains("Cream", ignoreCase = true) -> "🍦"
                item.displayName.contains("Carousel", ignoreCase = true) || item.displayName.contains("Merry", ignoreCase = true) -> "🎠"
                item.displayName.contains("Clock", ignoreCase = true) -> "⏰"
                item.displayName.contains("Dolphin", ignoreCase = true) -> "🐬"
                item.displayName.contains("Mickey", ignoreCase = true) || item.displayName.contains("Mouse", ignoreCase = true) -> "🐭"
                item.displayName.contains("OK", ignoreCase = true) -> "👌"
                item.displayName.contains("Shark", ignoreCase = true) -> "🦈"
                item.displayName.contains("Chicken", ignoreCase = true) -> "🐔"
                item.displayName.contains("Elephant", ignoreCase = true) -> "🐘"
                item.displayName.contains("Pigeon", ignoreCase = true) -> "🕊️"
                item.displayName.contains("Custom", ignoreCase = true) || item.displayName.contains("Design", ignoreCase = true) -> "🎨"
                item.displayName.contains("Lock", ignoreCase = true) -> "🔒"
                item.displayName.contains("Heart", ignoreCase = true) -> "❤️"
                else -> "🧩"
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.arrowDefault.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = iconEmoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.arrowCount} Arrows",
                        fontSize = 12.sp,
                        color = theme.textSecondary
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = theme.textSecondary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${item.xSize}×${item.ySize}",
                        fontSize = 12.sp,
                        color = theme.textSecondary
                    )
                }
            }

            // Play CTA Pod
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = theme.arrowDefault,
                shadowElevation = 2.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PLAY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}










