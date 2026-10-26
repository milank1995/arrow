import json

with open('arrow_native/app/src/main/assets/new_levels/manifest.json') as f:
    m = json.load(f)

arrow_counts = [item['arrowCount'] for item in m[:100]]
print('First 30 levels arrow counts:')
for i in range(0, 30, 5):
    chunk = [(i+j+1, arrow_counts[i+j]) for j in range(5) if i+j < len(arrow_counts)]
    print(' ', chunk)

print('\nArrow counts at milestones:')
for lvl in [1, 2, 3, 5, 10, 15, 20, 25, 26, 30, 40, 50, 75, 100]:
    if lvl <= len(arrow_counts):
        item = m[lvl-1]
        print(f"  Level {lvl:3d}: {item['arrowCount']:3d} arrows (Grid: {item['xSize']}x{item['ySize']})")
