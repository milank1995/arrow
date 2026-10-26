import os
import glob
import json
import gzip
import time
import re
import UnityPy
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!' # 32 bytes = AES-256
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
print("  STEP 1: INGESTING ALL SOURCES (23,000+ LEVELS)          ")
print("==========================================================")
t0 = time.time()
raw_candidates = []

# 1. Old Levels (1,001)
old_dir = "scratch/old_levels/arrow_native/app/src/main/assets/levels"
for p in glob.glob(os.path.join(old_dir, "Level_*.json")):
    try:
        with open(p, 'r', encoding='utf-8') as f:
            d = json.load(f)
            d['Source'] = os.path.basename(p)
            d['Hash'] = 'OLD_' + os.path.basename(p).replace('.json', '')
            raw_candidates.append(d)
    except Exception:
        pass

# 2. New Levels (1,533)
new_dir = "scratch/old_levels/arrow_native/app/src/main/assets/new_levels"
for p in glob.glob(os.path.join(new_dir, "NewLevel_*.json")):
    try:
        with open(p, 'r', encoding='utf-8') as f:
            d = json.load(f)
            d['Source'] = os.path.basename(p)
            d['Hash'] = 'NEW_' + os.path.basename(p).replace('.json', '')
            raw_candidates.append(d)
    except Exception:
        pass

# 3. Unity Levels (21,204)
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

print(f"Total unique geometries across all sources: {len(unique_by_geom)}")

print("Verifying solvability of unique candidates...")
solvable_unique = []
for lvl in unique_by_geom.values():
    if solve_level(lvl):
        solvable_unique.append(lvl)

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

print(f"Identified {len(shapes_pool)} solvable shape puzzles")
print(f"Identified {len(regular_pool)} solvable regular puzzles")

# Sort pools by complexity (arrow count, grid area)
shapes_pool.sort(key=lambda l: (len(l['Arrows']), l['XSize'] * l['YSize']))
regular_pool.sort(key=lambda l: (len(l['Arrows']), l['XSize'] * l['YSize']))

print("==========================================================")
print("  STEP 4: CURATING ONBOARDING LEVELS 1 - 30               ")
print("==========================================================")
# Target arrow counts for Levels 1 to 30:
# Regular levels: [4, 6, 8, (14 shape), 10, 12, 14, 16, (20 shape), 16, 18, 20, 22, (25 shape), 20, 22, 24, 26, (32 shape), 28, 30, 32, 34, (38 shape), 35, 38, 40, 42, (46 shape), 50]
onboarding_levels = []
used_regular = set()
used_shapes = set()

def pick_closest(pool, used_set, target_arrows, max_diff=3):
    best = None
    best_diff = 999
    for i, lvl in enumerate(pool):
        if i in used_set:
            continue
        diff = abs(len(lvl['Arrows']) - target_arrows)
        if diff < best_diff:
            best_diff = diff
            best = (i, lvl)
            if diff == 0:
                break
    if best is not None:
        used_set.add(best[0])
        return best[1]
    return None

onboarding_plan = [
    (1, False, 4),
    (2, False, 6),
    (3, False, 8),
    (4, True, 14),   # Shape Milestone 1
    (5, False, 10),
    (6, False, 12),
    (7, False, 14),
    (8, False, 16),
    (9, True, 20),   # Shape Milestone 2
    (10, False, 16),
    (11, False, 18),
    (12, False, 20),
    (13, False, 22),
    (14, True, 25),  # Shape Milestone 3
    (15, False, 20),
    (16, False, 22),
    (17, False, 24),
    (18, False, 26),
    (19, True, 32),  # Shape Milestone 4
    (20, False, 28),
    (21, False, 30),
    (22, False, 32),
    (23, False, 34),
    (24, True, 38),  # Shape Milestone 5
    (25, False, 35),
    (26, False, 38),
    (27, False, 40),
    (28, False, 42),
    (29, True, 46),  # Shape Milestone 6
    (30, False, 50)  # Chapter 1 Boss Milestone
]

for lvl_num, is_shape, target_arr in onboarding_plan:
    if is_shape:
        lvl = pick_closest(shapes_pool, used_shapes, target_arr)
    else:
        lvl = pick_closest(regular_pool, used_regular, target_arr)
    onboarding_levels.append(lvl)
    print(f"  Level {lvl_num:2d} ({'SHAPE *' if is_shape else 'REGULAR'}): {len(lvl['Arrows']):2d} arrows ({lvl['XSize']}x{lvl['YSize']})")

print("==========================================================")
print("  STEP 5: ASSEMBLING MASTER 5,000-LEVEL SEQUENCE          ")
print("==========================================================")
master_levels = list(onboarding_levels)

# Remaining regular pool: filter out already used
remaining_regular = [lvl for i, lvl in enumerate(regular_pool) if i not in used_regular]
remaining_shapes = [lvl for i, lvl in enumerate(shapes_pool) if i not in used_shapes]

# We need levels 31 to 5000 (total 4970 levels).
# Milestones are at every L % 5 == 4:
# 34, 39, 44, 49, 54, ... 4999 (exactly 994 milestone shape slots)
# Plus the 6 in onboarding = 1,000 milestone shape puzzles total!
# Remaining regular slots = 3976 slots (plus 24 in onboarding = 4,000 regular total!)
milestone_slots = set()
for l in range(31, 5001):
    if l % 5 == 4:
        milestone_slots.add(l)

num_regular_needed = 4970 - len(milestone_slots)
num_shapes_needed = len(milestone_slots)
print(f"Slots needed for L31-5000: {num_regular_needed} Regular, {num_shapes_needed} Shapes")

# Select the best regular levels with smooth arrow progression
# Target arrow curve from Level 31 (40 arrows) to Level 5000 (150 arrows)
# Interpolate target arrows for regular slots:
selected_regular = []
reg_pool_by_arr = defaultdict(list)
for lvl in remaining_regular:
    reg_pool_by_arr[len(lvl['Arrows'])].append(lvl)

# Sort available arrow counts
available_arrow_counts = sorted(reg_pool_by_arr.keys())

# We want regular levels to span from ~35 arrows up to ~150+ arrows smoothly
# Let's sample evenly across the sorted remaining regular levels
step = len(remaining_regular) / num_regular_needed
for i in range(num_regular_needed):
    idx = int(i * step)
    selected_regular.append(remaining_regular[idx])

# Ensure selected regular levels are sorted monotonically by arrow count
selected_regular.sort(key=lambda l: (len(l['Arrows']), l['XSize'] * l['YSize']))

# For shapes: sample evenly from remaining shapes
step_s = len(remaining_shapes) / num_shapes_needed
selected_shapes = []
for i in range(num_shapes_needed):
    idx = int(i * step_s)
    selected_shapes.append(remaining_shapes[idx])
selected_shapes.sort(key=lambda l: (len(l['Arrows']), l['XSize'] * l['YSize']))

print(f"Selected {len(selected_regular)} regular levels (arrows: {len(selected_regular[0]['Arrows'])} -> {len(selected_regular[-1]['Arrows'])})")
print(f"Selected {len(selected_shapes)} shape levels (arrows: {len(selected_shapes[0]['Arrows'])} -> {len(selected_shapes[-1]['Arrows'])})")

# Weave into master sequence
reg_idx = 0
shape_idx = 0
for l in range(31, 5001):
    if l in milestone_slots:
        master_levels.append(selected_shapes[shape_idx])
        shape_idx += 1
    else:
        master_levels.append(selected_regular[reg_idx])
        reg_idx += 1

print(f"Total master levels assembled: {len(master_levels)}")
assert len(master_levels) == 5000

print("==========================================================")
print("  STEP 6: VALIDATING 100% SOLVABILITY OF ALL 5,000 LEVELS  ")
print("==========================================================")
t_val = time.time()
unsolvable = []
for idx, lvl in enumerate(master_levels):
    if not solve_level(lvl):
        unsolvable.append(idx + 1)

print(f"Solvability validation complete in {time.time() - t_val:.2f}s")
if unsolvable:
    print(f"ERROR: Found {len(unsolvable)} unsolvable levels: {unsolvable[:10]}")
    exit(1)
else:
    print("ALL 5,000 LEVELS ARE 100% SOLVABLE! Zero deadlocks!")

print("==========================================================")
print("  STEP 7: ENCRYPTING & PACKAGING 5 CHUNK FILES            ")
print("==========================================================")
chunk_size = 1000
for pack_idx in range(5):
    start = pack_idx * chunk_size
    end = start + chunk_size
    chunk = master_levels[start:end]
    
    # Assign canonical level numbers
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

# Remove old packs 05 to 14
for old_pack in range(5, 15):
    p = os.path.join(OUTPUT_DIR, f"level_pack_{old_pack:02d}.dat")
    if os.path.exists(p):
        os.remove(p)
        print(f"Removed obsolete pack: {p}")

print("\n==========================================================")
print("  STEP 8: PROGRESSION CURVE AUDIT (500-LEVEL BLOCKS)      ")
print("==========================================================")
for start in range(0, 5000, 500):
    end = start + 500
    counts = [len(master_levels[i]['Arrows']) for i in range(start, end)]
    print(f"Levels {start+1:4d} - {end:4d}: min={min(counts):2d}, avg={sum(counts)/len(counts):4.1f}, max={max(counts):3d}")

print("\nMASTER 5,000 LEVEL PIPELINE COMPLETED SUCCESSFULLY!")
