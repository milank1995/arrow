import json

with open('arrow_native/app/src/main/assets/levels/Level_35.json') as f:
    lvl = json.load(f)

xs = lvl['XSize']
ys = lvl['YSize']
arrows = lvl['Arrows']

# Check all arrow heads and their steps
for i, a in enumerate(arrows):
    idxs = a['Indices']
    hx, hy = idxs[0] % xs, (ys - 1) - (idxs[0] // xs)
    dx = a['Dx']
    dy = -a['Dy'] # canvas dy
    # check where it points
    first_step = (hx + dx, hy + dy)
    # is first_step inside grid or outside?
    outside = not (0 <= first_step[0] < xs and 0 <= first_step[1] < ys)
    # stem neck
    if len(idxs) > 1:
        nx, ny = idxs[1] % xs, (ys - 1) - (idxs[1] // xs)
        neck_to_head = (hx - nx, hy - ny)
        if neck_to_head != (dx, dy):
            print(f"Arrow {i}: direction mismatch! neck_to_head={neck_to_head}, (dx,dy)={(dx,dy)}")
    else:
        print(f"Arrow {i}: only 1 index!")

print("Level 35 check completed.")
