import json
import sys
import glob
import os

sys.stdout.reconfigure(encoding='utf-8')

categories = [
    ('Existing 1K Levels', 'manifest_existing_1k.json'),
    ('New Unity Normal Levels', 'manifest_unity_normal.json'),
    ('Shape Puzzle Levels', 'manifest_shape_puzzles.json')
]

for name, path in categories:
    with open(path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    arrow_counts = [d['ArrowCount'] for d in data]
    min_arr = min(arrow_counts)
    max_arr = max(arrow_counts)
    min_levels = [d['Level'] for d in data if d['ArrowCount'] == min_arr]
    max_levels = [d['Level'] for d in data if d['ArrowCount'] == max_arr]
    large_puzzles = [d for d in data if 250 <= d['ArrowCount'] <= 450]
    
    print(f"==================================================")
    print(f"CATEGORY: {name}")
    print(f"==================================================")
    print(f"Total Levels:       {len(data):,}")
    print(f"Min Arrow Count:    {min_arr} arrows (Level #{min_levels[0]} to #{min_levels[-1]})")
    print(f"Max Arrow Count:    {max_arr} arrows (Level #{max_levels[0]} to #{max_levels[-1]})")
    print(f"250–450 Arrows:     {len(large_puzzles)} levels found")
    
    if large_puzzles:
        print("\nAll Levels with 250-450 arrows:")
        for p in large_puzzles:
            shape_str = f" | Shape: {p.get('ShapeName')}" if p.get('ShapeName') else ""
            print(f"  Level #{p['Level']:4d}: {p['ArrowCount']:3d} arrows (Grid {p['XSize']}x{p['YSize']}){shape_str} [Hash: {p.get('Hash')}]")
    else:
        print("  None. (All levels in this category have fewer than 250 arrows)")
    print()
