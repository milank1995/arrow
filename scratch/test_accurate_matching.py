import sys
import subprocess
import json
import gzip
from collections import defaultdict
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

print("Loading packs...")
shapes = get_pack_levels('test_shape_puzzles')
large = get_pack_levels('test_large_puzzles')
one_k = get_pack_levels('test_existing_1k')
unity = get_pack_levels('test_unity_normal')
print(f"Loaded: Shapes={len(shapes)}, Large={len(large)}, 1K={len(one_k)}, UnityNormal={len(unity)}")

# Expansion and solver
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

# Indexing
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

large_by_arr = defaultdict(list)
large_by_name = defaultdict(list)
for l in large:
    large_by_arr[len(l['Arrows'])].append(l)
    n = l.get('ShapeName', '').strip()
    if n:
        large_by_name[n.lower()].append(l)

with open('scratch/curated_150_plan_v3.json', encoding='utf-8') as f:
    plan = json.load(f)

used_sigs = set()
def get_sig(lvl):
    all_i = tuple(sorted(i for a in lvl['Arrows'] for i in a['Indices']))
    return (lvl['XSize'], lvl['YSize'], len(lvl['Arrows']), all_i[:8])

results = []
for p in plan:
    lvl = p['level']
    target_arr = p['arrows']
    is_s = p['is_shape']
    pname = p['name']
    src = p['source_collection']
    diff = p['difficulty']
    
    cand = None
    chosen_name = None
    
    if is_s:
        # 1. Search shapes by name
        matches = [s for k, v in shapes_by_name.items() if pname.lower() in k or k in pname.lower() for s in v]
        if not matches:
            matches = [s for k, v in large_by_name.items() if pname.lower() in k or k in pname.lower() for s in v]
            
        if matches:
            matches.sort(key=lambda s: abs(len(s['Arrows']) - target_arr))
            for m in matches:
                prep = prepare(m, lvl, pname, diff, src)
                if get_sig(prep) not in used_sigs and solve(prep):
                    cand = prep
                    chosen_name = pname
                    break
        if not cand:
            # 2. Search shapes by exact or nearest arrow count
            for delta in range(0, 15):
                for arr_c in (target_arr, target_arr + delta, target_arr - delta):
                    if arr_c in shapes_by_arr:
                        for s in shapes_by_arr[arr_c]:
                            s_name = s.get('ShapeName', pname)
                            prep = prepare(s, lvl, pname, diff, src)
                            if get_sig(prep) not in used_sigs and solve(prep):
                                cand = prep
                                chosen_name = pname
                                break
                    if cand: break
                if cand: break
    else:
        # Normal level
        if src == "Large Puzzles":
            pool = large_by_arr
            fb_pool = unity_by_arr
        elif src == "Existing 1K Levels":
            pool = one_k_by_arr
            fb_pool = unity_by_arr
        else: # New Unity Normal Levels
            pool = unity_by_arr
            fb_pool = one_k_by_arr
            
        # Search exact arrow first, then delta
        for delta in range(0, 10):
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
            for delta in range(0, 15):
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
                
    assert cand is not None, f"L{lvl} failed to match!"
    used_sigs.add(get_sig(cand))
    results.append(cand)

print(f"Successfully matched and verified all {len(results)} levels!")

# Analyze arrow differences
diff_count = 0
for p, r in zip(plan, results):
    lvl = p['level']
    t_arr = p['arrows']
    g_arr = r['ArrowCount']
    d = g_arr - t_arr
    if d != 0:
        diff_count += 1
        print(f"L{lvl:3d}: Target {t_arr:3d} -> Got {g_arr:3d} ({d:+d}) | {r['ShapeName'] if r['ShapeName'] else 'Normal'}")

print(f"\nExact arrow matches: {150 - diff_count} / 150 ({100*(150-diff_count)/150:.1f}%)")
