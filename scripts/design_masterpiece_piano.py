import json
from PIL import Image, ImageDraw
from collections import defaultdict

# Let's explore canvas size 42 x 46
XS = 42
YS = 46

def solve_canvas(arrows, xs, ys):
    occupied = {}
    heads, dirs, cells_list = [], [], []
    for i, a in enumerate(arrows):
        coords = a['canvas_coords']
        cdx = a['cdx']
        cdy = a['cdy']
        heads.append(coords[0])
        dirs.append((cdx, cdy))
        cset = set(coords)
        for c in cset: occupied[c] = i
        cells_list.append(cset)
        
    active = list(range(len(heads)))
    solve_order = []
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
                solve_order.append(i)
                prog = True
                break
        if not prog:
            return False, len(solve_order), solve_order
    return True, len(solve_order), solve_order

print("Masterpiece designer ready.")
