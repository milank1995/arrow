path = 'arrow_native/app/src/main/java/com/arrowescape/puzzle/ui/screens/DevTestScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace("5,000 Curated Authentic Levels", "2,000 Curated Levels")

old_quick = """                val quickLevels = listOf(
                    1 to "Tutorial (4 arr)",
                    4 to "Fish Shape (15 arr)",
                    9 to "Cup Shape (20 arr)",
                    14 to "Cat Shape (25 arr)",
                    19 to "Binoculars (32 arr)",
                    24 to "Heart Shape (38 arr)",
                    29 to "Jet Shape (46 arr)",
                    30 to "Chapter 1 (50 arr)",
                    50 to "Casual L50",
                    100 to "Century L100",
                    500 to "Pack 00 Mid",
                    1000 to "Pack 00 Finale",
                    2500 to "Mid-Game Master",
                    5000 to "Grand Finale"
                )"""

new_quick = """                val quickLevels = listOf(
                    1 to "Tutorial (4 arr)",
                    5 to "Shape 1 (54 arr)",
                    10 to "Showstopper (99 arr)",
                    15 to "Shape 2 (53 arr)",
                    20 to "Cat Shape (49 arr)",
                    25 to "SuperHard (74 arr)",
                    26 to "Cactus (130 arr)",
                    30 to "Moon Shape (68 arr)",
                    31 to "L31 Flow (28 arr)",
                    50 to "Golden L50 (48 arr)",
                    100 to "Century L100",
                    500 to "Pack 00 Mid",
                    1000 to "Pack 00 Finale",
                    2000 to "Grand Finale"
                )"""

c_norm = c.replace('\r\n', '\n')
old_quick_norm = old_quick.replace('\r\n', '\n')
new_quick_norm = new_quick.replace('\r\n', '\n')

if old_quick_norm in c_norm:
    c_norm = c_norm.replace(old_quick_norm, new_quick_norm)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(c_norm)
    print("Updated DevTestScreen.kt quickLevels successfully!")
else:
    print("old_quick not found in DevTestScreen.kt")
