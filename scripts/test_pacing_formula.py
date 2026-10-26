import json, gzip, os, glob
from collections import defaultdict

# Test how the 200 chapters of 10 levels will be generated
chapters = 200 # 200 * 10 = 2000 levels

print("=== Pacing Wave Formula for 2,000 Levels (200 Chapters) ===")
# Base arrow count ramps smoothly from 4 (L1) to ~120 (L2000)
# Inside each chapter of 10 levels:
# offsets from chapter_base:
# L1: base - 2
# L2: base - 1
# L3: base + 1
# L4: base + 3
# L5 (Shape): base + 4
# L6: base (relief)
# L7: base + 2
# L8: base + 5 (challenging)
# L9: base + 7 (challenging)
# L10 (Shape): base + 9 (climax)

sample_chapters = [1, 2, 3, 5, 10, 20, 50, 100, 150, 200]
for c in sample_chapters:
    t = (c - 1) / 199.0
    # Non-linear curve: gentle start, steady mid-game, challenging endgame
    base_arr = 6 + int(105 * (t ** 0.85))
    start_lvl = (c - 1) * 10 + 1
    end_lvl = c * 10
    l5_target = base_arr + 4
    l10_target = base_arr + 9
    print(f"Chapter {c:3d} (Levels {start_lvl:4d}-{end_lvl:4d}): Base={base_arr:3d} | L{start_lvl}: {base_arr-2:3d} arr -> L{start_lvl+4} (Shape): {l5_target:3d} arr -> L{end_lvl} (Shape Climax): {l10_target:3d} arr")
