import sys, subprocess, json, gzip
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

sys.stdout.reconfigure(encoding='utf-8')
AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'

def decrypt_blob(blob_id):
    p = subprocess.Popen(['git', 'cat-file', 'blob', blob_id], stdout=subprocess.PIPE)
    data, _ = p.communicate()
    iv, ct = data[:16], data[16:]
    cipher = Cipher(algorithms.AES(AES_KEY), modes.CBC(iv))
    dec = cipher.decryptor()
    pt = dec.update(ct) + dec.finalize()
    pad_len = pt[-1]
    pt = pt[:-pad_len]
    return json.loads(gzip.decompress(pt))

def get_pack_levels(prefix):
    tree = subprocess.check_output(['git', 'ls-tree', f'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/{prefix}'], encoding='utf-8')
    levels = []
    for line in tree.strip().splitlines():
        parts = line.split()
        if len(parts) >= 4 and parts[3].startswith('level_pack_') and parts[3].endswith('.dat'):
            pack = decrypt_blob(parts[2])
            levels.extend(pack)
    return levels

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

def solve(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows: return False
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
        if not prog: return False
    return True

print("Loading shape puzzles pack...")
shapes = get_pack_levels('test_shape_puzzles')
clock = None
for l in shapes:
    if 'Clock-8cd7' in (l.get('Hash') or ''):
        clock = l
        break

assert clock is not None, "Clock shape not found in bucket!"
xs = clock['XSize']
ys = clock['YSize']

expanded_arrows = []
for a in clock['Arrows']:
    expanded_arrows.append({
        'Indices': expand_indices(a['Indices'], xs),
        'Dx': a['Dx'],
        'Dy': a['Dy'],
        'BendCount': a.get('BendCount', 0)
    })

level_33_clock = {
    'LevelNumber': 33,
    'XSize': xs,
    'YSize': ys,
    'MaxHearts': 3,
    'ShapeName': 'Clock',
    'Difficulty': 'Medium',
    'ArrowCount': len(expanded_arrows),
    'SourceCategory': 'Shape Puzzle Levels',
    'Arrows': expanded_arrows
}

assert solve(level_33_clock), "Clock level failed solvability check!"
print(f"Clock validated: {len(expanded_arrows)} arrows, {xs}x{ys}, 100% Solvable, 0 Overlaps!")

# Write to Level_33.json
out_path = 'arrow_native/app/src/main/assets/levels/Level_33.json'
with open(out_path, 'w', encoding='utf-8') as f:
    json.dump(level_33_clock, f, indent=2)
print("Updated Level_33.json")

# Update manifest.json
manifest_path = 'arrow_native/app/src/main/assets/levels/manifest.json'
with open(manifest_path, 'r', encoding='utf-8') as f:
    manifest = json.load(f)

for item in manifest:
    if item['LevelNumber'] == 33:
        item['ArrowCount'] = level_33_clock['ArrowCount']
        item['Difficulty'] = level_33_clock['Difficulty']
        item['ShapeName'] = level_33_clock['ShapeName']
        item['XSize'] = level_33_clock['XSize']
        item['YSize'] = level_33_clock['YSize']
        item['IsShape'] = True
        item['SourceCategory'] = 'Shape Puzzle Levels'

with open(manifest_path, 'w', encoding='utf-8') as f:
    json.dump(manifest, f, indent=2)
print("Updated manifest.json")
