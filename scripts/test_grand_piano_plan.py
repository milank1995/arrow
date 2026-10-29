import json
from collections import defaultdict
from render_helper import render_arrows_to_image

# Candidate 1: Classic Concert Grand Piano (Top-Down / 3/4 Plan View)
# Canvas: 38 x 44
XS1, YS1 = 38, 44

def build_grand_piano_plan():
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

    # --- 1. KEYBOARD AT BOTTOM (cy=34..38, cx=8..29) ---
    # White keys and Black keys
    # 22 keys!
    for cx in range(8, 30):
        # Black keys pattern: 2 black keys, 1 skip, 3 black keys, 1 skip...
        # Let's place black keys at: 9, 11 (group of 2), 14, 16, 18 (group of 3), 21, 23 (group of 2), 26, 28 (group of 3)
        if cx in [9, 11, 14, 16, 18, 21, 23, 26, 28]:
            # Black key: cy=34..36 (3 cells, recessed)
            add([(cx, cy) for cy in range(34, 37)], -1, 0, 1, f"black_key_{cx}")
        else:
            # White key: cy=34..38 (5 cells, full length extending to bottom)
            add([(cx, cy) for cy in range(34, 39)], -1, 0, 1, f"white_key_{cx}")

    # Keyfront edge / lip (cy=39):
    # Left cheek & right cheek of keyboard
    add([(cx, 39) for cx in range(7, 12)], 0, -1, 0, "keybed_lip_l")
    add([(cx, 39) for cx in range(26, 31)], -1, 1, 0, "keybed_lip_r")

    # --- 2. FALLBOARD / KEYBED SILL (cy=33, cx=7..30) ---
    add([(cx, 33) for cx in range(7, 13)], 0, -1, 0, "fallboard_l")
    add([(cx, 33) for cx in range(13, 19)], 0, -1, 0, "fallboard_ml")
    add([(cx, 33) for cx in range(19, 25)], -1, 1, 0, "fallboard_mr")
    add([(cx, 33) for cx in range(25, 31)], -1, 1, 0, "fallboard_r")

    # --- 3. MUSIC STAND & SHEET MUSIC (cy=30..32, cx=14..24) ---
    # Sheet music pages
    add([(cx, 30) for cx in range(15, 19)], 0, 0, -1, "sheet_music_l")
    add([(cx, 30) for cx in range(20, 24)], -1, 0, -1, "sheet_music_r")
    # Music desk shelf
    add([(cx, 31) for cx in range(14, 19)], 0, -1, 0, "desk_shelf_l")
    add([(cx, 31) for cx in range(19, 25)], -1, 1, 0, "desk_shelf_r")
    # Stand legs
    add([(14, 32), (15, 32)], 0, -1, 0, "desk_leg_l")
    add([(23, 32), (24, 32)], -1, 1, 0, "desk_leg_r")

    # --- 4. STRAIGHT SPINE (RIGHT RIM) (cx=30, cy=7..32) ---
    # Right rim is straight as an arrow
    add([(30, cy) for cy in range(7, 13)], 0, 1, 0, "spine_1")
    add([(30, cy) for cy in range(13, 19)], 0, 1, 0, "spine_2")
    add([(30, cy) for cy in range(19, 26)], 0, 1, 0, "spine_3")
    add([(30, cy) for cy in range(26, 33)], 0, 1, 0, "spine_4")

    # Inner spine rim (cx=29, cy=8..29)
    add([(29, cy) for cy in range(8, 14)], 0, 1, 0, "spine_in_1")
    add([(29, cy) for cy in range(14, 21)], 0, 1, 0, "spine_in_2")
    add([(29, cy) for cy in range(21, 29)], 0, 1, 0, "spine_in_3")

    # --- 5. TAIL CURVE AT REAR (TOP) (cy=5..7, cx=10..30) ---
    # Rounded rear tail
    add([(cx, 5) for cx in range(15, 23)], 0, 0, -1, "tail_top_1")
    add([(cx, 5) for cx in range(23, 29)], -1, 0, -1, "tail_top_2")
    add([(cx, 6) for cx in range(13, 20)], 0, 0, -1, "tail_top_in1")
    add([(cx, 6) for cx in range(20, 28)], -1, 0, -1, "tail_top_in2")

    # --- 6. GRACEFUL S-CURVE BENTSIDE (LEFT RIM) ---
    # Starts at tail (cx=12..15, cy=6), curves down-left, tucks inward around cy=18..22, flares out around cy=28..33!
    # Upper curve (tail to upper belly):
    add([(14, 7), (13, 7), (12, 8), (11, 8)], 0, -1, 0, "curve_upper_1")
    add([(10, 9), (9, 9), (8, 10), (8, 11)], 0, -1, 0, "curve_upper_2")
    add([(7, 12), (7, 13), (7, 14), (7, 15)], 0, -1, 0, "curve_belly_1")
    add([(7, 16), (7, 17), (8, 18), (8, 19)], 0, -1, 0, "curve_belly_2")

    # Inward hollow curve (waist / bentside):
    add([(9, 20), (10, 21), (10, 22), (10, 23)], 0, -1, 0, "curve_waist_1")
    add([(10, 24), (9, 25), (9, 26), (8, 27)], 0, -1, 0, "curve_waist_2")

    # Flare out to keyboard cheek:
    add([(8, 28), (7, 29), (7, 30), (7, 31), (7, 32)], 0, -1, 0, "curve_cheek")

    # Inner bentside rim lining (adds depth & thickness to the rim!):
    add([(12, 9), (11, 10), (10, 11), (9, 12)], 0, -1, 0, "rim_inner_1")
    add([(9, 13), (9, 14), (9, 15), (9, 16)], 0, -1, 0, "rim_inner_2")
    add([(9, 17), (10, 18), (11, 19), (11, 20)], 0, -1, 0, "rim_inner_3")
    add([(11, 24), (10, 25), (10, 26), (9, 27)], 0, -1, 0, "rim_inner_4")
    add([(8, 29), (8, 30), (8, 31), (8, 32)], 0, -1, 0, "rim_inner_5")

    # --- 7. CAST-IRON HARP & SOUNDBOARD STRINGS (Inside the body) ---
    # Soundboard strings running horizontally & diagonally inside the rim!
    # String row 10..12 (treble strings, short, high pitch near tail)
    add([(cx, 8) for cx in range(16, 22)], 0, 0, -1, "string_hi_1")
    add([(cx, 8) for cx in range(22, 28)], -1, 1, 0, "string_hi_2")

    add([(cx, 10) for cx in range(14, 21)], 0, 0, -1, "string_10_l")
    add([(cx, 10) for cx in range(21, 28)], -1, 1, 0, "string_10_r")

    add([(cx, 12) for cx in range(12, 20)], 0, -1, 0, "string_12_l")
    add([(cx, 12) for cx in range(20, 28)], -1, 1, 0, "string_12_r")

    add([(cx, 14) for cx in range(11, 19)], 0, -1, 0, "string_14_l")
    add([(cx, 14) for cx in range(19, 28)], -1, 1, 0, "string_14_r")

    add([(cx, 16) for cx in range(11, 19)], 0, -1, 0, "string_16_l")
    add([(cx, 16) for cx in range(19, 28)], -1, 1, 0, "string_16_r")

    # Cast iron plate curved struts (dividing bass & treble sections):
    add([(cx, 18) for cx in range(12, 19)], 0, -1, 0, "harp_strut_1")
    add([(cx, 18) for cx in range(19, 28)], -1, 1, 0, "harp_strut_2")

    add([(cx, 20) for cx in range(13, 20)], 0, -1, 0, "string_20_l")
    add([(cx, 20) for cx in range(20, 28)], -1, 1, 0, "string_20_r")

    add([(cx, 22) for cx in range(13, 20)], 0, -1, 0, "string_22_l")
    add([(cx, 22) for cx in range(20, 28)], -1, 1, 0, "string_22_r")

    add([(cx, 24) for cx in range(13, 20)], 0, -1, 0, "string_24_l")
    add([(cx, 24) for cx in range(20, 28)], -1, 1, 0, "string_24_r")

    add([(cx, 26) for cx in range(13, 20)], 0, -1, 0, "string_26_l")
    add([(cx, 26) for cx in range(20, 28)], -1, 1, 0, "string_26_r")

    add([(cx, 28) for cx in range(13, 20)], 0, -1, 0, "string_28_l")
    add([(cx, 28) for cx in range(20, 28)], -1, 1, 0, "string_28_r")

    # --- 8. OPEN LID PROP & WING FLARE (cx=31..36, cy=4..24) ---
    # The open lid flares out to the right side of the spine!
    # Lid edge sloping out
    add([(cx, 4) for cx in range(28, 34)], 0, 0, -1, "lid_top_edge")
    add([(33, cy) for cy in range(5, 11)], 0, 1, 0, "lid_rim_1")
    add([(34, cy) for cy in range(7, 14)], 0, 1, 0, "lid_rim_2")
    add([(35, cy) for cy in range(10, 18)], 0, 1, 0, "lid_rim_3")
    add([(34, cy) for cy in range(16, 23)], 0, 1, 0, "lid_rim_4")
    # Lid prop stick
    add([(32, cy) for cy in range(12, 19)], 0, 1, 0, "lid_prop_stick")

    # Lid panel fills
    add([(cx, 6) for cx in range(29, 33)], 0, 0, -1, "lid_panel_1")
    add([(cx, 8) for cx in range(31, 33)], 0, 1, 0, "lid_panel_2")
    add([(cx, 11) for cx in range(31, 33)], 0, 1, 0, "lid_panel_3")
    add([(31, 14)], 0, 1, 0, "lid_panel_4")

    # --- 9. PIANO BENCH (In front of keyboard, cy=40..43, cx=13..24) ---
    add([(cx, 41) for cx in range(13, 19)], 0, 0, 1, "bench_top_l")
    add([(cx, 41) for cx in range(19, 25)], -1, 0, 1, "bench_top_r")
    add([(cx, 42) for cx in range(13, 19)], 0, -1, 0, "bench_bot_l")
    add([(cx, 42) for cx in range(19, 25)], -1, 1, 0, "bench_bot_r")
    # Bench legs
    add([(13, 43)], 0, 0, 1, "bench_leg_1")
    add([(24, 43)], 0, 0, 1, "bench_leg_2")

    return arrows

arrows1 = build_grand_piano_plan()
print(f"Grand Piano Plan: {len(arrows1)} arrows")
from design_perfect_piano import solve_canvas
ok1, steps1, order1 = solve_canvas(arrows1, XS1, YS1)
print(f"Plan Solvable: {ok1}, Steps: {steps1}/{len(arrows1)}")
render_arrows_to_image(arrows1, XS1, YS1, "grand_piano_plan_preview.png")
