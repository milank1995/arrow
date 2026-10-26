path = 'arrow_native/app/src/main/java/com/arrowescape/puzzle/ui/screens/HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace("14,040 Authentic Levels", "5,000 Curated Levels")
c = c.replace("14,040", "5,000")

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)
print("Updated HomeScreen.kt successfully!")
