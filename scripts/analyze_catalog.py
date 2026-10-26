import json
from collections import Counter

with open('scripts/unity_normal_levels_catalog.json', 'r', encoding='utf-8') as f:
    catalog = json.load(f)

print(f"Total levels in catalog: {len(catalog)}")

arrow_counts = [x['arrowCount'] for x in catalog]
min_arr = min(arrow_counts)
max_arr = max(arrow_counts)

# Brackets
brackets = {
    'Tutorial (4-10 arrows)': 0,
    'Easy (11-25 arrows)': 0,
    'Medium (26-50 arrows)': 0,
    'Hard (51-80 arrows)': 0,
    'Expert (81-120 arrows)': 0,
    'Master (121-180 arrows)': 0,
    'Giant / Nightmare (181+ arrows)': 0
}

for a in arrow_counts:
    if 4 <= a <= 10:
        brackets['Tutorial (4-10 arrows)'] += 1
    elif 11 <= a <= 25:
        brackets['Easy (11-25 arrows)'] += 1
    elif 26 <= a <= 50:
        brackets['Medium (26-50 arrows)'] += 1
    elif 51 <= a <= 80:
        brackets['Hard (51-80 arrows)'] += 1
    elif 81 <= a <= 120:
        brackets['Expert (81-120 arrows)'] += 1
    elif 121 <= a <= 180:
        brackets['Master (121-180 arrows)'] += 1
    else:
        brackets['Giant / Nightmare (181+ arrows)'] += 1

print("\n--- Arrow Count Distribution ---")
for k, v in brackets.items():
    pct = v / len(catalog) * 100
    print(f"  {k:35s}: {v:5d} levels ({pct:5.1f}%)")

# Grid dimensions
grid_sizes = [(x['xSize'], x['ySize']) for x in catalog]
common_grids = Counter(grid_sizes).most_common(10)
print("\n--- Top 10 Most Common Grid Sizes ---")
for (xs, ys), cnt in common_grids:
    print(f"  {xs:2d} x {ys:2d}: {cnt:5d} levels")

# Shape & Milestone analysis
shapes = [x for x in catalog if x['isShape']]
print(f"\n--- Shape & Custom Silhouette Levels: {len(shapes)} ---")

# Group shapes by arrow range
shape_brackets = {
    'Small Shapes (< 40 arrows)': len([s for s in shapes if s['arrowCount'] < 40]),
    'Medium Shapes (40-80 arrows)': len([s for s in shapes if s['arrowCount'] >= 40 and s['arrowCount'] <= 80]),
    'Large Shapes (81-120 arrows)': len([s for s in shapes if s['arrowCount'] > 80 and s['arrowCount'] <= 120]),
    'Giant Shapes (121+ arrows)': len([s for s in shapes if s['arrowCount'] > 120])
}
for k, v in shape_brackets.items():
    print(f"  {k:32s}: {v:4d} levels")

# SuperHard and Hard levels
super_hard_levels = [x for x in catalog if any('superhard' in t.lower() for t in x['tables'])]
hard_levels = [x for x in catalog if any('hard' in t.lower() and 'superhard' not in t.lower() for t in x['tables'])]
guide_levels = [x for x in catalog if any('guide' in t.lower() for t in x['tables'])]

print(f"\n--- Special Gameplay Categories ---")
print(f"  Official Guide / Tutorial Levels: {len(guide_levels)}")
print(f"  Official Hard Challenge Levels  : {len(hard_levels)}")
print(f"  Official SuperHard Boss Levels  : {len(super_hard_levels)}")
