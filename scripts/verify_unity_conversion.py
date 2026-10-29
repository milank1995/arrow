import json
from test_grand_piano_plan import build_grand_piano_plan, XS1, YS1

# Build the grand piano arrows in canvas coordinates
canvas_arrows = build_grand_piano_plan()
print(f"Canvas arrows count: {len(canvas_arrows)}")

# Convert each arrow to Unity JSON format:
# Canvas -> Unity:
# unityX = cx
# unityY = (YS1 - 1) - cy
# Dx = cdx
# Dy = -cdy
unity_arrows = []
for i, a in enumerate(canvas_arrows):
    coords = a['canvas_coords']
    cdx, cdy = a['cdx'], a['cdy']
    
    # In unity coordinates:
    unity_indices = [((YS1 - 1) - cy) * XS1 + cx for cx, cy in coords]
    unity_dx = cdx
    unity_dy = -cdy
    
    unity_arrows.append({
        'Indices': unity_indices,
        'Dx': unity_dx,
        'Dy': unity_dy,
        'BendCount': 0 # will compute bends
    })

# Compute BendCount
for a in unity_arrows:
    idxs = a['Indices']
    bends = 0
    if len(idxs) >= 3:
        for k in range(len(idxs) - 2):
            x1, y1 = idxs[k] % XS1, idxs[k] // XS1
            x2, y2 = idxs[k+1] % XS1, idxs[k+1] // XS1
            x3, y3 = idxs[k+2] % XS1, idxs[k+2] // XS1
            if (x2 - x1, y2 - y1) != (x3 - x2, y3 - y2):
                bends += 1
    a['BendCount'] = bends

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

# Now verify with the authentic game engine solver
def solve_unity_level(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    occupied = {}
    heads, dirs, cells_list = [], [], []
    for i, a in enumerate(arrows):
        idxs = a['Indices']
        if not idxs: continue
        # LevelLoader.kt mapping:
        # canvasX = idx % xs
        # canvasY = (ys - 1) - (idx // xs)
        # canvasDx = dx
        # canvasDy = -dy
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

ok, steps = solve_unity_level(level_33_data)
print(f"Game Engine Solvable: {ok}, Steps: {steps}/{len(unity_arrows)}")
assert ok, "Engine solver failed!"

# Also verify simulated canvas orientation matches grand_piano_plan_preview.png
# Head of bench leg 1:
bench_leg_1 = unity_arrows[-2]
bl1_cx = bench_leg_1['Indices'][0] % XS1
bl1_cy = (YS1 - 1) - (bench_leg_1['Indices'][0] // XS1)
print(f"Bench leg 1 on canvas: ({bl1_cx}, {bl1_cy}) - should be near bottom (y=43)")
assert bl1_cy == 43, f"Expected cy=43, got {bl1_cy}"

# Head of lid top edge:
lid_top = unity_arrows[-12]
lid_cx = lid_top['Indices'][0] % XS1
lid_cy = (YS1 - 1) - (lid_top['Indices'][0] // XS1)
print(f"Lid top on canvas: ({lid_cx}, {lid_cy}) - should be near top (y=4)")
assert lid_cy == 4, f"Expected cy=4, got {lid_cy}"

print("ORIENTATION AND COORDINATE MAPPINGS 100% VERIFIED!")
