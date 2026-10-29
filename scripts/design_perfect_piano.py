import json
from collections import defaultdict

XS = 36
YS = 40

def solve_canvas(arrows, xs, ys):
    occupied = {}
    heads, dirs, cells_list = [], [], []
    for i, a in enumerate(arrows):
        coords = a['canvas_coords']
        cdx = a['cdx']
        cdy = a['cdy']
        heads.append(coords[0])
        dirs.append((cdx, cdy))
        cset = set(coords)
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

def build_grand_piano():
    arrows = []
    occupied = set()

    def add(coords, head_idx, cdx, cdy, name=""):
        for c in coords:
            if c in occupied:
                raise ValueError(f"Collision at {c} in {name}")
            occupied.add(c)
        if head_idx == -1:
            ordered = list(reversed(coords))
        else:
            ordered = list(coords)
        arrows.append({
            'name': name,
            'canvas_coords': ordered,
            'cdx': cdx,
            'cdy': cdy
        })

    # 1. BENCH LEGS (Exit DOWN off bottom screen)
    add([(11, cy) for cy in range(31, 37)], -1, 0, 1, "bench_leg_l")
    add([(22, cy) for cy in range(31, 37)], -1, 0, 1, "bench_leg_r")

    # 2. BENCH CUSHION
    add([(cx, 29) for cx in range(11, 14)], 0, -1, 0, "bench_cush_tl")
    add([(cx, 29) for cx in range(14, 17)], 0, 0, 1, "bench_cush_tml")
    add([(cx, 29) for cx in range(17, 20)], -1, 0, 1, "bench_cush_tmr")
    add([(cx, 29) for cx in range(20, 23)], -1, 1, 0, "bench_cush_tr")

    add([(cx, 30) for cx in range(11, 14)], 0, -1, 0, "bench_cush_bl")
    add([(cx, 30) for cx in range(14, 17)], 0, 0, 1, "bench_cush_bml")
    add([(cx, 30) for cx in range(17, 20)], -1, 0, 1, "bench_cush_bmr")
    add([(cx, 30) for cx in range(20, 23)], -1, 1, 0, "bench_cush_br")

    # 3. FRONT LEGS & REAR LEG
    # Left front leg
    add([(7, cy) for cy in range(24, 29)], 0, -1, 0, "leg_fl_top_out")
    add([(7, cy) for cy in range(29, 34)], -1, 0, 1, "leg_fl_bot_out")
    add([(8, cy) for cy in range(24, 29)], 0, -1, 0, "leg_fl_top_in")
    add([(8, cy) for cy in range(29, 34)], -1, 0, 1, "leg_fl_bot_in")

    # Right front leg
    add([(26, cy) for cy in range(24, 29)], 0, 1, 0, "leg_fr_top_out")
    add([(26, cy) for cy in range(29, 34)], -1, 0, 1, "leg_fr_bot_out")
    add([(25, cy) for cy in range(24, 29)], 0, 1, 0, "leg_fr_top_in")
    add([(25, cy) for cy in range(29, 34)], -1, 0, 1, "leg_fr_bot_in")

    # Rear leg under tail
    add([(10, cy) for cy in range(12, 17)], 0, -1, 0, "leg_rear_out")
    add([(11, cy) for cy in range(12, 17)], -1, 0, 1, "leg_rear_in")

    # 4. PEDAL LYRE & BRASS PEDALS
    add([(16, cy) for cy in range(24, 29)], -1, 0, 1, "lyre_l")
    add([(17, cy) for cy in range(24, 29)], -1, 0, 1, "lyre_r")
    add([(cx, 34) for cx in range(15, 19)], 0, 0, 1, "pedals")

    # 5. KEYBOARD (18 keys: cx=8..25, cy=20..23)
    for cx in range(8, 26):
        if cx in [9, 11, 14, 16, 18, 21, 23]:
            # Black keys: cy=20..22 (recessed, 3 cells)
            add([(cx, cy) for cy in range(20, 23)], -1, 0, 1, f"black_key_{cx}")
        else:
            # White keys: cy=20..23 (full 4 cells)
            add([(cx, cy) for cy in range(20, 24)], -1, 0, 1, f"white_key_{cx}")

    # 6. FALLBOARD & KEYBED RIM (cy=19)
    add([(cx, 19) for cx in range(7, 12)], 0, -1, 0, "fallboard_l")
    add([(cx, 19) for cx in range(12, 17)], 0, -1, 0, "fallboard_ml")
    add([(cx, 19) for cx in range(17, 22)], -1, 1, 0, "fallboard_mr")
    add([(cx, 19) for cx in range(22, 27)], -1, 1, 0, "fallboard_r")

    # 7. MUSIC STAND & SHEET MUSIC
    add([(cx, 17) for cx in range(13, 17)], 0, 0, -1, "sheet_l")
    add([(cx, 17) for cx in range(17, 21)], -1, 0, -1, "sheet_r")
    add([(cx, 18) for cx in range(13, 17)], 0, -1, 0, "shelf_l")
    add([(cx, 18) for cx in range(17, 21)], -1, 1, 0, "shelf_r")

    # 8. SOUNDBOARD STRINGS (cy=12..16, cx=12..22)
    add([(cx, 12) for cx in range(12, 17)], 0, 0, -1, "string_12_l")
    add([(cx, 12) for cx in range(17, 23)], -1, 1, 0, "string_12_r")

    add([(cx, 13) for cx in range(12, 17)], 0, -1, 0, "string_13_l")
    add([(cx, 13) for cx in range(17, 23)], -1, 1, 0, "string_13_r")

    add([(cx, 14) for cx in range(12, 17)], 0, -1, 0, "string_14_l")
    add([(cx, 14) for cx in range(17, 23)], -1, 1, 0, "string_14_r")

    add([(cx, 15) for cx in range(12, 17)], 0, -1, 0, "string_15_l")
    add([(cx, 15) for cx in range(17, 23)], -1, 1, 0, "string_15_r")

    add([(cx, 16) for cx in range(12, 17)], 0, -1, 0, "string_16_l")
    add([(cx, 16) for cx in range(17, 23)], -1, 1, 0, "string_16_r")

    # 9. PIANO RIM & CASE
    # Left bentside
    add([(7, cy) for cy in range(12, 16)], 0, -1, 0, "rim_l_top_out")
    add([(7, cy) for cy in range(16, 19)], 0, -1, 0, "rim_l_bot_out")
    add([(8, cy) for cy in range(12, 16)], 0, -1, 0, "rim_l_top_in")
    add([(8, cy) for cy in range(16, 19)], 0, -1, 0, "rim_l_bot_in")

    # Tail
    add([(cx, 10) for cx in range(9, 14)], 0, 0, -1, "tail_10")
    add([(cx, 11) for cx in range(9, 14)], 0, 0, -1, "tail_11")

    # Right spine
    add([(25, cy) for cy in range(9, 14)], 0, 1, 0, "rim_r_top_in")
    add([(25, cy) for cy in range(14, 19)], 0, 1, 0, "rim_r_bot_in")
    add([(26, cy) for cy in range(9, 14)], 0, 1, 0, "rim_r_top_out")
    add([(26, cy) for cy in range(14, 19)], 0, 1, 0, "rim_r_bot_out")

    # 10. PROP STICK
    add([(23, cy) for cy in range(8, 12)], 0, 0, -1, "prop_top")
    add([(23, cy) for cy in range(12, 16)], -1, 0, 1, "prop_bot")

    # 11. OPEN LID (Slopes up to top right)
    lid_cells = []
    for cx in range(8, 29):
        top_y = int(12 - (cx - 8) * 0.42)
        for cy in range(top_y, top_y + 3):
            if (cx, cy) not in occupied:
                lid_cells.append((cx, cy))

    by_y = defaultdict(list)
    for cx, cy in sorted(lid_cells):
        by_y[cy].append(cx)

    lid_idx = 0
    for cy, xs_in_row in sorted(by_y.items()):
        chunk = []
        for cx in xs_in_row:
            if not chunk or cx == chunk[-1] + 1:
                chunk.append(cx)
            else:
                if len(chunk) >= 2:
                    coords = [(x, cy) for x in chunk]
                    add(coords, 0 if cy < 7 else -1, 0, -1, f"lid_{lid_idx}")
                    lid_idx += 1
                chunk = [cx]
        if len(chunk) >= 2:
            coords = [(x, cy) for x in chunk]
            add(coords, 0 if cy < 7 else -1, 0, -1, f"lid_{lid_idx}")
            lid_idx += 1
        elif len(chunk) == 1:
            coords = [(chunk[0], cy)]
            add(coords, 0, 0, -1, f"lid_{lid_idx}")
            lid_idx += 1

    return arrows

arrows = build_grand_piano()
print(f"Total arrows: {len(arrows)}")
ok, steps, order = solve_canvas(arrows, XS, YS)
print(f"Canvas Solvable: {ok}, Cleared: {steps}/{len(arrows)}")
