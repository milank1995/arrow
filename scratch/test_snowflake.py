import sys
sys.stdout.reconfigure(encoding='utf-8')
from test_windmill_isolated import shapes, prepare_level, solve

snowflakes = [s for s in shapes if s.get('ShapeName') == '雪花']
print("Found snowflakes:", len(snowflakes))
for s in snowflakes:
    prep = prepare_level(s, 79, "Snowflake", "Medium", "Shape Puzzle Levels")
    print(f"Snowflake {len(s['Arrows'])} arrows: solvable? {solve(prep)}")
