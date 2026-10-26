path = 'arrow_native/app/src/main/java/com/arrowescape/puzzle/ui/screens/DevTestScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

target = """                    Button(
                        onClick = {
                            val lvl = inputLevelText.toIntOrNull()?.coerceIn(1, maxLimit) ?: 1
                            onLevelSelected(lvl, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.height(56.dp)
                    ) {"""

replacement = """                    Button(
                        onClick = {
                            val parsed = inputLevelText.replace(",", "").replace(".", "").replace(" ", "").trim().toIntOrNull()
                            val lvl = parsed?.coerceIn(1, maxLimit) ?: 1
                            onLevelSelected(lvl, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.height(56.dp)
                    ) {"""

c_norm = c.replace('\r\n', '\n')
target_norm = target.replace('\r\n', '\n')
replacement_norm = replacement.replace('\r\n', '\n')

if target_norm in c_norm:
    c_norm = c_norm.replace(target_norm, replacement_norm)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(c_norm)
    print("Updated DevTestScreen.kt successfully!")
else:
    print("Target not found in DevTestScreen.kt")
