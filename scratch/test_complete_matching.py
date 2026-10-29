import subprocess
import json
import gzip
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

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

print("Loading packs...")
shapes = get_pack_levels('test_shape_puzzles')
large = get_pack_levels('test_large_puzzles')
one_k = get_pack_levels('test_existing_1k')
unity = get_pack_levels('test_unity_normal')
print(f"Loaded: Shapes={len(shapes)}, Large={len(large)}, 1K={len(one_k)}, UnityNormal={len(unity)}")

# Solvability check
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

def prepare(raw, lvl_num, name, diff, src_cat):
    xs = raw['XSize']
    ys = raw['YSize']
    arrows_out = []
    for a in raw['Arrows']:
        expanded = expand_indices(a['Indices'], xs)
        arrows_out.append({
            'Indices': expanded,
            'Dx': a['Dx'],
            'Dy': a['Dy'],
            'BendCount': a.get('BendCount', 0)
        })
    return {
        'LevelNumber': lvl_num,
        'XSize': xs,
        'YSize': ys,
        'MaxHearts': 3,
        'ShapeName': name if name else None,
        'Difficulty': diff,
        'ArrowCount': len(arrows_out),
        'SourceCategory': src_cat,
        'Arrows': arrows_out
    }

# Index collections by arrow count
unity_by_arr = defaultdict(list)
for l in unity:
    unity_by_arr[len(l['Arrows'])].append(l)

one_k_by_arr = defaultdict(list)
for l in one_k:
    one_k_by_arr[len(l['Arrows'])].append(l)

shapes_by_arr = defaultdict(list)
shapes_by_name = defaultdict(list)
for l in shapes:
    shapes_by_arr[len(l['Arrows'])].append(l)
    n = l.get('ShapeName', '').strip()
    if n:
        shapes_by_name[n.lower()].append(l)

large_by_name = defaultdict(list)
for l in large:
    n = l.get('ShapeName', '').strip()
    if n:
        large_by_name[n.lower()].append(l)

with open('scratch/curated_150_plan_v3.json', encoding='utf-8') as f:
    plan = json.load(f)

print("\nTesting matching for all 150 plan levels:")
success_count = 0
used_sigs = set()

def get_sig(lvl):
    all_i = tuple(sorted(i for a in lvl['Arrows'] for i in a['Indices']))
    return (lvl['XSize'], lvl['YSize'], len(lvl['Arrows']), all_i[:8])

for p in plan:
    lvl = p['level']
    target_arr = p['arrows']
    is_s = p['is_shape']
    pname = p['name']
    src = p['source_collection']
    diff = p['difficulty']
    
    cand = None
    chosen_shape_name = None
    
    if is_s:
        # Check shapes
        # 1. Look by name
        matches = [s for k, v in shapes_by_name.items() if pname.lower() in k or k in pname.lower() for s in v]
        if not matches and src == "Large Puzzles":
            matches = [s for k, v in large_by_name.items() if pname.lower() in k or k in pname.lower() for s in v]
            
        if matches:
            # find closest arrow count
            matches.sort(key=lambda s: abs(len(s['Arrows']) - target_arr))
            for m in matches:
                prep = prepare(m, lvl, m.get('ShapeName', pname), diff, src)
                if get_sig(prep) not in used_sigs and solve(prep):
                    cand = prep
                    chosen_shape_name = m.get('ShapeName', pname)
                    break
        if not cand:
            # Look by arrow count in shapes
            for delta in range(0, 10):
                for arr_c in (target_arr, target_arr + delta, target_arr - delta):
                    if arr_c in shapes_by_arr:
                        for s in shapes_by_arr[arr_c]:
                            s_name = s.get('ShapeName', pname)
                            prep = prepare(s, lvl, s_name, diff, src)
                            if get_sig(prep) not in used_sigs and solve(prep):
                                cand = prep
                                chosen_shape_name = s_name
                                break
                    if cand: break
                if cand: break
    else:
        # Normal level
        pool = unity_by_arr if src == "New Unity Normal Levels" else one_k_by_arr
        fb_pool = one_k_by_arr if src == "New Unity Normal Levels" else unity_by_arr
        
        # Exact requested early levels:
        if lvl == 1:
            cand = prepare(unity[0], 1, None, diff, src)
        elif lvl in (2, 3, 4):
            cand = prepare(one_k[lvl - 1], lvl, None, diff, src)
        else:
            # Try exact arrow in pool
            for delta in range(0, 5):
                for arr_c in (target_arr + delta, target_arr - delta):
                    if arr_c in pool:
                        for s in pool[arr_c]:
                            if not s.get('ShapeName'):
                                prep = prepare(s, lvl, None, diff, src)
                                if get_sig(prep) not in used_sigs and solve(prep):
                                    cand = prep
                                    break
                    if cand: break
                if cand: break
            if not cand:
                # Try fallback
                for delta in range(0, 10):
                    for arr_c in (target_arr + delta, target_arr - delta):
                        if arr_c in fb_pool:
                            for s in fb_pool[arr_c]:
                                if not s.get('ShapeName'):
                                    prep = prepare(s, lvl, None, diff, src)
                                    if get_sig(prep) not in used_sigs and solve(prep):
                                        cand = prep
                                        break
                        if cand: break
                    if cand: break

    if cand:
        used_sigs.add(get_sig(cand))
        success_count += 1
        arr_diff = cand['ArrowCount'] - target_arr
        if abs(arr_diff) > 2 or is_s:
            print(f"L{lvl:3d}: Target {target_arr} arr -> Got {cand['ArrowCount']} arr ({arr_diff:+d}) | Shape: {cand['ShapeName']} | Diff: {cand['Difficulty']}")
    else:
        print(f"L{lvl:3d}: FAILED to find level! Target: {target_arr} arrows, Shape: {is_s}")

print(f"\nResult: Found {success_count} / 150 levels!")
