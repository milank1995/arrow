import json

with open('arrow_native/app/src/main/assets/levels/manifest.json', encoding='utf-8') as f:
    manifest = json.load(f)

print(f"Total manifest levels: {len(manifest)}")

# 1. Starting levels
print("\n--- 1. Starting Levels 1-5 ---")
for m in manifest[:5]:
    print(f"Level {m['LevelNumber']}: {m['ArrowCount']} arrows | Diff: {m['Difficulty']} | Shape: {m['ShapeName']}")

# 2. Level 35
print("\n--- 2. Level 35 Check ---")
l35 = manifest[34]
print(f"Level 35: {l35['ArrowCount']} arrows, size: {l35['XSize']}x{l35['YSize']}, Source: {l35['SourceCategory']}")

# 3. Levels 51-55 & Level 53
print("\n--- 3. Levels 51-55 Range & Level 53 ---")
for m in manifest[50:55]:
    is_s = "SHAPE PUZZLE" if m['IsShape'] else "Normal Grid"
    print(f"Level {m['LevelNumber']}: {m['ArrowCount']:3d} arrows | {is_s:13s} | ShapeName: {m['ShapeName']}")

# 4. Levels 68-75 (Checking Levels 70, 71, 73, 74)
print("\n--- 4. Levels 68-75 Check (No Consecutive Shapes, L74 arrows) ---")
for m in manifest[67:75]:
    is_s = "SHAPE PUZZLE" if m['IsShape'] else "Normal Grid"
    print(f"Level {m['LevelNumber']}: {m['ArrowCount']:3d} arrows | {is_s:13s} | ShapeName: {m['ShapeName']}")

# 5. Level 79 Check
print("\n--- 5. Level 76-80 Range & Level 79 ---")
for m in manifest[75:80]:
    is_s = "SHAPE PUZZLE" if m['IsShape'] else "Normal Grid"
    print(f"Level {m['LevelNumber']}: {m['ArrowCount']:3d} arrows | {is_s:13s} | ShapeName: {m['ShapeName']}")

# 6. Check all shape levels and consecutive check
print("\n--- 6. All Shape Levels and Spacing ---")
shape_indices = [m['LevelNumber'] for m in manifest if m['IsShape']]
print(f"Total Shape levels: {len(shape_indices)}")
consecutive_shapes = []
for i in range(len(shape_indices) - 1):
    gap = shape_indices[i+1] - shape_indices[i]
    if gap <= 1:
        consecutive_shapes.append((shape_indices[i], shape_indices[i+1]))
    print(f"Shape L{shape_indices[i]:3d} ({manifest[shape_indices[i]-1]['ShapeName']}, {manifest[shape_indices[i]-1]['ArrowCount']} arr) -> gap of {gap:2d} -> next")
print(f"Shape L{shape_indices[-1]:3d} ({manifest[shape_indices[-1]-1]['ShapeName']}, {manifest[shape_indices[-1]-1]['ArrowCount']} arr)")

print(f"\nConsecutive shape pairs: {len(consecutive_shapes)} (should be 0)")

# 7. Check brackets 1 to 30
missing_brackets = []
for b in range(1, 31):
    start = (b - 1) * 5 + 1
    end = b * 5
    has_shape = any(m['IsShape'] for m in manifest[start-1:end])
    if not has_shape:
        missing_brackets.append(f"Bracket {b} (L{start}-L{end})")

print(f"Brackets without shape: {missing_brackets} (should be empty)")
