import json

with open('scratch/lock_user_input.json', 'r', encoding='utf-8') as f:
    lvl = json.load(f)

xs = lvl['XSize']
ys = lvl['YSize']
arrows = lvl['Arrows']
shape_name = lvl.get('ShapeName')
max_hearts = lvl.get('MaxHearts')
print(f"Level: XSize={xs}, YSize={ys}, Arrows={len(arrows)}, ShapeName={shape_name}, MaxHearts={max_hearts}")

all_indices = [idx for a in arrows for idx in a['Indices']]
min_x = min(idx % xs for idx in all_indices)
max_x = max(idx % xs for idx in all_indices)
min_y = min(idx // xs for idx in all_indices)
max_y = max(idx // xs for idx in all_indices)
print(f"Bounding box: X=[{min_x}, {max_x}] (span {max_x - min_x + 1}), Y=[{min_y}, {max_y}] (span {max_y - min_y + 1})")

# Check overlaps
cell_count = {}
overlaps = []
for i, a in enumerate(arrows):
    for idx in a['Indices']:
        cell = (idx % xs, idx // xs)
        if cell in cell_count:
            overlaps.append((cell, cell_count[cell], i))
        cell_count[cell] = i
print(f"Total overlapping cells: {len(overlaps)}")

# Check solvability in both coordinate systems (Unity raw dy vs Canvas -dy)
def solve_canvas(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
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
    steps = 0
    order = []
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
                order.append(i)
                prog = True
                steps += 1
                break
        if not prog:
            print(f"Canvas coordinate solver: DEADLOCK with {len(active)} arrows remaining after {steps} steps!")
            return False, order
    print(f"Canvas coordinate solver: SOLVABLE! All {len(arrows)} arrows cleared in {steps} steps.")
    return True, order

solvable, order = solve_canvas(lvl)
