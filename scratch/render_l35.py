import json

with open('arrow_native/app/src/main/assets/levels/Level_35.json') as f:
    lvl = json.load(f)

xs = lvl['XSize']
ys = lvl['YSize']
grid = [['.' for _ in range(xs)] for _ in range(ys)]

for i, a in enumerate(lvl['Arrows']):
    for idx in a['Indices']:
        x = idx % xs
        y = idx // xs
        grid[y][x] = '#'

print(f"Level 35 grid ({xs}x{ys}):")
for r in range(ys-1, -1, -1):
    print("".join(grid[r]))
