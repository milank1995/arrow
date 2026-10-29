import sys
sys.path.append('scratch')
sys.stdout.reconfigure(encoding='utf-8')
from test_windmill_isolated import shapes, prepare_level, solve

# Wait, let's also test 'Cocktail' (22 arrows), 'Apple' (48 arrows), 'Flower Vase' (46 arrows)
for name in ['Flower Vase', 'Apple', 'Kitten Sharky', 'Goose', 'Fishing', 'Knight']:
    cands = [s for s in shapes if s.get('ShapeName') == name]
    for c in cands:
        p = prepare_level(c, 53, name, "Medium", "Shape Puzzle Levels")
        print(f"'{name}' ({len(c['Arrows'])} arr): solvable? {solve(p)}")
