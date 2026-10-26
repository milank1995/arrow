with open('arrow_native/app/src/main/java/com/arrowescape/puzzle/ui/screens/HomeScreen.kt', 'r', encoding='utf-8') as f:
    txt = f.read()

target = """                // New Levels Test Hub Button (Testing 1,533 Authentic Sequential Levels)
                AppSecondaryButton(
                    text = "🧪 New Levels (1,533 Normal Levels)",
                    onClick = onDevTestClick,
                    theme = theme,
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.88f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // New Levels Test Hub Button (Testing 1,533 Authentic Sequential Levels)
                AppSecondaryButton(
                    text = "🧪 New Levels (1,533 Normal Levels)",
                    onClick = onDevTestClick,
                    theme = theme,
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.88f)
                )"""

replacement = """                // Explore All 14,040 Authentic Levels
                AppSecondaryButton(
                    text = "🧪 Explore All 14,040 Levels",
                    onClick = onDevTestClick,
                    theme = theme,
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.88f)
                )"""

txt_norm = txt.replace('\r\n', '\n')
target_norm = target.replace('\r\n', '\n')
replacement_norm = replacement.replace('\r\n', '\n')

assert target_norm in txt_norm, "Target not found in HomeScreen.kt"
txt_norm = txt_norm.replace(target_norm, replacement_norm)

with open('arrow_native/app/src/main/java/com/arrowescape/puzzle/ui/screens/HomeScreen.kt', 'w', encoding='utf-8') as f:
    f.write(txt_norm)

print("Updated HomeScreen.kt successfully!")
