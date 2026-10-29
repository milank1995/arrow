import sys
sys.path.append('scripts')
from build_curated_150_v4 import shapes, solve, prepare_level

w = [s for s in shapes if 'windmill' in s.get('ShapeName', '').lower()][0]
prep = prepare_level(w, 73, "Windmill", "Medium", "Shape Puzzle Levels")
print("Windmill arrows:", len(prep['Arrows']))
print("Solvable?", solve(prep))

# Check why it's not solvable:
xs = prep['XSize']
ys = prep['YSize']
arrows = prep['Arrows']
occupied = {}
heads, dirs, cells_list = [], [], []
for i, a in enumerate(arrows):
    idxs = a['Indices']
    hx = idxs[0] % xs
    hy = (ys - 1) - (idxs[0] // xs)
    heads.append((hx, hy))
    dirs.append((a['Dx'], -a['Dy']))
    cset = set((idx % xs, (ys - 1) - (idx // xs)) for idx in idxs)
    for c in cset: occupied[c] = i
    cells_list.append(cset)

active = list(range(len(heads)))
while active:
    prog = False
    for i in list(active):
        hx, hy = heads[i]
        dx, dy = dirs[i]
        cx, cy = hx + dx, hy + dy
        blocked = False
        while 0 <= cx < xs and 0 <= cy < ys:
            if (cx, cy) in occupied and occupied[(cx, cy)] != i:
                blocked = True
                break
            cx += dx
            cy += dy
        if not blocked:
            for c in cells_list[i]:
                if c in occupied and occupied[c] == i:
                    del occupied[c]
            active.remove(i)
            prog = True
            break
    if not prog:
        print(f"Stuck with {len(active)} active arrows remaining!")
        break
