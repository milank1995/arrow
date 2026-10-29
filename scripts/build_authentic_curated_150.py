import subprocess
import gzip
import json
import os
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'
OUT_DIR = r'arrow_native/app/src/main/assets/levels'
os.makedirs(OUT_DIR, exist_ok=True)

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

print("Loading collections from backup branch...")
shapes = decrypt_blob('df660515e10e0d3fb09bc7374ac2e40025da06de')
one_k = decrypt_blob('24d00416f1df3786d07da493d94eb3e53df74701')
unity_n = decrypt_blob('62f9bd8a0a265e9946eaab39b69d90e6aab385b0')
large_p = decrypt_blob('e7b51b086143380b756ef7f35b7e7b612962ec50')

print(f"Loaded {len(shapes)} shapes, {len(one_k)} 1K, {len(unity_n)} unity normal, {len(large_p)} large.")

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
        idxs = a['Indices'] # already expanded
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

def prepare_level(raw_level, level_num, shape_name=None, diff="Medium", source_category=""):
    xs = raw_level['XSize']
    ys = raw_level['YSize']
    arrows_out = []
    for a in raw_level['Arrows']:
        expanded = expand_indices(a['Indices'], xs)
        arrows_out.append({
            'Indices': expanded,
            'Dx': a['Dx'],
            'Dy': a['Dy'],
            'BendCount': a.get('BendCount', 0)
        })
    level_dict = {
        'LevelNumber': level_num,
        'XSize': xs,
        'YSize': ys,
        'MaxHearts': raw_level.get('MaxHearts', 3),
        'ShapeName': shape_name,
        'Difficulty': diff,
        'ArrowCount': len(arrows_out),
        'SourceCategory': source_category,
        'Arrows': arrows_out
    }
    return level_dict

# 1. Prepare target shapes pool
shape_pool = {}
for s in shapes + large_p:
    n = s.get('ShapeName', '')
    if n:
        shape_pool[(n, len(s['Arrows']))] = s

shape_targets = [
    ('Palm Tree', 23),
    ('Balloon', 33),
    ('Umbrella Cat', 33),
    ('Jellyfish', 35),
    ('Flower Vase', 46),
    ('Apple', 48),
    ('Kitten Sharky', 50),
    ('Robot', 52),
    ('Winecat', 54),
    ('Black Cat', 57),
    ('Eiffel Tower', 58),
    ('Magnolia2', 55),
    ('Sunflower', 67),
    ('Conch', 74),
    ('Sitting Cat', 78),
    ('Panda', 81),
    ('Windowview', 86),
    ('Butterfly', 90),
    ('Swan', 99),
    ('Winterbird2', 110),
    ('Tropical Waterfall Elephant', 115),
    ('Whale', 136),
    ('Strawberry', 139),
    ('Lion', 141),
    ('Cancer (Crab)', 156),
    ('Chameleon', 167),
    ('Stargazer Cat', 170),
    ('Lucky Cat', 186),
    ('Hrose Final', 190),
    ('The Thinker', 262)
]

# Clean display names for shapes
SHAPE_DISPLAY_NAMES = {
    'Magnolia2': 'Magnolia Flower',
    'Winterbird2': 'Winter Bird',
    'Cancer (Crab)': 'Crab',
    'Hrose Final': 'Rose',
    'Winecat': 'Wine Cat'
}

# 2. Index normal candidate levels
one_k_by_arr = defaultdict(list)
for idx, lvl in enumerate(one_k):
    # Skip levels that are shapes
    if not lvl.get('ShapeName'):
        cnt = len(lvl['Arrows'])
        one_k_by_arr[cnt].append((idx, lvl))

unity_n_by_arr = defaultdict(list)
for idx, lvl in enumerate(unity_n):
    if not lvl.get('ShapeName'):
        cnt = len(lvl['Arrows'])
        unity_n_by_arr[cnt].append((idx, lvl))

used_hashes = set()
final_150 = []

def get_signature(lvl):
    all_i = tuple(sorted(i for a in lvl['Arrows'] for i in a['Indices']))
    return (lvl['XSize'], lvl['YSize'], len(lvl['Arrows']), all_i[:10])

# Specific targets for the 120 normal levels
def target_arrow_for_level(lvl_num):
    # Smooth progression curve
    if lvl_num == 1: return 4
    if lvl_num == 2: return 6
    if lvl_num == 3: return 8
    if lvl_num == 4: return 12
    
    # Block index 0 to 29
    blk = lvl_num // 5
    pos = lvl_num % 5 # 1, 2, 3, 4
    
    # Base arrows for block
    base = 16 + blk * 5.2
    target = int(base + (pos - 1) * 1.5)
    return max(4, min(230, target))

for lvl_num in range(1, 151):
    if lvl_num % 5 == 0:
        # SHAPE LEVEL
        shape_idx = (lvl_num // 5) - 1
        name, cnt = shape_targets[shape_idx]
        raw = shape_pool.get((name, cnt))
        if not raw:
            raw = [s for (n, c), s in shape_pool.items() if n == name][0]
        
        disp_name = SHAPE_DISPLAY_NAMES.get(name, name)
        diff = "Super Hard" if lvl_num > 100 or len(raw['Arrows']) >= 130 else ("Hard" if lvl_num > 40 or len(raw['Arrows']) >= 60 else "Medium")
        source_cat = "Large Puzzles" if len(raw['Arrows']) >= 250 else "Shape Puzzle Levels"
        
        prep = prepare_level(raw, lvl_num, shape_name=disp_name, diff=diff, source_category=source_cat)
        assert solve(prep), f"Shape level {lvl_num} ({disp_name}) failed solvability check!"
        used_hashes.add(get_signature(prep))
        final_150.append(prep)
    else:
        # NORMAL RECTANGLE LEVEL
        target_arr = target_arrow_for_level(lvl_num)
        is_unity = ((lvl_num % 5) in (1, 3)) # alternate Unity and 1K
        
        # Level 1 explicit request: Unity Normal L1
        if lvl_num == 1:
            raw = unity_n[0]
            prep = prepare_level(raw, 1, shape_name=None, diff="Easy", source_category="New Unity Normal Levels")
            assert solve(prep), "L1 failed!"
            used_hashes.add(get_signature(prep))
            final_150.append(prep)
            continue
            
        # Levels 2-4 explicit request: Existing 1K L2, L3, L4
        if lvl_num in (2, 3, 4):
            raw = one_k[lvl_num - 1]
            prep = prepare_level(raw, lvl_num, shape_name=None, diff="Easy" if lvl_num <= 3 else "Medium", source_category="Existing 1K Levels")
            assert solve(prep), f"L{lvl_num} failed!"
            used_hashes.add(get_signature(prep))
            final_150.append(prep)
            continue
            
        primary_pool = unity_n_by_arr if is_unity else one_k_by_arr
        fallback_pool = one_k_by_arr if is_unity else unity_n_by_arr
        source_name = "New Unity Normal Levels" if is_unity else "Existing 1K Levels"
        fallback_source = "Existing 1K Levels" if is_unity else "New Unity Normal Levels"
        
        chosen = None
        chosen_source = source_name
        
        for delta in range(0, 15):
            for candidate_arr in (target_arr + delta, target_arr - delta):
                if candidate_arr in primary_pool:
                    for idx, candidate in primary_pool[candidate_arr]:
                        prep = prepare_level(candidate, lvl_num, shape_name=None, diff="Hard" if candidate_arr >= 65 else "Medium", source_category=source_name)
                        sig = get_signature(prep)
                        if sig not in used_hashes and solve(prep):
                            chosen = prep
                            used_hashes.add(sig)
                            break
                if chosen: break
            if chosen: break
            
        if not chosen:
            # Try fallback pool
            for delta in range(0, 20):
                for candidate_arr in (target_arr + delta, target_arr - delta):
                    if candidate_arr in fallback_pool:
                        for idx, candidate in fallback_pool[candidate_arr]:
                            prep = prepare_level(candidate, lvl_num, shape_name=None, diff="Hard" if candidate_arr >= 65 else "Medium", source_category=fallback_source)
                            sig = get_signature(prep)
                            if sig not in used_hashes and solve(prep):
                                chosen = prep
                                used_hashes.add(sig)
                                break
                    if chosen: break
                if chosen: break
                
        assert chosen is not None, f"Could not find solvable level for L{lvl_num} targeting {target_arr} arrows!"
        final_150.append(chosen)

print(f"Successfully generated all {len(final_150)} curated levels!")

# Write individual files and manifest
manifest_list = []
for lvl in final_150:
    lvl_num = lvl['LevelNumber']
    file_path = os.path.join(OUT_DIR, f"Level_{lvl_num}.json")
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(lvl, f, separators=(',', ':'))
    manifest_list.append({
        'LevelNumber': lvl_num,
        'ArrowCount': lvl['ArrowCount'],
        'Difficulty': lvl['Difficulty'],
        'ShapeName': lvl['ShapeName'],
        'IsShape': lvl['ShapeName'] is not None,
        'SourceCategory': lvl['SourceCategory'],
        'XSize': lvl['XSize'],
        'YSize': lvl['YSize']
    })

manifest_path = os.path.join(OUT_DIR, 'manifest.json')
with open(manifest_path, 'w', encoding='utf-8') as f:
    json.dump(manifest_list, f, indent=2)

print(f"Wrote Level_1.json to Level_150.json and manifest.json to {OUT_DIR}.")
