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

print("==========================================================", flush=True)
print("  STEP 1: INGESTING CANDIDATE LEVELS                      ", flush=True)
print("==========================================================", flush=True)
t0 = time.time()
raw_candidates = []

# Scratch old/new levels
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
print("  STEP 2: GEOMETRIC DEDUPLICATION & SOLVABILITY FILTER    ", flush=True)
print("==========================================================", flush=True)
seen_fps = set()
unique_candidates = []

for c in raw_candidates:
    xs = c['XSize']
    ys = c['YSize']
    arrs = c['Arrows']
    fp_items = []
    for a in arrs:
        idxs = a['Indices']
        fp_items.append((idxs[0], idxs[-1], a['Dx'], a['Dy'], a['BendCount']))
    fp = (xs, ys, tuple(sorted(fp_items)))
    if fp not in seen_fps:
        seen_fps.add(fp)
        unique_candidates.append(c)

print(f"Unique geometric candidates: {len(unique_candidates)}", flush=True)

solvable_regular = []
solvable_shapes = []

landmark_keywords = ['sphinx', 'taj', 'tower', 'opera', 'arc', 'starry', 'pirate', 'chest', 'shape', 'gallery', 'trophy', 'cat', 'horse', 'robot', 'car', 'falcon', 'fish', 'guitar', 'camera', 'crown', 'butterfly']

t1 = time.time()
print("Validating solvability and classifying shapes...", flush=True)
for idx, c in enumerate(unique_candidates):
    xs = c['XSize']
    ys = c['YSize']
    arrs = c['Arrows']
    narr = len(arrs)
    h = c.get('Hash', '')
    
    occupied = set()
    for a in arrs:
        for p in a['Indices']:
            occupied.add(p)
    occ = len(occupied) / max(1, xs * ys)
    
    # Refined shape classifier:
    # True shapes have recognizable silhouettes: lower occupancy, or explicit landmark names
    is_shape = (
        any(k in h.lower() for k in landmark_keywords) or
        (occ <= 0.60 and xs >= 20 and ys >= 20 and narr >= 16)
    )
    
    if solve_level(c):
        c['Occupancy'] = round(occ, 3)
        c['ArrowCount'] = narr
        if is_shape:
            solvable_shapes.append(c)
        else:
            solvable_regular.append(c)

print(f"Solvable unique regular: {len(solvable_regular)}", flush=True)
print(f"Solvable unique shapes:  {len(solvable_shapes)}", flush=True)
print(f"Solvability validation completed in {time.time()-t1:.1f}s", flush=True)

print("==========================================================", flush=True)
print("  STEP 3: BUILDING CURATED DYNAMIC SHAPE PACK (6 TIERS)   ", flush=True)
print("==========================================================", flush=True)
tier_defs = [
    (0, "Tier 0 (L5-50)", 54, 100, 160),
    (1, "Tier 1 (L55-200)", 70, 125, 200),
    (2, "Tier 2 (L205-500)", 90, 150, 240),
    (3, "Tier 3 (L505-1000)", 120, 190, 200),
    (4, "Tier 4 (L1005-1500)", 160, 260, 150),
    (5, "Tier 5 (L1505-2000)", 220, 437, 150)
]

used_shape_hashes = set()
shape_pack_list = []

for t_idx, t_name, min_arr, max_arr, quota in tier_defs:
    pool = [s for s in solvable_shapes if min_arr <= s['ArrowCount'] <= max_arr and s['Hash'] not in used_shape_hashes]
    pool.sort(key=lambda s: (0 if any(k in s['Hash'].lower() for k in landmark_keywords) else 1, s['ArrowCount']))
    selected = pool[:quota]
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
    print(f"{t_name}: Selected {len(selected)} unique shapes (targets {min_arr}-{max_arr} arrows)", flush=True)

print(f"Total dynamic shapes in Shape Pack: {len(shape_pack_list)}", flush=True)

# Save shape_pack.dat
shape_pack_path = "arrow_native/app/src/main/assets/levels/shape_pack.dat"
with open(shape_pack_path, "wb") as f:
    f.write(encrypt_pack(shape_pack_list))
print(f"Encrypted shape_pack.dat created: {os.path.getsize(shape_pack_path) / 1024:.1f} KB", flush=True)

print("==========================================================", flush=True)
print("  STEP 4: BUILDING 2,000 REGULAR CAMPAIGN LEVELS          ", flush=True)
print("==========================================================", flush=True)
# Target progression for regular levels:
# First 30 levels:
# L1-4: 4, 4, 5, 7
# L6-9: 12, 14, 16, 18
# L11-14: 18, 21, 24, 27
# L16-19: 25, 28, 31, 34
# L21-24: 32, 35, 38, 41
# L26-29: 38, 41, 44, 48

early_targets = {
    1: 4, 2: 4, 3: 5, 4: 7,
    6: 12, 7: 14, 8: 16, 9: 18,
    11: 18, 12: 21, 13: 24, 14: 27,
    16: 25, 17: 28, 18: 31, 19: 34,
    21: 32, 22: 35, 23: 38, 24: 41,
    26: 38, 27: 41, 28: 44, 29: 48
}

def get_regular_floor(lvl_num):
    if lvl_num <= 4: return 4
    if lvl_num <= 9: return 12
    if lvl_num <= 14: return 18
    if lvl_num <= 19: return 25
    if lvl_num <= 24: return 32
    if lvl_num <= 29: return 38
    if lvl_num <= 50: return 42
    if lvl_num <= 100: return 50
    if lvl_num <= 250: return 55
    if lvl_num <= 500: return 65
    if lvl_num <= 1000: return 80
    if lvl_num <= 1500: return 110
    if lvl_num <= 1800: return 140
    return 160

campaign_2000 = [None] * 2000
used_campaign_hashes = set()

# Fill early 30 regular levels
for lvl_num, target_arr in sorted(early_targets.items()):
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

# Fill milestone fallback shapes (every 5th level)
shape_idx_by_tier = defaultdict(int)
for lvl_num in range(5, 2001, 5):
    if lvl_num <= 50: t_idx = 0
    elif lvl_num <= 200: t_idx = 1
    elif lvl_num <= 500: t_idx = 2
    elif lvl_num <= 1000: t_idx = 3
    elif lvl_num <= 1500: t_idx = 4
    else: t_idx = 5
    
    tier_shapes = [s for s in shape_pack_list if s['Tier'] == t_idx]
    s_obj = tier_shapes[shape_idx_by_tier[t_idx] % len(tier_shapes)]
    shape_idx_by_tier[t_idx] += 1
    
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

# Fill remaining regular levels strictly WITHOUT DUPLICATES
# Sort available regular by arrow count
valid_regular = [r for r in solvable_regular if r['Hash'] not in used_campaign_hashes and r['ArrowCount'] >= 42]
valid_regular.sort(key=lambda r: (r['ArrowCount'], r['XSize'] * r['YSize']))

# Index remaining unfilled slots
unfilled_slots = [i for i in range(2000) if campaign_2000[i] is None]
print(f"Unfilled regular slots: {len(unfilled_slots)}, available candidates (>=42 arr): {len(valid_regular)}", flush=True)

assert len(valid_regular) >= len(unfilled_slots), "Not enough regular candidates!"

# Smoothly allocate distinct candidates across unfilled slots
N = len(valid_regular)
K = len(unfilled_slots)
step = (N - 1) / (K - 1)

for k, slot_i in enumerate(unfilled_slots):
    lvl_num = slot_i + 1
    cand_i = int(k * step)
    chosen = valid_regular[cand_i]
    assert chosen['Hash'] not in used_campaign_hashes, f"Duplicate at slot {lvl_num}!"
    used_campaign_hashes.add(chosen['Hash'])
    campaign_2000[slot_i] = {
        'Level': lvl_num,
        'XSize': chosen['XSize'],
        'YSize': chosen['YSize'],
        'MaxHearts': 3,
        'Hash': chosen['Hash'],
        'ArrowCount': chosen['ArrowCount'],
        'Arrows': chosen['Arrows']
    }

print("All 2,000 campaign slots filled with 100% unique puzzles!", flush=True)

# Validate campaign
print("Validating campaign levels...", flush=True)
hashes_seen = set()
for idx, lvl in enumerate(campaign_2000):
    assert lvl is not None, f"Level {idx+1} is None!"
    assert len(lvl['Arrows']) == lvl['ArrowCount'], f"Arrow count mismatch at {idx+1}"
    assert solve_level(lvl), f"Level {idx+1} is not solvable!"
    if not lvl.get('IsMilestoneShape', False):
        assert lvl['Hash'] not in hashes_seen, f"Duplicate regular level at {idx+1}: {lvl['Hash']}"
        hashes_seen.add(lvl['Hash'])

print("Campaign verification passed! 100% solvable, 0 duplicate regular levels!", flush=True)

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

print("\n=== First 35 Levels Audit ===", flush=True)
for i in range(35):
    l = campaign_2000[i]
    is_m = l.get('IsMilestoneShape', False)
    kind = f"[DYNAMIC SHAPE MILESTONE]" if is_m else "regular"
    print(f"Level {i+1:2d}: {l['ArrowCount']:3d} arrows ({l['XSize']:2d}x{l['YSize']:2d}) - {kind} - Hash={l['Hash'][:16]}", flush=True)

print("\n=== 100-Level Blocks Arrow Summary ===", flush=True)
for b in range(20):
    chunk = campaign_2000[b*100 : (b+1)*100]
    c_counts = [l['ArrowCount'] for l in chunk]
    print(f"Levels {b*100+1:4d}-{ (b+1)*100:4d}: min {min(c_counts):3d}, avg {sum(c_counts)/len(c_counts):5.1f}, max {max(c_counts):3d}", flush=True)
