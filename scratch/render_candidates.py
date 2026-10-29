from test_accurate_matching import shapes

def render_shape(raw):
    xs = raw['XSize']
    ys = raw['YSize']
    grid = [[' ' for _ in range(xs)] for _ in range(ys)]
    for a in raw['Arrows']:
        for idx in a['Indices']:
            grid[idx // xs][idx % xs] = '#'
    lines = ["".join(row) for row in grid]
    # Trim empty rows/cols
    return "\n".join(r for r in lines if '#' in r)

for s in shapes:
    name = s.get('ShapeName', '')
    if name in ['对称星', 'Goose', 'Apple', 'Kitten Sharky', 'Winecat', 'Clock', 'Spin']:
        print(f"=== Shape: {name} ({len(s['Arrows'])} arrows) ===")
        print(render_shape(s)[:500])
        print("...")
