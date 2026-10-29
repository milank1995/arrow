import json
import glob
from level_transformer import transform_level

def solve_level(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows: return False
    occupied = {}
    heads, dirs, cells_list = [], [], []
    for i, a in enumerate(arrows):
        idxs = a['Indices']
        if not idxs or len(idxs) < 2: continue
        # Unity: index = Y * XSize + X. Canvas: (idx % xs, (ys - 1) - idx // xs)
        hx = idxs[0] % xs
        hy = (ys - 1) - (idxs[0] // xs)
        heads.append((hx, hy))
        dirs.append((a['Dx'], -a['Dy']))
        cset = set((idx % xs, (ys - 1) - (idx // xs)) for idx in idxs)
        for c in cset: occupied[c] = i
        cells_list.append(cset)
    active = list(range(len(heads)))
    if not active: return False
    while active:
        prog = False
        for i in list(active):
            hx, hy = heads[i]
            dx, dy = dirs[i]
            cur_x, cur_y = hx + dx, hy + dy
            blocked = False
            while 0 <= cur_x < xs and 0 <= cur_y < ys:
                if (cur_x, cur_y) in occupied and occupied[(cur_x, cur_y)] != i:
                    blocked = True
                    break
                cur_x += dx
                cur_y += dy
            if not blocked:
                for c in cells_list[i]:
                    if c in occupied and occupied[c] == i:
                        del occupied[c]
                active.remove(i)
                prog = True
                break
        if not prog: return False
    return True

# Test on 10 levels from scratch/old_levels
files = glob.glob('scratch/old_levels/arrow_native/app/src/main/assets/levels/Level_*.json')[:10]
all_pass = True
for f in files:
    with open(f, 'r', encoding='utf-8') as fp:
        raw = json.load(fp)
    if not solve_level(raw):
        continue
    transformed = transform_level(raw, seed=42)
    s = solve_level(transformed)
    print(f"File {f}: original size {raw['XSize']}x{raw['YSize']}, transformed size {transformed['XSize']}x{transformed['YSize']}, Solvable: {s}")
    if not s:
        all_pass = False

print("Test result all pass:", all_pass)
