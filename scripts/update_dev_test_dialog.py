with open('arrow_native/app/src/main/java/com/arrowescape/puzzle/ui/screens/DevTestScreen.kt', 'r', encoding='utf-8') as f:
    txt = f.read()

# Replace LevelSelectorDialog with the full 14,040 level explorer dialog
target_dialog_start = """@Composable
private fun LevelSelectorDialog("""

idx = txt.find(target_dialog_start)
assert idx != -1, "LevelSelectorDialog not found"

prefix = txt[:idx]

new_dialog = """@Composable
private fun LevelSelectorDialog(
    currentLevel: Int,
    isNewLevelsMode: Boolean,
    onLevelSelected: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var inputLevelText by remember { mutableStateOf(currentLevel.toString()) }
    val maxLimit = LevelLoader.TOTAL_LEVELS // 14,040

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Explore Authentic Levels",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "14,040 Authentic Amaze Go Levels",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Level Number Input First
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputLevelText,
                        onValueChange = { inputLevelText = it },
                        label = { Text("Enter Level # (1-$maxLimit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val lvl = inputLevelText.toIntOrNull()?.coerceIn(1, maxLimit) ?: 1
                            onLevelSelected(lvl, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Load", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Quick Milestone & Shape Jumps:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val quickLevels = listOf(
                    1 to "Tutorial (4x5)",
                    5 to "Binoculars (50x18)",
                    10 to "Lightbulb (40x50)",
                    15 to "Tic-Tac-Toe #",
                    20 to "Cat Head (37x35)",
                    25 to "SuperHard U-Turn",
                    26 to "Cactus (130 arr)",
                    30 to "Crescent Moon",
                    50 to "Hard Normal",
                    100 to "Century L100",
                    500 to "Master L500",
                    1000 to "Pack 1 End",
                    5000 to "Pack 5 Mid",
                    10000 to "Pack 10 Master",
                    14040 to "Grand Finale"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickLevels.chunked(2).forEach { rowPair ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowPair.forEach { (lvl, desc) ->
                                val isCurrent = (lvl == currentLevel)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCurrent) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isCurrent) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onLevelSelected(lvl, true) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "L$lvl",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isCurrent) Color(0xFF4338CA) else Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = desc,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (rowPair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
"""

with open('arrow_native/app/src/main/java/com/arrowescape/puzzle/ui/screens/DevTestScreen.kt', 'w', encoding='utf-8') as f:
    f.write(prefix + new_dialog)

print("Updated DevTestScreen.kt LevelSelectorDialog successfully!")
