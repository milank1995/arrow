import os
import sys
import json
import gzip
import time
import glob
import re
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend
import UnityPy

sys.stdout.reconfigure(line_buffering=True)
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'

def encrypt_pack(json_obj):
    raw_json = json.dumps(json_obj, separators=(',', ':')).encode('utf-8')
    gzipped = gzip.compress(raw_json)
    pad_len = 16 - (len(gzipped) % 16)
    padded = gzipped + bytes([pad_len] * pad_len)
    iv = os.urandom(16)
    cipher = Cipher(algorithms.AES(AES_KEY), modes.CBC(iv), backend=default_backend())
    encryptor = cipher.encryptor()
    ciphertext = encryptor.update(padded) + encryptor.finalize()
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

def crop_level(lvl, padding=1):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows:
        return lvl
    
    all_indices = [p for a in arrows for p in a['Indices']]
    if not all_indices:
        return lvl
        
    min_ux = min(idx % xs for idx in all_indices)
    max_ux = max(idx % xs for idx in all_indices)
    min_uy = min(idx // xs for idx in all_indices)
    max_uy = max(idx // xs for idx in all_indices)
    
    # If already tightly cropped
    if min_ux == 0 and min_uy == 0 and max_ux == xs - 1 and max_uy == ys - 1:
        return lvl
        
    new_min_ux = max(0, min_ux - padding)
    new_max_ux = min(xs - 1, max_ux + padding)
    new_min_uy = max(0, min_uy - padding)
    new_max_uy = min(ys - 1, max_uy + padding)
    
    new_xs = new_max_ux - new_min_ux + 1
    new_ys = new_max_uy - new_min_uy + 1
    
    new_arrows = []
    for a in arrows:
        new_indices = []
        for idx in a['Indices']:
            ux = idx % xs
            uy = idx // xs
            new_ux = ux - new_min_ux
            new_uy = uy - new_min_uy
            new_indices.append(new_uy * new_xs + new_ux)
        
        new_a = dict(a)
        new_a['Indices'] = new_indices
        new_arrows.append(new_a)
        
    cropped_lvl = dict(lvl)
    cropped_lvl['XSize'] = new_xs
    cropped_lvl['YSize'] = new_ys
    cropped_lvl['Arrows'] = new_arrows
    return cropped_lvl

print("==========================================================", flush=True)
print("  STEP 1: INGESTING CANDIDATES & CONFIG TABLES            ", flush=True)
print("==========================================================", flush=True)
t0 = time.time()

# 1. Ingest official shape level hashes from Unity levelconfigs
config_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
official_shape_hashes = set()
with open(config_path, 'rb') as f:
    f.seek(32)
    env_cfg = UnityPy.load(f.read())
for obj in env_cfg.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        if 'shapenormal' in name.lower():
            for h in re.findall(r'[0-9a-fA-F]{16}', d.m_Script):
                official_shape_hashes.add(h.lower())

print(f"Loaded {len(official_shape_hashes)} official shape hashes from Unity design tables.", flush=True)

raw_candidates = []

# 2. Ingest Scratch levels
old_dir = "scratch/old_levels/arrow_native/app/src/main/assets/levels"
new_dir = "scratch/old_levels/arrow_native/app/src/main/assets/new_levels"
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

# 3. Ingest Unity levels
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
            if arrows_out:
                raw_candidates.append({
                    'XSize': xs,
                    'YSize': ys,
                    'Arrows': arrows_out,
                    'Hash': name,
                    'Source': 'Unity_' + name
                })
        except Exception:
            pass

print(f"Total raw candidates ingested: {len(raw_candidates)} (in {time.time()-t0:.1f}s)", flush=True)

print("==========================================================", flush=True)
print("  STEP 2: AUTO-CROP & GEOMETRIC DEDUPLICATION             ", flush=True)
print("==========================================================", flush=True)
t1 = time.time()
seen_fps = set()
unique_candidates = []

for c in raw_candidates:
    # Auto-crop empty outer borders with 1-cell padding
    cropped = crop_level(c, padding=1)
    xs = cropped['XSize']
    ys = cropped['YSize']
    arrs = cropped['Arrows']
    
    # Cap maximum dimension to 58 to ensure arrows always render with comfortable touch targets
    if max(xs, ys) > 58:
        continue
        
    fp_items = []
    for a in arrs:
        idxs = a['Indices']
        fp_items.append((idxs[0], idxs[-1], a['Dx'], a['Dy'], a['BendCount']))
    fp = (xs, ys, tuple(sorted(fp_items)))
    if fp not in seen_fps:
        seen_fps.add(fp)
        unique_candidates.append(cropped)

print(f"Unique cropped candidates (max dim <= 58): {len(unique_candidates)} (in {time.time()-t1:.1f}s)", flush=True)

print("==========================================================", flush=True)
print("  STEP 3: SOLVABILITY FILTER & SHAPE CLASSIFICATION       ", flush=True)
print("==========================================================", flush=True)
t2 = time.time()
solvable_all = []
solvable_shapes = []

landmark_keywords = ['sphinx', 'taj', 'tower', 'opera', 'arc', 'starry', 'pirate', 'chest', 'shape', 'gallery', 'trophy', 'cat', 'horse', 'robot', 'car', 'falcon', 'fish', 'guitar', 'camera', 'crown', 'butterfly', 'heart', 'star', 'bird', 'flower', 'apple', 'anchor', 'diamond', 'sushi']

for c in unique_candidates:
    h = c.get('Hash', '').lower()
    xs = c['XSize']
    ys = c['YSize']
    arrs = c['Arrows']
    narr = len(arrs)
    
    occupied = set()
    for a in arrs:
        for p in a['Indices']:
            occupied.add(p)
    occ = len(occupied) / max(1, xs * ys)
    
    is_official_shape = any(sh in h for sh in official_shape_hashes)
    is_landmark = any(k in h for k in landmark_keywords)
    is_silhouette_shape = (occ <= 0.52 and xs >= 18 and ys >= 18 and narr >= 18)
    
    if solve_level(c):
        c['Occupancy'] = round(occ, 3)
        c['ArrowCount'] = narr
        c['IsOfficialShape'] = is_official_shape
        c['IsLandmark'] = is_landmark
        solvable_all.append(c)
        
        if is_official_shape or is_landmark or is_silhouette_shape:
            solvable_shapes.append(c)

print(f"Total verified solvable candidates: {len(solvable_all)}", flush=True)
print(f"Solvable shape candidates:          {len(solvable_shapes)}", flush=True)
print(f"Solvability validation completed in {time.time()-t2:.1f}s", flush=True)

print("==========================================================", flush=True)
print("  STEP 4: CURATING DYNAMIC SHAPE PACK (6 TIERS)           ", flush=True)
print("==========================================================", flush=True)
# Target calibrated shape tiers:
# Tier 0 (L5-50): 20 to 60 arrows, compact grid <= 32x32
# Tier 1 (L55-200): 60 to 95 arrows, grid <= 42x42
# Tier 2 (L205-500): 95 to 130 arrows, grid <= 48x48
# Tier 3 (L505-1000): 130 to 170 arrows, grid <= 52x52
# Tier 4 (L1005-1500): 170 to 210 arrows, grid <= 56x56
# Tier 5 (L1505-2000): 180 to 265 arrows, grid <= 58x58

tier_defs = [
    (0, "Tier 0 (L5-50)", 20, 60, 32, 160),
    (1, "Tier 1 (L55-200)", 60, 95, 42, 200),
    (2, "Tier 2 (L205-500)", 95, 130, 48, 240),
    (3, "Tier 3 (L505-1000)", 130, 170, 52, 200),
    (4, "Tier 4 (L1005-1500)", 170, 210, 56, 150),
    (5, "Tier 5 (L1505-2000)", 180, 265, 58, 150)
]

used_shape_hashes = set()
shape_pack_list = []

for t_idx, t_name, min_arr, max_arr, max_dim, quota in tier_defs:
    pool = [
        s for s in solvable_shapes
        if min_arr <= s['ArrowCount'] <= max_arr
        and max(s['XSize'], s['YSize']) <= max_dim
        and s['Hash'] not in used_shape_hashes
    ]
    # Priority: Official shapes first, then landmark names, then arrow count
    pool.sort(key=lambda s: (
        0 if s.get('IsOfficialShape', False) else (1 if s.get('IsLandmark', False) else 2),
        s['ArrowCount'],
        s['XSize'] * s['YSize']
    ))
    selected = pool[:quota]
    # If pool is slightly short for quota in Tier 5, pull from solvable_all in that arrow/dim range
    if len(selected) < quota and t_idx == 5:
        extra_cands = [
            c for c in solvable_all
            if min_arr <= c['ArrowCount'] <= max_arr
            and max(c['XSize'], c['YSize']) <= max_dim
            and c['Hash'] not in used_shape_hashes
            and c not in selected
        ]
        extra_cands.sort(key=lambda c: (c['ArrowCount'], c['XSize'] * c['YSize']))
        needed = quota - len(selected)
        selected.extend(extra_cands[:needed])
        
    for s in selected:
        used_shape_hashes.add(s['Hash'])
        shape_pack_list.append({
            'ShapeId': len(shape_pack_list),
            'Tier': t_idx,
            'Hash': s['Hash'],
            'XSize': s['XSize'],
            'YSize': s['YSize'],
            'ArrowCount': s['ArrowCount'],
            'Occupancy': s['Occupancy'],
            'MaxHearts': 3,
            'Arrows': s['Arrows']
        })
    print(f"{t_name}: Selected {len(selected)} unique shapes (targets {min_arr}-{max_arr} arrows, max dim {max_dim})", flush=True)

print(f"Total dynamic shapes in Shape Pack: {len(shape_pack_list)}", flush=True)

# Save shape_pack.dat
shape_pack_path = "arrow_native/app/src/main/assets/levels/shape_pack.dat"
with open(shape_pack_path, "wb") as f:
    f.write(encrypt_pack(shape_pack_list))
print(f"Encrypted shape_pack.dat created: {os.path.getsize(shape_pack_path) / 1024:.1f} KB", flush=True)

print("==========================================================", flush=True)
print("  STEP 5: BUILDING 2,000 REGULAR CAMPAIGN LEVELS          ", flush=True)
print("==========================================================", flush=True)

campaign_2000 = [None] * 2000
used_campaign_hashes = set()

# Curate specific iconic milestone shapes for early milestones (L5, 10, 15, 20, 25, 30, 35, 40, 45, 50)
early_milestone_shapes = {
    5:  24,  # Compact iconic shape (e.g. 7dbf39492f230c20, 23x23)
    10: 28,  # Bird / Parrot (e.g. aa9ac9686e23de71, 29x29)
    15: 34,  # Anchor / Icon (e.g. 6c4ffb28ec59ef95, 23x21)
    20: 39,  # Shield / Crest (e.g. ab3554567ed86251, 20x26)
    25: 43,  # Trophy (e.g. fec4f9635987f93d, 27x26)
    30: 48,  # Star / Rocket (e.g. 9b1dc0cfd5870020, 28x28)
    35: 50,  # Diamond / Crown (e.g. 0e3200028896cf1f, 29x31)
    40: 54,  # Flower / Icon (e.g. cb74c99238c5ad9c, 28x27)
    45: 57,  # Heart (e.g. e3f71776b765205a, 30x30)
    50: 60   # Milestone Landmark (30x32)
}

for lvl_num, target_arr in early_milestone_shapes.items():
    pool0 = [s for s in shape_pack_list if s['Tier'] == 0]
    cands = [s for s in pool0 if s['ArrowCount'] == target_arr]
    if not cands:
        cands = [s for s in pool0 if abs(s['ArrowCount'] - target_arr) <= 2]
    s_obj = cands[0] if cands else pool0[0]
    campaign_2000[lvl_num - 1] = {
        'Level': lvl_num,
        'XSize': s_obj['XSize'],
        'YSize': s_obj['YSize'],
        'MaxHearts': 3,
        'Hash': s_obj['Hash'],
        'ArrowCount': s_obj['ArrowCount'],
        'IsMilestoneShape': True,
        'Arrows': s_obj['Arrows']
    }

# Fill remaining milestone shapes (L55 to L2000, every 5th level)
shape_tier_ptr = defaultdict(int)
for lvl_num in range(55, 2001, 5):
    if lvl_num <= 200: t_idx = 1
    elif lvl_num <= 500: t_idx = 2
    elif lvl_num <= 1000: t_idx = 3
    elif lvl_num <= 1500: t_idx = 4
    else: t_idx = 5
    
    tier_shapes = [s for s in shape_pack_list if s['Tier'] == t_idx]
    s_obj = tier_shapes[shape_tier_ptr[t_idx] % len(tier_shapes)]
    shape_tier_ptr[t_idx] += 1
    
    campaign_2000[lvl_num - 1] = {
        'Level': lvl_num,
        'XSize': s_obj['XSize'],
        'YSize': s_obj['YSize'],
        'MaxHearts': 3,
        'Hash': s_obj['Hash'],
        'ArrowCount': s_obj['ArrowCount'],
        'IsMilestoneShape': True,
        'Arrows': s_obj['Arrows']
    }

# Pool of available regular levels: all solvable candidates not used in shape_pack_list
solvable_regular = [c for c in solvable_all if c['Hash'] not in used_shape_hashes]
print(f"Available regular candidates (not in shape pack): {len(solvable_regular)}", flush=True)

# First 24 regular slots onboarding targets (L1-4, 6-9, 11-14, 16-19, 21-24, 26-29)
early_regular_targets = {
    1: 4, 2: 4, 3: 5, 4: 7,
    6: 12, 7: 14, 8: 16, 9: 18,
    11: 18, 12: 20, 13: 22, 14: 24,
    16: 25, 17: 27, 18: 29, 19: 31,
    21: 32, 22: 34, 23: 36, 24: 38,
    26: 38, 27: 40, 28: 42, 29: 44
}

for lvl_num, target_arr in sorted(early_regular_targets.items()):
    cands = [r for r in solvable_regular if r['ArrowCount'] == target_arr and r['Hash'] not in used_campaign_hashes]
    if not cands:
        cands = [r for r in solvable_regular if abs(r['ArrowCount'] - target_arr) <= 2 and r['Hash'] not in used_campaign_hashes]
    if not cands:
        cands = [r for r in solvable_regular if r['Hash'] not in used_campaign_hashes]
    chosen = cands[0]
    used_campaign_hashes.add(chosen['Hash'])
    campaign_2000[lvl_num - 1] = {
        'Level': lvl_num,
        'XSize': chosen['XSize'],
        'YSize': chosen['YSize'],
        'MaxHearts': 3,
        'Hash': chosen['Hash'],
        'ArrowCount': chosen['ArrowCount'],
        'Arrows': chosen['Arrows']
    }

# Fill remaining 1,576 regular levels with a smooth monotonic progression curve (44 to 245 arrows)
unfilled_slots = [i for i in range(2000) if campaign_2000[i] is None]
print(f"Unfilled regular slots: {len(unfilled_slots)}", flush=True)

reg_pool = [r for r in solvable_regular if r['Hash'] not in used_campaign_hashes and r['ArrowCount'] >= 42 and max(r['XSize'], r['YSize']) <= 56]
reg_pool.sort(key=lambda r: (r['ArrowCount'], r['XSize'] * r['YSize']))
print(f"Eligible regular pool (>=42 arr, dim <= 56): {len(reg_pool)}", flush=True)
assert len(reg_pool) >= len(unfilled_slots), "Not enough regular candidates!"

K = len(unfilled_slots)
for k, slot_i in enumerate(unfilled_slots):
    lvl_num = slot_i + 1
    progress = k / (K - 1)
    target_arr = int(round(44 + (245 - 44) * (progress ** 1.15)))
    
    # Binary/linear search for best candidate closest to target_arr
    best_cand = None
    best_diff = 999999
    best_idx = -1
    for idx_c, cand in enumerate(reg_pool):
        diff = abs(cand['ArrowCount'] - target_arr)
        if diff < best_diff:
            best_diff = diff
            best_cand = cand
            best_idx = idx_c
            if diff == 0:
                break
                
    reg_pool.pop(best_idx)
    used_campaign_hashes.add(best_cand['Hash'])
    campaign_2000[slot_i] = {
        'Level': lvl_num,
        'XSize': best_cand['XSize'],
        'YSize': best_cand['YSize'],
        'MaxHearts': 3,
        'Hash': best_cand['Hash'],
        'ArrowCount': best_cand['ArrowCount'],
        'Arrows': best_cand['Arrows']
    }

print("All 2,000 campaign slots filled with 100% unique puzzles!", flush=True)

# Validate campaign
print("Validating campaign levels...", flush=True)
hashes_seen = set()
for idx, lvl in enumerate(campaign_2000):
    assert lvl is not None, f"Level {idx+1} is None!"
    assert len(lvl['Arrows']) == lvl['ArrowCount'], f"Arrow count mismatch at {idx+1}"
    assert solve_level(lvl), f"Level {idx+1} is not solvable!"
    assert max(lvl['XSize'], lvl['YSize']) <= 58, f"Level {idx+1} exceeded dimension 58: {lvl['XSize']}x{lvl['YSize']}"
    if not lvl.get('IsMilestoneShape', False):
        assert lvl['Hash'] not in hashes_seen, f"Duplicate regular level at {idx+1}: {lvl['Hash']}"
        hashes_seen.add(lvl['Hash'])

print("Campaign verification passed! 100% solvable, 0 duplicate regular levels, all grids <= 58!", flush=True)

pack00 = campaign_2000[:1000]
pack01 = campaign_2000[1000:2000]

p00_path = "arrow_native/app/src/main/assets/levels/level_pack_00.dat"
p01_path = "arrow_native/app/src/main/assets/levels/level_pack_01.dat"

with open(p00_path, "wb") as f:
    f.write(encrypt_pack(pack00))
with open(p01_path, "wb") as f:
    f.write(encrypt_pack(pack01))

print(f"Created {p00_path}: {os.path.getsize(p00_path) / 1024:.1f} KB", flush=True)
print(f"Created {p01_path}: {os.path.getsize(p01_path) / 1024:.1f} KB", flush=True)

print("\n=== First 50 Levels Audit ===", flush=True)
for i in range(50):
    l = campaign_2000[i]
    is_m = l.get('IsMilestoneShape', False)
    kind = f"[DYNAMIC SHAPE MILESTONE]" if is_m else "regular"
    print(f"Level {i+1:2d}: {l['ArrowCount']:3d} arrows ({l['XSize']:2d}x{l['YSize']:2d}) - {kind} - Hash={l['Hash'][:16]}", flush=True)

print("\n=== 100-Level Blocks Arrow Summary ===", flush=True)
for b in range(20):
    start = b * 100
    end = start + 100
    chunk = campaign_2000[start:end]
    reg_chunk = [l['ArrowCount'] for l in chunk if not l.get('IsMilestoneShape', False)]
    shape_chunk = [l['ArrowCount'] for l in chunk if l.get('IsMilestoneShape', False)]
    print(f"Levels {start+1:4d}-{end:4d}: Regular min={min(reg_chunk):3d}, avg={sum(reg_chunk)/len(reg_chunk):5.1f}, max={max(reg_chunk):3d} | Shapes min={min(shape_chunk):3d}, avg={sum(shape_chunk)/len(shape_chunk):5.1f}, max={max(shape_chunk):3d}", flush=True)
