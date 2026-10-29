import json
import random
from collections import defaultdict

# Forward solver definition
def solve(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows: return False, 0
    occupied = {}
    heads, dirs, cells_list = [], [], []
    for i, a in enumerate(arrows):
        idxs = a['Indices']
        if not idxs: continue
        hx = idxs[0] % xs
        hy = (ys - 1) - (idxs[0] // xs)
        cdx = a['Dx']
        cdy = -a['Dy']
        heads.append((hx, hy))
        dirs.append((cdx, cdy))
        cset = set((idx % xs, (ys - 1) - (idx // xs)) for idx in idxs)
        for c in cset: occupied[c] = i
        cells_list.append(cset)
        
    active = list(range(len(heads)))
    steps = 0
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
                steps += 1
                break
        if not prog:
            return False, steps
    return True, steps

print("Solver ready")
