import os
import json

def render_ascii(xs, ys, arrows, max_w=60, max_h=30):
    grid = [[' ' for _ in range(xs)] for _ in range(ys)]
    for a in arrows:
        for idx in a.get('Indices', []):
            x = idx % xs
            y = idx // xs
            if 0 <= y < ys and 0 <= x < xs:
                grid[y][x] = '#'
                
    # Downsample if too large
    scale_x = max(1, xs // max_w + 1)
    scale_y = max(1, ys // max_h + 1)
    
    out_ys = (ys + scale_y - 1) // scale_y
    out_xs = (xs + scale_x - 1) // scale_x
    out_grid = [[' ' for _ in range(out_xs)] for _ in range(out_ys)]
    
    for y in range(ys):
        for x in range(xs):
            if grid[y][x] == '#':
                out_grid[y // scale_y][x // scale_x] = '#'
                
    lines = [''.join(row).rstrip() for row in out_grid]
    # Trim empty top/bottom lines
    while lines and not lines[0].strip():
        lines.pop(0)
    while lines and not lines[-1].strip():
        lines.pop()
    return '\n'.join(lines)

# Render the 6 shape levels found in new_levels
new_dir = 'arrow_native/app/src/main/assets/new_levels'
for lvl_num in [5, 10, 15, 26, 30, 76]:
    fpath = os.path.join(new_dir, f'NewLevel_{lvl_num}.json')
    if os.path.exists(fpath):
        with open(fpath) as f:
            d = json.load(f)
            print(f"\n================ NewLevel_{lvl_num} ({d['XSize']}x{d['YSize']}, {len(d['Arrows'])} arrows) ================")
            print(render_ascii(d['XSize'], d['YSize'], d['Arrows']))
