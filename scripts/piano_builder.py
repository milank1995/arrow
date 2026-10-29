import json

# Let's define the layout on a 36x40 grid:
# XSize = 36, YSize = 40
XS, YS = 36, 40

grid = [[" " for _ in range(XS)] for _ in range(YS)]

# Let's define the parts with distinct characters:
# 'L': Lid (Open angled lid)
# 'P': Prop Stick
# 'S': Soundboard / Harp Strings
# 'R': Rim & Case
# 'M': Music Stand & Sheet
# 'K': Keyboard & Keys
# 'F': Legs (Front left, front right, rear)
# 'Y': Pedal Lyre
# 'B': Bench / Stool

# 1. Lid (angled wing)
for x in range(8, 29):
    # Lid slopes from left (y=12) to right (y=3)
    # y = int(12 - (x - 8) * 0.45)
    top_y = int(12 - (x - 8) * 0.42)
    for y in range(top_y, top_y + 3):
        grid[y][x] = "L"

# 2. Prop Stick (holding lid up)
for y in range(8, 16):
    grid[y][23] = "P"

# 3. Soundboard / Strings (inside body cavity)
for y in range(12, 18):
    for x in range(10, 24):
        if grid[y][x] == " ":
            grid[y][x] = "S"

# 4. Rim & Case
# Left curved bentside
for y in range(12, 20):
    grid[y][7] = "R"
    grid[y][8] = "R"
# Back tail
for x in range(9, 14):
    grid[10][x] = "R"
    grid[11][x] = "R"
# Right straight rim
for y in range(9, 20):
    grid[y][25] = "R"
    grid[y][26] = "R"
# Fallboard / Keybed frame
for x in range(7, 27):
    grid[19][x] = "R"

# 5. Music Stand
for x in range(13, 21):
    grid[17][x] = "M"
    grid[18][x] = "M"

# 6. Keyboard
for y in range(20, 24):
    for x in range(8, 26):
        grid[y][x] = "K"

# 7. Legs
# Left front leg
for y in range(24, 34):
    grid[y][7] = "F"
    grid[y][8] = "F"
# Right front leg
for y in range(24, 34):
    grid[y][25] = "F"
    grid[y][26] = "F"
# Rear leg
for y in range(18, 28):
    grid[y][12] = "F"

# 8. Pedal Lyre
for y in range(25, 33):
    grid[y][16] = "Y"
    grid[y][17] = "Y"

# 9. Piano Bench (sitting in front)
# Bench cushion
for x in range(11, 23):
    grid[29][x] = "B"
    grid[30][x] = "B"
# Bench legs
for y in range(31, 37):
    grid[y][11] = "B"
    grid[y][22] = "B"

for y in range(YS):
    row_str = "".join(grid[y])
    if row_str.strip():
        print(f"{y:2d}: {row_str}")
