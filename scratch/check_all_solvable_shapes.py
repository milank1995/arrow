import sys
sys.stdout.reconfigure(encoding='utf-8')
from test_windmill_isolated import shapes, prepare_level, solve

print("Testing solvability of named shapes in shapes:")
solvable_named = []
unsolvable_named = []

for s in shapes:
    name = s.get('ShapeName', '')
    if name and not name.startswith('Shape #'):
        p = prepare_level(s, 1, name, "Medium", "Shape Puzzle Levels")
        is_solv = solve(p)
        if is_solv:
            solvable_named.append((name, len(p['Arrows'])))
        else:
            unsolvable_named.append((name, len(p['Arrows'])))

print(f"Total solvable named shapes: {len(solvable_named)}")
print(f"Total unsolvable named shapes: {len(unsolvable_named)}")
print("Sample unsolvable:", unsolvable_named[:10])

# Shapes around 60-90 arrows that ARE solvable:
print("\nSolvable named shapes between 60 and 95 arrows:")
for n, a in sorted(solvable_named, key=lambda x: x[1]):
    if 60 <= a <= 95:
        print(f"  '{n}': {a} arrows")
