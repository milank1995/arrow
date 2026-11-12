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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Dedicated screen for Unique Shapes, matching the custom-shapes design.
 * Features:
 * - Top App Bar: Circular Back button + Title & Level Count + Balanced spacer
 * - Search bar: Instant shape name / category / level lookup
 * - Category filter chips: Quick filtering by theme (Animals, Objects, Nature, Food, etc.)
 * - Rich Shape Cards: Emoji pod, Level badge, Title, Dimensions, Arrow count, and PLAY CTA pod
 */
@Composable
fun UniqueShapesScreen(
    theme: ThemePreset,
    onBackClick: () -> Unit,
    onLevelSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler(onBack = onBackClick)

    val allShapes = remember {
        LevelLoader.getCuratedManifest(context)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(allShapes) {
        val list = mutableListOf("All")
        val set = linkedSetOf<String>()
        allShapes.forEach { item ->
            item.category?.let { set.add(it) }
        }
        list.addAll(set)
        list
    }

    val filteredShapes = remember(allShapes, searchQuery, selectedCategory) {
        allShapes.filter { item ->
            val matchesCategory = (selectedCategory == "All") || (item.category == selectedCategory)
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                (item.shapeName?.lowercase()?.contains(q) == true) ||
                (item.category?.lowercase()?.contains(q) == true) ||
                ("level ${item.levelNumber}".contains(q)) ||
                ("${item.levelNumber}" == q)
            matchesCategory && matchesSearch
        }
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
                        text = "Unique Shapes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    Text(
                        text = if (filteredShapes.size == allShapes.size) {
                            "Curated Shapes (${allShapes.size})"
                        } else {
                            "${filteredShapes.size} of ${allShapes.size} Shapes"
                        },
                        fontSize = 11.5.sp,
                        color = theme.textSecondary
                    )
                }

                // Balance Spacer
                Spacer(modifier = Modifier.size(42.dp))
            }

            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search 350 shapes (e.g. Panda, Crown, 42)...",
                        fontSize = 13.sp,
                        color = theme.textSecondary.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = theme.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = theme.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.arrowDefault,
                    unfocusedBorderColor = theme.gridLine,
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary,
                    cursorColor = theme.arrowDefault
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // CATEGORY FILTER CHIPS ROW
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    val count = if (cat == "All") allShapes.size else allShapes.count { it.category == cat }
                    val label = when (cat) {
                        "All" -> "All ($count)"
                        "Animals & Wildlife" -> "🐾 Animals ($count)"
                        "Objects & Symbols" -> "💎 Objects ($count)"
                        "Flora & Nature" -> "🌿 Nature ($count)"
                        "Food & Refreshments" -> "🍎 Food ($count)"
                        "Mythology & Zodiac" -> "🔮 Zodiac ($count)"
                        "Music, Art & Culture" -> "🎨 Art ($count)"
                        "World Landmarks" -> "🏛️ Landmarks ($count)"
                        "Vehicles & Travel" -> "🚀 Vehicles ($count)"
                        else -> "$cat ($count)"
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) theme.arrowDefault else theme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) theme.arrowDefault else theme.gridLine.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else theme.textPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredShapes.isEmpty()) {
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
                        Text(text = "🔍", fontSize = 44.sp)
                        Text(
                            text = "No Shapes Found",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary
                        )
                        Text(
                            text = "Try searching with a different shape name or select 'All'.",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    items(filteredShapes, key = { it.levelNumber }) { shapeItem ->
                        UniqueShapeCard(
                            item = shapeItem,
                            theme = theme,
                            onClick = { onLevelSelected(shapeItem.levelNumber) }
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
private fun UniqueShapeCard(
    item: LevelLoader.ManifestLevelItem,
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
            // Icon pod with emoji
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.arrowDefault.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.emoji ?: "🧩", fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = theme.textPrimary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "L${item.levelNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = item.shapeName ?: "Level ${item.levelNumber}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                }

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
                    if (!item.category.isNullOrBlank()) {
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = theme.textSecondary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = item.category,
                            fontSize = 12.sp,
                            color = theme.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play CTA Pod
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = theme.arrowDefault,
                shadowElevation = 2.dp,
                modifier = Modifier.clickable(onClick = onClick)
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
