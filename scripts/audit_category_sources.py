import json
import os

with open('manifest_existing_1k.json', 'r', encoding='utf-8') as f:
    m1 = json.load(f)
with open('manifest_unity_normal.json', 'r', encoding='utf-8') as f:
    m2 = json.load(f)
with open('manifest_shape_puzzles.json', 'r', encoding='utf-8') as f:
    m3 = json.load(f)
with open('manifest_large_puzzles.json', 'r', encoding='utf-8') as f:
    m4 = json.load(f)

print("=== CATEGORY 1: Existing 1K Levels ===")
print(f"Total levels: {len(m1)}")
print(f"From Existing 1K files: {len([x for x in m1 if 'Level_' in x.get('Source', '')])}")
print(f"From Unity: {len([x for x in m1 if 'Unity' in x.get('Source', '')])}")
print(f"Shape puzzles inside Category 1: {len([x for x in m1 if x.get('ShapeName')])}")

print("\n=== CATEGORY 2: New Unity Normal Levels ===")
print(f"Total levels: {len(m2)}")
print(f"From Existing 1K files: {len([x for x in m2 if 'Level_' in x.get('Source', '')])}")
print(f"From Unity: {len([x for x in m2 if 'Unity' in x.get('Source', '')])}")
print(f"Shape puzzles inside Category 2: {len([x for x in m2 if x.get('ShapeName')])}")

print("\n=== CATEGORY 3: Shape Puzzle Levels ===")
print(f"Total levels: {len(m3)}")
print(f"From Existing 1K files: {len([x for x in m3 if 'Existing1K' in x.get('Source', '') or 'Level_' in x.get('Source', '')])}")
print(f"From Unity asset bundle: {len([x for x in m3 if x.get('Source') == 'Unity'])}")
print(f"From NewLevels (Unity-extracted): {len([x for x in m3 if x.get('Source') == 'NewLevels'])}")
print(f"Shape puzzles inside Category 3: {len([x for x in m3 if x.get('ShapeName')])}")

print("\n=== CATEGORY 4: Large Puzzles ===")
print(f"Total levels: {len(m4)}")
print(f"From Existing 1K files: {len([x for x in m4 if 'Level_' in x.get('Source', '')])}")
print(f"From Unity asset bundle: {len([x for x in m4 if 'Unity' in x.get('Source', '')])}")
print(f"Shape puzzles inside Category 4: {len([x for x in m4 if x.get('ShapeName')])}")
