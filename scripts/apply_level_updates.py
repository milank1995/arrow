import json
from build_grand_piano import generate_piano, solve

def expand_indices(idxs, xs):
    if len(idxs) < 2: return list(idxs)
    full = [idxs[0]]
    for i in range(len(idxs)-1):
        x1, y1 = idxs[i] % xs, idxs[i] // xs
        x2, y2 = idxs[i+1] % xs, idxs[i+1] // xs
        step_x = 0 if x1 == x2 else (1 if x2 > x1 else -1)
        step_y = 0 if y1 == y2 else (1 if y2 > y1 else -1)
        cur_x, cur_y = x1, y1
        while (cur_x, cur_y) != (x2, y2):
            if cur_x != x2: cur_x += step_x
            elif cur_y != y2: cur_y += step_y
            full.append(cur_y * xs + cur_x)
    return full

# 1. Process Level 5 (User's Palm Tree)
with open('user_palm_tree.json', 'r', encoding='utf-8') as f:
    raw_palm = json.load(f)

xs_p = raw_palm['XSize']
ys_p = raw_palm['YSize']
expanded_palm_arrows = []
for a in raw_palm['Arrows']:
    expanded_palm_arrows.append({
        'Indices': expand_indices(a['Indices'], xs_p),
        'Dx': a['Dx'],
        'Dy': a['Dy'],
        'BendCount': a.get('BendCount', 0)
    })

level_5 = {
    'LevelNumber': 5,
    'XSize': xs_p,
    'YSize': ys_p,
    'MaxHearts': 3,
    'ShapeName': 'Palm Tree',
    'Difficulty': 'Hard',
    'ArrowCount': len(expanded_palm_arrows),
    'SourceCategory': 'Shape Puzzle Levels',
    'Arrows': expanded_palm_arrows
}

ok_p, steps_p, _ = solve(level_5)
assert ok_p, f"Level 5 failed to solve: {steps_p}/{len(expanded_palm_arrows)}"
print(f"Level 5 (Palm Tree) validated: {len(expanded_palm_arrows)} arrows, Solvable: {ok_p}")

with open('arrow_native/app/src/main/assets/levels/Level_5.json', 'w', encoding='utf-8') as f:
    json.dump(level_5, f, indent=2)

# 2. Process Level 33 (Grand Piano)
raw_piano = generate_piano()
level_33 = {
    'LevelNumber': 33,
    'XSize': raw_piano['XSize'],
    'YSize': raw_piano['YSize'],
    'MaxHearts': 3,
    'ShapeName': 'Grand Piano',
    'Difficulty': 'Hard',
    'ArrowCount': len(raw_piano['Arrows']),
    'SourceCategory': 'Shape Puzzle Levels',
    'Arrows': raw_piano['Arrows']
}

ok_pi, steps_pi, _ = solve(level_33)
assert ok_pi, f"Level 33 failed to solve: {steps_pi}/{len(raw_piano['Arrows'])}"
print(f"Level 33 (Grand Piano) validated: {len(raw_piano['Arrows'])} arrows, Solvable: {ok_pi}")

with open('arrow_native/app/src/main/assets/levels/Level_33.json', 'w', encoding='utf-8') as f:
    json.dump(level_33, f, indent=2)

# 3. Update manifest.json
manifest_path = 'arrow_native/app/src/main/assets/levels/manifest.json'
with open(manifest_path, 'r', encoding='utf-8') as f:
    manifest = json.load(f)

for item in manifest:
    if item['LevelNumber'] == 5:
        item['ArrowCount'] = level_5['ArrowCount']
        item['Difficulty'] = level_5['Difficulty']
        item['ShapeName'] = level_5['ShapeName']
        item['XSize'] = level_5['XSize']
        item['YSize'] = level_5['YSize']
    elif item['LevelNumber'] == 33:
        item['ArrowCount'] = level_33['ArrowCount']
        item['Difficulty'] = level_33['Difficulty']
        item['ShapeName'] = level_33['ShapeName']
        item['XSize'] = level_33['XSize']
        item['YSize'] = level_33['YSize']

with open(manifest_path, 'w', encoding='utf-8') as f:
    json.dump(manifest, f, indent=2)
print("Updated manifest.json successfully!")
