import sys
import json
sys.path.append('scripts')
from test_grand_piano_plan import build_grand_piano_plan, XS1, YS1

# Build canvas arrows
canvas_arrows = build_grand_piano_plan()

# Convert to Unity JSON format
unity_arrows = []
for a in canvas_arrows:
    coords = a['canvas_coords']
    cdx, cdy = a['cdx'], a['cdy']
    
    unity_indices = [((YS1 - 1) - cy) * XS1 + cx for cx, cy in coords]
    unity_dx = cdx
    unity_dy = -cdy
    
    # Calculate bends
    bends = 0
    if len(unity_indices) >= 3:
        for k in range(len(unity_indices) - 2):
            x1, y1 = unity_indices[k] % XS1, unity_indices[k] // XS1
            x2, y2 = unity_indices[k+1] % XS1, unity_indices[k+1] // XS1
            x3, y3 = unity_indices[k+2] % XS1, unity_indices[k+2] // XS1
            if (x2 - x1, y2 - y1) != (x3 - x2, y3 - y2):
                bends += 1
                
    unity_arrows.append({
        'Indices': unity_indices,
        'Dx': unity_dx,
        'Dy': unity_dy,
        'BendCount': bends
    })

level_33_data = {
    'LevelNumber': 33,
    'XSize': XS1,
    'YSize': YS1,
    'MaxHearts': 3,
    'ShapeName': 'Concert Grand Piano',
    'Difficulty': 'Hard',
    'ArrowCount': len(unity_arrows),
    'SourceCategory': 'Shape Puzzle Levels',
    'Arrows': unity_arrows
}

# Verify with game engine solver
def solve_level(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    occupied = {}
    heads, dirs, cells_list = [], [], []
    for i, a in enumerate(arrows):
        idxs = a['Indices']
        if not idxs: continue
        hx = idxs[0] % xs
        hy = (ys - 1) - (idxs[0] // xs)
        cdx = a['Dx']
        cdy = -a['Dy']
        heads.append((hx, hy))
        dirs.append((cdx, cdy))
        cset = set((idx % xs, (ys - 1) - (idx // xs)) for idx in idxs)
        for c in cset: occupied[c] = i
        cells_list.append(cset)
        
    active = list(range(len(heads)))
    steps = 0
    while active:
        prog = False
        for i in list(active):
            hx, hy = heads[i]
            dx, dy = dirs[i]
            cx, cy = hx + dx, hy + dy
            blocked = False
            while 0 <= cx < xs and 0 <= cy < ys:
                if (cx, cy) in occupied and occupied[(cx, cy)] != i:
                    blocked = True
                    break
                cx += dx
                cy += dy
            if not blocked:
                for c in cells_list[i]:
                    if c in occupied and occupied[c] == i:
                        del occupied[c]
                active.remove(i)
                prog = True
                steps += 1
                break
        if not prog:
            return False, steps
    return True, steps

ok, steps = solve_level(level_33_data)
assert ok, f"Solver failed: {steps}/{len(unity_arrows)}"
print(f"Level 33 Concert Grand Piano: {len(unity_arrows)} arrows, Solvable: {ok}")

# Save Level_33.json
with open('arrow_native/app/src/main/assets/levels/Level_33.json', 'w', encoding='utf-8') as f:
    json.dump(level_33_data, f, indent=2)
print("Saved Level_33.json")

# Update manifest.json
manifest_path = 'arrow_native/app/src/main/assets/levels/manifest.json'
with open(manifest_path, 'r', encoding='utf-8') as f:
    manifest = json.load(f)

for item in manifest:
    if item['LevelNumber'] == 33:
        item['ArrowCount'] = level_33_data['ArrowCount']
        item['Difficulty'] = level_33_data['Difficulty']
        item['ShapeName'] = level_33_data['ShapeName']
        item['XSize'] = level_33_data['XSize']
        item['YSize'] = level_33_data['YSize']

with open(manifest_path, 'w', encoding='utf-8') as f:
    json.dump(manifest, f, indent=2)
print("Updated manifest.json")
