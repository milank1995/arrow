import json
from collections import defaultdict

XS, YS = 36, 40

def make_arrow(coords, head_idx, dx, dy):
    if head_idx == -1:
        ordered = list(reversed(coords))
    else:
        ordered = list(coords)
    indices = [y * XS + x for x, y in ordered]
    bends = 0
    if len(ordered) >= 3:
        for i in range(len(ordered) - 2):
            dx1 = ordered[i+1][0] - ordered[i][0]
            dy1 = ordered[i+1][1] - ordered[i][1]
            dx2 = ordered[i+2][0] - ordered[i+1][0]
            dy2 = ordered[i+2][1] - ordered[i+1][1]
            if (dx1, dy1) != (dx2, dy2):
                bends += 1
    return {
        'Indices': indices,
        'Dx': dx,
        'Dy': dy,
        'BendCount': bends
    }

def solve(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows: return False, 0, []
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
    solve_order = []
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
                solve_order.append(i)
                prog = True
                break
        if not prog:
            return False, len(solve_order), solve_order
    return True, len(solve_order), solve_order

def generate_piano():
    arrows = []
    occupied_cells = set()

    def add(coords, head_idx, dx, dy, name=""):
        for c in coords:
            if c in occupied_cells:
                raise ValueError(f"Cell {c} already occupied by another arrow! In {name}")
            occupied_cells.add(c)
        arrows.append(make_arrow(coords, head_idx, dx, dy))

    # --- 1. BENCH LEGS (Exit DOWN) ---
    add([(11, y) for y in range(31, 37)], -1, 0, 1, "bench_left_leg") # head at (11, 36) -> down
    add([(22, y) for y in range(31, 37)], -1, 0, 1, "bench_right_leg") # head at (22, 36) -> down

    # --- 2. BENCH CUSHION ---
    # Top row y=29
    add([(x, 29) for x in range(11, 14)], 0, -1, 0, "bench_cush_top_l") # head at (11, 29) -> left
    add([(x, 29) for x in range(14, 17)], 0, 0, 1, "bench_cush_top_ml") # head at (14, 29) -> down
    add([(x, 29) for x in range(17, 20)], -1, 0, 1, "bench_cush_top_mr") # head at (19, 29) -> down
    add([(x, 29) for x in range(20, 23)], -1, 1, 0, "bench_cush_top_r") # head at (22, 29) -> right
    # Bottom row y=30
    add([(x, 30) for x in range(11, 14)], 0, -1, 0, "bench_cush_bot_l") # head at (11, 30) -> left
    add([(x, 30) for x in range(14, 17)], 0, 0, 1, "bench_cush_bot_ml") # head at (14, 30) -> down
    add([(x, 30) for x in range(17, 20)], -1, 0, 1, "bench_cush_bot_mr") # head at (19, 30) -> down
    add([(x, 30) for x in range(20, 23)], -1, 1, 0, "bench_cush_bot_r") # head at (22, 30) -> right

    # --- 3. PIANO LEGS & FEET ---
    # Left front leg
    add([(7, y) for y in range(24, 29)], 0, -1, 0, "leg_l_top_out") # head at (7, 24) -> left
    add([(7, y) for y in range(29, 34)], -1, 0, 1, "leg_l_bot_out") # head at (7, 33) -> down
    add([(8, y) for y in range(24, 29)], 0, -1, 0, "leg_l_top_in") # head at (8, 24) -> left
    add([(8, y) for y in range(29, 34)], -1, 0, 1, "leg_l_bot_in") # head at (8, 33) -> down

    # Right front leg
    add([(26, y) for y in range(24, 29)], 0, 1, 0, "leg_r_top_out") # head at (26, 24) -> right
    add([(26, y) for y in range(29, 34)], -1, 0, 1, "leg_r_bot_out") # head at (26, 33) -> down
    add([(25, y) for y in range(24, 29)], 0, 1, 0, "leg_r_top_in") # head at (25, 24) -> right
    add([(25, y) for y in range(29, 34)], -1, 0, 1, "leg_r_bot_in") # head at (25, 33) -> down

    # Rear leg (under rear curve/tail)
    add([(10, y) for y in range(12, 17)], 0, -1, 0, "rear_leg_out") # head at (10, 12) -> left
    add([(11, y) for y in range(12, 17)], -1, 0, 1, "rear_leg_in")  # head at (11, 16) -> down

    # --- 4. PEDAL LYRE (Behind/above bench) ---
    add([(16, y) for y in range(24, 29)], -1, 0, 1, "lyre_l") # head at (16, 28) -> down
    add([(17, y) for y in range(24, 29)], -1, 0, 1, "lyre_r") # head at (17, 28) -> down
    # Brass pedals (visible under bench between bench legs x=15..18, y=34)
    add([(x, 34) for x in range(15, 19)], 0, 0, 1, "pedals") # head at (15, 34) -> down

    # --- 5. KEYBOARD (18 keys: x=8..25, y=20..23) ---
    for x in range(8, 26):
        # Alternating black & white keys
        if x in [9, 11, 14, 16, 18, 21, 23]:
            # Black keys: shorter, y=20..22 (authentic recessed black key)
            add([(x, y) for y in range(20, 23)], -1, 0, 1, f"black_key_{x}")
        else:
            # White keys: full length, y=20..23
            add([(x, y) for y in range(20, 24)], -1, 0, 1, f"white_key_{x}")

    # --- 6. FALLBOARD & KEYBED RIM (y=19) ---
    add([(x, 19) for x in range(7, 12)], 0, -1, 0, "fallboard_l") # head at (7, 19) -> left
    add([(x, 19) for x in range(12, 17)], 0, -1, 0, "fallboard_ml") # head at (12, 19) -> left
    add([(x, 19) for x in range(17, 22)], -1, 1, 0, "fallboard_mr") # head at (21, 19) -> right
    add([(x, 19) for x in range(22, 27)], -1, 1, 0, "fallboard_r") # head at (26, 19) -> right

    # --- 7. MUSIC STAND & SHEET MUSIC ---
    # Sheet music
    add([(x, 17) for x in range(13, 17)], 0, 0, -1, "sheet_l") # head at (13, 17) -> up
    add([(x, 17) for x in range(17, 21)], -1, 0, -1, "sheet_r") # head at (20, 17) -> up
    # Music desk shelf
    add([(x, 18) for x in range(13, 17)], 0, -1, 0, "music_shelf_l") # head at (13, 18) -> left
    add([(x, 18) for x in range(17, 21)], -1, 1, 0, "music_shelf_r") # head at (20, 18) -> right

    # --- 8. SOUNDBOARD STRINGS ---
    # y=12..16, x=12..22 (excluding prop stick at x=23)
    add([(x, 12) for x in range(12, 17)], 0, 0, -1, "string_12_l")
    add([(x, 12) for x in range(17, 23)], -1, 1, 0, "string_12_r")

    add([(x, 13) for x in range(12, 17)], 0, -1, 0, "string_13_l")
    add([(x, 13) for x in range(17, 23)], -1, 1, 0, "string_13_r")

    add([(x, 14) for x in range(12, 17)], 0, -1, 0, "string_14_l")
    add([(x, 14) for x in range(17, 23)], -1, 1, 0, "string_14_r")

    add([(x, 15) for x in range(12, 17)], 0, -1, 0, "string_15_l")
    add([(x, 15) for x in range(17, 23)], -1, 1, 0, "string_15_r")

    add([(x, 16) for x in range(12, 17)], 0, -1, 0, "string_16_l")
    add([(x, 16) for x in range(17, 23)], -1, 1, 0, "string_16_r")

    # --- 9. PIANO RIM & CASE ---
    # Left bentside curve (y=12..18, x=7..8)
    add([(7, y) for y in range(12, 16)], 0, -1, 0, "rim_l_top_out")
    add([(7, y) for y in range(16, 19)], 0, -1, 0, "rim_l_bot_out")
    add([(8, y) for y in range(12, 16)], 0, -1, 0, "rim_l_top_in")
    add([(8, y) for y in range(16, 19)], 0, -1, 0, "rim_l_bot_in")

    # Tail curve (x=9..13, y=10..11)
    add([(x, 10) for x in range(9, 14)], 0, 0, -1, "tail_10")
    add([(x, 11) for x in range(9, 14)], 0, 0, -1, "tail_11")

    # Right spine rim (y=9..18, x=25..26)
    add([(25, y) for y in range(9, 14)], 0, 1, 0, "rim_r_top_in")
    add([(25, y) for y in range(14, 19)], 0, 1, 0, "rim_r_bot_in")
    add([(26, y) for y in range(9, 14)], 0, 1, 0, "rim_r_top_out")
    add([(26, y) for y in range(14, 19)], 0, 1, 0, "rim_r_bot_out")

    # --- 10. PROP STICK ---
    add([(23, y) for y in range(8, 12)], 0, 0, -1, "prop_top") # head at (23, 8) -> up
    add([(23, y) for y in range(12, 16)], -1, 0, 1, "prop_bot") # head at (23, 15) -> down

    # --- 11. OPEN LID (Angled wing) ---
    # Lid slopes from left (y=12) to right (y=3)
    # Let's build lid strips
    lid_cells = []
    for x in range(8, 29):
        top_y = int(12 - (x - 8) * 0.42)
        for y in range(top_y, top_y + 3):
            if (x, y) not in occupied_cells:
                lid_cells.append((x, y))

    # Group lid cells into horizontal or diagonal arrows
    # Group by y
    by_y = defaultdict(list)
    for x, y in sorted(lid_cells):
        by_y[y].append(x)

    lid_idx = 0
    for y, xs_in_row in sorted(by_y.items()):
        # Split into chunks of 3-5
        chunk = []
        for x in xs_in_row:
            if not chunk or x == chunk[-1] + 1:
                chunk.append(x)
            else:
                if len(chunk) >= 2:
                    coords = [(cx, y) for cx in chunk]
                    add(coords, 0 if y < 7 else -1, 0, -1, f"lid_{lid_idx}")
                    lid_idx += 1
                chunk = [x]
        if len(chunk) >= 2:
            coords = [(cx, y) for cx in chunk]
            add(coords, 0 if y < 7 else -1, 0, -1, f"lid_{lid_idx}")
            lid_idx += 1
        elif len(chunk) == 1:
            # Single cell: attach to nearby if possible or make 1-cell
            coords = [(chunk[0], y)]
            add(coords, 0, 0, -1, f"lid_{lid_idx}")
            lid_idx += 1

    lvl = {
        'XSize': XS,
        'YSize': YS,
        'MaxHearts': 3,
        'ShapeName': 'GrandPiano',
        'Difficulty': 'Medium',
        'Arrows': arrows
    }
    return lvl

lvl = generate_piano()
print(f"Generated Grand Piano with {len(lvl['Arrows'])} arrows!")
ok, steps, order = solve(lvl)
print(f"Solvable: {ok}, Steps cleared: {steps}/{len(lvl['Arrows'])}")

if not ok:
    cleared = set(order)
    xs = lvl['XSize']
    ys = lvl['YSize']
    for i, a in enumerate(lvl['Arrows']):
        if i not in cleared:
            h = a['Indices'][0]
            hx = h % xs
            hy = h // xs
            print(f"Blocked arrow {i}: head at ({hx}, {hy}), dir=({a['Dx']}, {a['Dy']}), len={len(a['Indices'])}")

