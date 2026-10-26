import os
import glob
import json
import gzip
import time
import re
import subprocess
import UnityPy
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'
OUTPUT_DIR = r'arrow_native/app/src/main/assets/levels'

def encrypt_bytes(data_bytes):
    compressed = gzip.compress(data_bytes, compresslevel=9)
    padder = padding.PKCS7(128).padder()
    padded_data = padder.update(compressed) + padder.finalize()
    iv = os.urandom(16)
    cipher = Cipher(algorithms.AES(AES_KEY), modes.CBC(iv))
    encryptor = cipher.encryptor()
    ciphertext = encryptor.update(padded_data) + encryptor.finalize()
    return iv + ciphertext

def solve_level(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows:
        return False
    
    active_arrows = list(range(len(arrows)))
    occupied = {}
    arrow_heads = []
    arrow_dirs = []
    arrow_cells = []
    
    for i, a in enumerate(arrows):
        dx = a['Dx']
        dy = -a['Dy'] # canvas dy
        indices = a['Indices']
        cells = set()
        for idx in indices:
            ux = idx % xs
            uy = idx // xs
            cx = ux
            cy = (ys - 1) - uy
            cells.add((cx, cy))
            occupied[(cx, cy)] = i
            
        head_idx = indices[0]
        hx = head_idx % xs
        hy = (ys - 1) - (head_idx // xs)
        arrow_heads.append((hx, hy))
        arrow_dirs.append((dx, dy))
        arrow_cells.append(cells)
        
    while active_arrows:
        progress = False
        for a_idx in list(active_arrows):
            hx, hy = arrow_heads[a_idx]
            dx, dy = arrow_dirs[a_idx]
            cur_x = hx + dx
            cur_y = hy + dy
            blocked = False
            while 0 <= cur_x < xs and 0 <= cur_y < ys:
                if (cur_x, cur_y) in occupied and occupied[(cur_x, cur_y)] != a_idx:
                    blocked = True
                    break
                cur_x += dx
                cur_y += dy
            if not blocked:
                for c in arrow_cells[a_idx]:
                    if c in occupied and occupied[c] == a_idx:
                        del occupied[c]
                active_arrows.remove(a_idx)
                progress = True
                break
        if not progress:
            return False
            
    return True

print("==========================================================")
print("  STEP 1: INGESTING CANDIDATE LEVELS                      ")
print("==========================================================")
t0 = time.time()
raw_candidates = []

# Ensure scratch old_levels extracted
old_dir = "scratch/old_levels/arrow_native/app/src/main/assets/levels"
new_dir = "scratch/old_levels/arrow_native/app/src/main/assets/new_levels"
if not os.path.exists(old_dir):
    os.makedirs("scratch/old_levels", exist_ok=True)
    subprocess.run("git archive 9b5ebd7 arrow_native/app/src/main/assets/levels arrow_native/app/src/main/assets/new_levels | tar -x -C scratch/old_levels", shell=True)

for p in glob.glob(os.path.join(old_dir, "Level_*.json")):
    try:
        with open(p, 'r', encoding='utf-8') as f:
            d = json.load(f)
            d['Source'] = os.path.basename(p)
            d['Hash'] = 'OLD_' + os.path.basename(p).replace('.json', '')
            raw_candidates.append(d)
    except Exception:
        pass

for p in glob.glob(os.path.join(new_dir, "NewLevel_*.json")):
    try:
        with open(p, 'r', encoding='utf-8') as f:
            d = json.load(f)
            d['Source'] = os.path.basename(p)
            d['Hash'] = 'NEW_' + os.path.basename(p).replace('.json', '')
            raw_candidates.append(d)
    except Exception:
        pass

# Unity levels
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'
with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        try:
            raw = json.loads(gzip.decompress(b))
            xs = raw['XSize']
            ys = raw['YSize']
            arrows_out = []
            for a in raw.get('Arrows', []):
                pts = a.get('Points', [])
                if not pts or len(pts) < 2:
                    continue
                hx, hy = pts[0] % xs, pts[0] // xs
                nx, ny = pts[1] % xs, pts[1] // xs
                dx = 1 if hx > nx else (-1 if hx < nx else 0)
                dy = 1 if hy > ny else (-1 if hy < ny else 0)
                arrows_out.append({
                    'Indices': pts,
                    'Dx': dx,
                    'Dy': dy,
                    'BendCount': len(pts) - 2 if len(pts) > 2 else 0
                })
            raw_candidates.append({
                'XSize': xs,
                'YSize': ys,
                'Arrows': arrows_out,
                'MaxHearts': raw.get('MaxHearts', 3),
                'Hash': name,
                'Source': 'Unity_' + name
            })
        except Exception:
            pass

print(f"Total raw candidates ingested: {len(raw_candidates)} in {time.time() - t0:.2f}s")

print("==========================================================")
print("  STEP 2: GEOMETRIC DEDUPLICATION & SOLVABILITY CHECK     ")
print("==========================================================")
unique_by_geom = {}
for lvl in raw_candidates:
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl.get('Arrows', [])
    if not arrows:
        continue
    arrow_sigs = []
    for a in arrows:
        indices = tuple(sorted(a.get('Indices', [])))
        arrow_sigs.append((indices, a.get('Dx'), a.get('Dy')))
    sig = (xs, ys, tuple(sorted(arrow_sigs)))
    if sig not in unique_by_geom:
        unique_by_geom[sig] = lvl

print(f"Total unique geometries: {len(unique_by_geom)}")
solvable_unique = [lvl for lvl in unique_by_geom.values() if solve_level(lvl)]
print(f"Total 100% solvable unique levels: {len(solvable_unique)}")

print("==========================================================")
print("  STEP 3: CLASSIFYING SHAPE vs REGULAR PUZZLES            ")
print("==========================================================")
shapes_pool = []
regular_pool = []

for lvl in solvable_unique:
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    narr = len(arrows)
    occupied = set()
    for a in arrows:
        for idx in a.get('Indices', []):
            occupied.add(idx)
    tot = xs * ys
    occ = len(occupied) / tot if tot > 0 else 1.0
    
    # Shape criteria: spacious boundary forming an iconic silhouette
    if occ <= 0.70 and xs >= 18 and ys >= 18 and narr >= 12:
        shapes_pool.append(lvl)
    else:
        regular_pool.append(lvl)

print(f"Shapes available: {len(shapes_pool)}")
print(f"Regular puzzles available: {len(regular_pool)}")

shapes_by_arr = defaultdict(list)
for s in shapes_pool:
    shapes_by_arr[len(s['Arrows'])].append(s)

reg_by_arr = defaultdict(list)
for r in regular_pool:
    reg_by_arr[len(r['Arrows'])].append(r)

print("==========================================================")
print("  STEP 4: EXTRACTING AMAZE GO TARGET PROGRESSION          ")
print("==========================================================")
configs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
with open(configs_path, 'rb') as f:
    f.seek(32)
    env_cfg = UnityPy.load(f.read())

unity_tables = {}
for obj in env_cfg.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
        if hashes:
            unity_tables[name] = hashes

amaze_sequence = []
seen = set()
for h in unity_tables.get('GuideLevels1', []):
    if h not in seen:
        seen.add(h)
        amaze_sequence.append(('GuideLevels1', h))

for ptab in ['NormalLevelsPriority', 'NormalLevelsPriority1', 'NormalLevelsPriority2', 'NormalLevelsPriority3', 'NormalLevels1']:
    for h in unity_tables.get(ptab, []):
        if h not in seen:
            seen.add(h)
            amaze_sequence.append((ptab, h))
        if len(amaze_sequence) >= 2000:
            break
    if len(amaze_sequence) >= 2000:
        break

# Map targets
amaze_raw_map = {}
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        amaze_raw_map[name] = d.m_Script.encode('utf-8', 'surrogateescape')

target_arrows = []
for idx, (tab, h) in enumerate(amaze_sequence):
    lvl_num = idx + 1
    if h in amaze_raw_map:
        raw = json.loads(gzip.decompress(amaze_raw_map[h]))
        cnt = len(raw.get('Arrows', []))
    else:
        cnt = 60
    target_arrows.append(cnt)

print(f"Extracted {len(target_arrows)} Amaze Go target arrow counts.")

print("==========================================================")
print("  STEP 5: BUILDING 2,000-LEVEL CONTINUOUS MOMENTUM PACK   ")
print("==========================================================")
# Strategy:
# For every level 1 to 2000:
# Enforce a hard non-regressing floor:
# L1-4: floor = 4
# L5-10: floor = 12
# L11-20: floor = 18
# L21-30: floor = 22
# L31-50: floor = 28
# L51-100: floor = 38
# L101-250: floor = 45
# L251-500: floor = 52
# L501-1000: floor = 60
# L1001-1500: floor = 70
# L1501-2000: floor = 80

def get_floor(l):
    if l <= 4: return 4
    if l <= 10: return 12
    if l <= 20: return 18
    if l <= 30: return 22
    if l <= 50: return 28
    if l <= 100: return 38
    if l <= 250: return 45
    if l <= 500: return 52
    if l <= 1000: return 60
    if l <= 1500: return 70
    return 80

used_hashes = set()
master_2000 = []

def find_best_level(pool_dict, target, floor_min, max_dist=15):
    # Search closest arrow count >= floor_min
    best = None
    best_diff = 999
    
    # Try exact or nearby arrow counts
    sorted_counts = sorted(pool_dict.keys(), key=lambda c: (abs(c - target), -c))
    for cnt in sorted_counts:
        if cnt < floor_min:
            continue
        diff = abs(cnt - target)
        if diff > max_dist and best is not None:
            break
        for lvl in pool_dict[cnt]:
            if lvl['Hash'] not in used_hashes:
                used_hashes.add(lvl['Hash'])
                return lvl
                
    # If no candidate found within floor, take highest available above floor
    for cnt in sorted(pool_dict.keys(), reverse=True):
        if cnt >= floor_min:
            for lvl in pool_dict[cnt]:
                if lvl['Hash'] not in used_hashes:
                    used_hashes.add(lvl['Hash'])
                    return lvl
    return None

for idx in range(2000):
    lvl_num = idx + 1
    is_milestone = (lvl_num % 5 == 0)
    target = target_arrows[idx]
    floor_min = get_floor(lvl_num)
    
    # Ensure target is at least floor_min
    target = max(target, floor_min)
    
    # Special high-challenge milestones in early game
    if lvl_num == 5:
        target = 54
    elif lvl_num == 10:
        target = 99
    elif lvl_num == 15:
        target = 53
    elif lvl_num == 20:
        target = 49
    elif lvl_num == 25:
        target = 74
    elif lvl_num == 26:
        # Cactus
        target = 130
    elif lvl_num == 30:
        target = 68
    elif lvl_num == 35:
        target = 101
    elif lvl_num == 40:
        target = 122
        
    if is_milestone or lvl_num == 26:
        # Pull from shapes pool
        selected = find_best_level(shapes_by_arr, target, floor_min)
        if selected is None:
            selected = find_best_level(reg_by_arr, target, floor_min)
    else:
        # Pull from regular pool
        selected = find_best_level(reg_by_arr, target, floor_min)
        if selected is None:
            selected = find_best_level(shapes_by_arr, target, floor_min)
            
    assert selected is not None, f"Could not find level for L{lvl_num}"
    master_2000.append(selected)

print(f"Total master levels assembled: {len(master_2000)}")

print("==========================================================")
print("  STEP 6: VERIFYING 100% SOLVABILITY OF ALL 2,000 LEVELS  ")
print("==========================================================")
t_val = time.time()
unsolvable = []
for idx, lvl in enumerate(master_2000):
    if not solve_level(lvl):
        unsolvable.append(idx + 1)

print(f"Solvability validation complete in {time.time() - t_val:.2f}s")
if unsolvable:
    print(f"ERROR: Found {len(unsolvable)} unsolvable levels!")
    exit(1)
print("ALL 2,000 LEVELS ARE 100% SOLVABLE! Zero deadlocks!")

print("\n=== First 50 Levels Audit ===")
for i in range(50):
    lvl = master_2000[i]
    is_m = ((i + 1) % 5 == 0)
    print(f"Level {i+1:2d}: {len(lvl['Arrows']):3d} arrows ({lvl['XSize']:2d}x{lvl['YSize']:2d}) {'[MILESTONE SHAPE *]' if is_m else ''} (Hash={lvl['Hash'][:8]})")

print("\n=== Progression Curve Audit (100-Level Blocks) ===")
for start in range(0, 2000, 100):
    end = start + 100
    counts = [len(master_2000[i]['Arrows']) for i in range(start, end)]
    print(f"Levels {start+1:4d} - {end:4d}: min={min(counts):2d}, avg={sum(counts)/len(counts):4.1f}, max={max(counts):3d}")

print("==========================================================")
print("  STEP 7: ENCRYPTING & PACKAGING 2 CHUNK FILES            ")
print("==========================================================")
chunk_size = 1000
for pack_idx in range(2):
    start = pack_idx * chunk_size
    end = start + chunk_size
    chunk = master_2000[start:end]
    
    chunk_json_objs = []
    for offset, lvl in enumerate(chunk):
        lvl_num = start + offset + 1
        chunk_json_objs.append({
            'Level': lvl_num,
            'XSize': lvl['XSize'],
            'YSize': lvl['YSize'],
            'MaxHearts': lvl.get('MaxHearts', 3),
            'Hash': lvl.get('Hash', f'AMZ_{lvl_num:05d}'),
            'Arrows': lvl['Arrows']
        })
        
    chunk_json_str = json.dumps(chunk_json_objs)
    encrypted_bytes = encrypt_bytes(chunk_json_str.encode('utf-8'))
    
    out_file = os.path.join(OUTPUT_DIR, f"level_pack_{pack_idx:02d}.dat")
    with open(out_file, 'wb') as f:
        f.write(encrypted_bytes)
    print(f"Wrote {out_file} ({len(chunk_json_objs)} levels, {len(encrypted_bytes):,} bytes)")

# Remove old packs 02 to 14
for old_pack in range(2, 15):
    p = os.path.join(OUTPUT_DIR, f"level_pack_{old_pack:02d}.dat")
    if os.path.exists(p):
        os.remove(p)
        print(f"Removed obsolete pack: {p}")

print("\n2,000-LEVEL PIPELINE COMPLETED SUCCESSFULLY!")
