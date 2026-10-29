import sys
sys.stdout.reconfigure(encoding='utf-8')
sys.path.append('scratch')
from test_accurate_matching import shapes, unity, prepare, solve

candidates = ['Hrose Final', 'Phoenix', 'Temple', 'Love', 'Scorpio (Scorpion)', 'Lucky Cat', 'Lion', 'Griffin V2pro']

print("Testing 185-210 arrow climax shapes solvability:")
for name in candidates:
    matches = [s for s in shapes if s.get('ShapeName') == name]
    if matches:
        s = matches[0]
        p = prepare(s, 150, name, "Super Hard", "Shape Puzzle Levels")
        print(f"'{name}' ({len(s['Arrows'])} arrows): solvable? {solve(p)}")
