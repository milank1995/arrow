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

sys.stdout.reconfigure(line_buffering=True, encoding='utf-8')
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'

OUTPUT_DIR = r'arrow_native/app/src/main/assets/levels'

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
print("  STEP 1: INGESTING CANDIDATE LEVELS                      ", flush=True)
print("==========================================================", flush=True)
t0 = time.time()
raw_candidates = []

# 1. Scratch old levels (Level_*.json)
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

# 2. Scratch new levels (NewLevel_*.json)
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

# 3. Unity levels from asset bundle
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
                is_named = (
                    not re.match(r'^[0-9a-fA-F]{16}$', name) and
                    not any(p in name for p in ['pipes_', 'hole_', 'reason_', 'Level_', 'NewLevel_'])
                )
                raw_candidates.append({
                    'XSize': xs,
                    'YSize': ys,
                    'Arrows': arrows_out,
                    'MaxHearts': raw.get('MaxHearts', 3),
                    'Hash': name,
                    'IsNamedShape': is_named,
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
    # Keep original dimensions for specific old milestone shapes if already tight, otherwise crop
    cropped = crop_level(c, padding=1)
    xs = cropped['XSize']
    ys = cropped['YSize']
    arrs = cropped['Arrows']
    if not arrs:
        continue
    
    fp_items = []
    for a in arrs:
        idxs = a['Indices']
        fp_items.append((idxs[0], idxs[-1], a['Dx'], a['Dy'], a['BendCount']))
    fp = (xs, ys, tuple(sorted(fp_items)))
    if fp not in seen_fps:
        seen_fps.add(fp)
        unique_candidates.append(cropped)

print(f"Unique cropped candidates: {len(unique_candidates)} (in {time.time()-t1:.1f}s)", flush=True)

print("==========================================================", flush=True)
print("  STEP 3: SOLVABILITY FILTER & POOL CLASSIFICATION        ", flush=True)
print("==========================================================", flush=True)
t2 = time.time()
solvable_all = []
solvable_shapes = []
solvable_regular = []

landmark_keywords = ['sphinx', 'taj', 'tower', 'opera', 'arc', 'starry', 'pirate', 'chest', 'shape', 'gallery', 'trophy', 'cat', 'horse', 'robot', 'car', 'falcon', 'fish', 'guitar', 'camera', 'crown', 'butterfly', 'heart', 'star', 'bird', 'flower', 'apple', 'anchor', 'diamond', 'sushi', 'stoneidol', 'thinker', 'sofa', 'zhentan']

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
    
    is_named = c.get('IsNamedShape', False)
    is_landmark = any(k in h for k in landmark_keywords)
    is_silhouette_shape = (occ <= 0.65 and xs >= 18 and ys >= 18 and narr >= 12)
    is_giant_grid = (narr >= 280)
    
    if solve_level(c):
        c['Occupancy'] = round(occ, 3)
        c['ArrowCount'] = narr
        solvable_all.append(c)
        
        if is_named or is_landmark or is_silhouette_shape or is_giant_grid:
            solvable_shapes.append(c)
        else:
            solvable_regular.append(c)

print(f"Total verified solvable candidates: {len(solvable_all)}", flush=True)
print(f"Solvable shape candidates:          {len(solvable_shapes)}", flush=True)
print(f"Solvable regular candidates:        {len(solvable_regular)}", flush=True)
print(f"Solvability validation completed in {time.time()-t2:.1f}s", flush=True)

print("==========================================================", flush=True)
print("  STEP 4: CURATING 2,000-LEVEL CAMPAIGN                   ", flush=True)
print("==========================================================", flush=True)
campaign_2000 = [None] * 2000
used_hashes = set()

# A. Exact user-requested early default milestones (Levels 5, 10, 15, 20, 25, 26, 30, 35, 40, 45, 50)
early_milestone_hashes = {
    5:  ('OLD_Level_175', 54),
    10: ('OLD_Level_353', 99),
    15: ('OLD_Level_201', 53),
    20: ('OLD_Level_67',  49),
    25: ('OLD_Level_157', 74),
    26: ('NEW_NewLevel_26', 130), # Cactus showstopper
    30: ('OLD_Level_105', 68),
    35: ('c6814a344fb36cb1', 101),
    40: ('OLD_Level_478', 122),
    45: ('OLD_Level_173', 44),
    50: ('OLD_Level_104', 48)
}

for lvl_num, (target_hash, exp_arr) in early_milestone_hashes.items():
    cands = [c for c in solvable_all if target_hash.lower() in c['Hash'].lower()]
    if not cands:
        # Fallback by arrow count if hash not matched
        cands = [c for c in solvable_shapes if c['ArrowCount'] == exp_arr]
    chosen = cands[0]
    used_hashes.add(chosen['Hash'])
    campaign_2000[lvl_num - 1] = {
        'Level': lvl_num,
        'XSize': chosen['XSize'],
        'YSize': chosen['YSize'],
        'MaxHearts': 3,
        'Hash': chosen['Hash'],
        'ArrowCount': chosen['ArrowCount'],
        'IsMilestoneShape': True,
        'Arrows': chosen['Arrows']
    }
    print(f"L{lvl_num:2d} Early Milestone: {chosen['ArrowCount']} arr, {chosen['XSize']}x{chosen['YSize']}, Hash={chosen['Hash']}")

# B. Curate the pinnacle ~400 arrow grids for the late-game milestones (Levels 1850 to 2000)
# Top largest solvable levels
large_solvable = [c for c in solvable_all if c['ArrowCount'] >= 280 and c['Hash'] not in used_hashes]
large_solvable.sort(key=lambda c: (c['ArrowCount'], c['XSize'] * c['YSize']))
print(f"Available ~300-437 arrow candidate levels: {len(large_solvable)}")

# Place Sphinx at Level 2000
sphinx_cands = [c for c in large_solvable if 'sphinx' in c['Hash'].lower()]
sphinx_obj = sphinx_cands[0] if sphinx_cands else large_solvable[-1]
large_solvable.remove(sphinx_obj)
used_hashes.add(sphinx_obj['Hash'])
campaign_2000[1999] = {
    'Level': 2000,
    'XSize': sphinx_obj['XSize'],
    'YSize': sphinx_obj['YSize'],
    'MaxHearts': 3,
    'Hash': sphinx_obj['Hash'],
    'ArrowCount': sphinx_obj['ArrowCount'],
    'IsMilestoneShape': True,
    'Arrows': sphinx_obj['Arrows']
}
print(f"L2000 Grand Finale: Sphinx ({sphinx_obj['ArrowCount']} arrows, {sphinx_obj['XSize']}x{sphinx_obj['YSize']})")

# Place top large grids into the final milestones (from L1995 backwards)
late_milestones = list(range(1850, 2000, 5)) # 30 milestone slots
# Sort large_solvable ascending so biggest are near the end
late_large = large_solvable[-len(late_milestones):]
for slot_lvl, lvl_obj in zip(late_milestones, late_large):
    used_hashes.add(lvl_obj['Hash'])
    campaign_2000[slot_lvl - 1] = {
        'Level': slot_lvl,
        'XSize': lvl_obj['XSize'],
        'YSize': lvl_obj['YSize'],
        'MaxHearts': 3,
        'Hash': lvl_obj['Hash'],
        'ArrowCount': lvl_obj['ArrowCount'],
        'IsMilestoneShape': True,
        'Arrows': lvl_obj['Arrows']
    }

# C. Fill remaining shape milestones (L55 to L1845, every 5th level)
# Progress smoothly from 50 to 280 arrows
mid_milestones = [l for l in range(55, 1846, 5) if campaign_2000[l - 1] is None]
available_shapes = [s for s in solvable_shapes if s['Hash'] not in used_hashes and 45 <= s['ArrowCount'] <= 280]
available_shapes.sort(key=lambda s: (s['ArrowCount'], s['XSize'] * s['YSize']))

N_mid = len(mid_milestones)
for idx, slot_lvl in enumerate(mid_milestones):
    prog = idx / max(1, N_mid - 1)
    target_arr = int(round(50 + (280 - 50) * (prog ** 1.1)))
    
    # Pick closest
    best_cand = min(available_shapes, key=lambda s: abs(s['ArrowCount'] - target_arr))
    available_shapes.remove(best_cand)
    used_hashes.add(best_cand['Hash'])
    campaign_2000[slot_lvl - 1] = {
        'Level': slot_lvl,
        'XSize': best_cand['XSize'],
        'YSize': best_cand['YSize'],
        'MaxHearts': 3,
        'Hash': best_cand['Hash'],
        'ArrowCount': best_cand['ArrowCount'],
        'IsMilestoneShape': True,
        'Arrows': best_cand['Arrows']
    }

print("All 400 shape milestones + L26 placed successfully!", flush=True)

# D. Early regular levels (L1-4, 6-9, 11-14, 16-19, 21-24, 27-29)
early_regular_targets = {
    1: 4, 2: 4, 3: 4, 4: 7,
    6: 14, 7: 17, 8: 15, 9: 23,
    11: 18, 12: 30, 13: 20, 14: 31,
    16: 28, 17: 19, 18: 34, 19: 24,
    21: 62, 22: 54, 23: 28, 24: 46,
    27: 22, 28: 33, 29: 36
}

for lvl_num, target_arr in sorted(early_regular_targets.items()):
    cands = [r for r in solvable_regular if r['ArrowCount'] == target_arr and r['Hash'] not in used_hashes]
    if not cands:
        cands = [r for r in solvable_all if r['ArrowCount'] == target_arr and r['Hash'] not in used_hashes]
    if not cands:
        cands = [r for r in solvable_all if abs(r['ArrowCount'] - target_arr) <= 2 and r['Hash'] not in used_hashes]
    chosen = cands[0]
    used_hashes.add(chosen['Hash'])
    campaign_2000[lvl_num - 1] = {
        'Level': lvl_num,
        'XSize': chosen['XSize'],
        'YSize': chosen['YSize'],
        'MaxHearts': 3,
        'Hash': chosen['Hash'],
        'ArrowCount': chosen['ArrowCount'],
        'IsMilestoneShape': False,
        'Arrows': chosen['Arrows']
    }

# E. Fill remaining regular levels with smooth, gradual increase from 28 arrows up to ~320+ arrows
unfilled_slots = [i for i in range(2000) if campaign_2000[i] is None]
print(f"Unfilled regular slots: {len(unfilled_slots)}", flush=True)

eligible_reg = [c for c in solvable_all if c['Hash'] not in used_hashes and c['ArrowCount'] >= 25]
eligible_reg.sort(key=lambda c: (c['ArrowCount'], c['XSize'] * c['YSize']))
print(f"Eligible regular candidates: {len(eligible_reg)}", flush=True)
assert len(eligible_reg) >= len(unfilled_slots), "Not enough regular candidates!"

K_reg = len(unfilled_slots)
for k, slot_i in enumerate(unfilled_slots):
    lvl_num = slot_i + 1
    prog = k / max(1, K_reg - 1)
    target_arr = int(round(28 + (330 - 28) * (prog ** 1.25)))
    
    # Pick candidate closest to target_arr
    best_cand = None
    best_diff = 999999
    best_idx = -1
    for idx_c, cand in enumerate(eligible_reg):
        diff = abs(cand['ArrowCount'] - target_arr)
        if diff < best_diff:
            best_diff = diff
            best_cand = cand
            best_idx = idx_c
            if diff == 0:
                break
                
    eligible_reg.pop(best_idx)
    used_hashes.add(best_cand['Hash'])
    campaign_2000[slot_i] = {
        'Level': lvl_num,
        'XSize': best_cand['XSize'],
        'YSize': best_cand['YSize'],
        'MaxHearts': 3,
        'Hash': best_cand['Hash'],
        'ArrowCount': best_cand['ArrowCount'],
        'IsMilestoneShape': False,
        'Arrows': best_cand['Arrows']
    }

print("==========================================================", flush=True)
print("  STEP 5: VALIDATING 100% SOLVABILITY OF ALL 2,000 LEVELS ", flush=True)
print("==========================================================", flush=True)
t_val = time.time()
unsolvable = []
for idx, lvl in enumerate(campaign_2000):
    if not solve_level(lvl):
        unsolvable.append(idx + 1)

print(f"Solvability validation complete in {time.time() - t_val:.2f}s", flush=True)
if unsolvable:
    print(f"ERROR: Found {len(unsolvable)} unsolvable levels: {unsolvable}", flush=True)
    exit(1)
print("ALL 2,000 LEVELS ARE 100% SOLVABLE! Zero deadlocks!", flush=True)

# Progression summary
counts = [c['ArrowCount'] for c in campaign_2000]
print(f"Progression stats: min={min(counts)}, max={max(counts)}")
for start in range(0, 2000, 200):
    end = start + 200
    block = counts[start:end]
    print(f"Levels {start+1:4d} - {end:4d}: min={min(block):2d}, avg={sum(block)/len(block):5.1f}, max={max(block):3d}")

print("==========================================================", flush=True)
print("  STEP 6: ENCRYPTING & WRITING LEVEL PACKS                ", flush=True)
print("==========================================================", flush=True)
chunk_size = 1000
manifest_packs = []

for pack_idx in range(2):
    start = pack_idx * chunk_size
    end = start + chunk_size
    chunk = campaign_2000[start:end]
    
    chunk_json_objs = []
    for offset, lvl in enumerate(chunk):
        lvl_num = start + offset + 1
        chunk_json_objs.append({
            'Level': lvl_num,
            'XSize': lvl['XSize'],
            'YSize': lvl['YSize'],
            'MaxHearts': 3,
            'Hash': lvl.get('Hash', f'AMZ_{lvl_num:05d}'),
            'Arrows': lvl['Arrows']
        })
        
    encrypted_bytes = encrypt_pack(chunk_json_objs)
    out_file = os.path.join(OUTPUT_DIR, f"level_pack_{pack_idx:02d}.dat")
    with open(out_file, 'wb') as f:
        f.write(encrypted_bytes)
    print(f"Wrote {out_file} ({len(chunk_json_objs)} levels, {len(encrypted_bytes):,} bytes)", flush=True)
    
    manifest_packs.append({
        'packIndex': pack_idx,
        'fileName': f"level_pack_{pack_idx:02d}.dat",
        'startLevel': start + 1,
        'endLevel': end,
        'count': len(chunk_json_objs),
        'sizeBytes': len(encrypted_bytes)
    })

manifest_doc = {
    'totalLevels': 2000,
    'chunkSize': 1000,
    'totalPacks': 2,
    'packs': manifest_packs
}

with open(os.path.join(OUTPUT_DIR, "pack_manifest.json"), 'w', encoding='utf-8') as f:
    json.dump(manifest_doc, f, indent=2)

print("==========================================================", flush=True)
print("  STEP 7: GENERATING CAMPAIGN_LEVELS_2000.JSON            ", flush=True)
print("==========================================================", flush=True)

def get_shape_name(lvl_num, h):
    h_low = h.lower()
    if 'sphinx' in h_low:
        return 'The Great Sphinx (狮身人面像)'
    if 'zhentan' in h_low:
        return 'Detective (侦探)'
    if 'towerofpisa' in h_low:
        return 'Tower of Pisa (比萨斜塔)'
    if 'tajmahal' in h_low:
        return 'Taj Mahal (泰姬陵)'
    if 'sydneyopera' in h_low:
        return 'Sydney Opera House (悉尼歌剧院)'
    if 'pirate' in h_low:
        return 'Pirate Ship (海盗船)'
    if 'arc_de_triomphe' in h_low or 'arcde' in h_low:
        return 'Arc de Triomphe (凯旋门)'
    if 'starry' in h_low:
        return 'The Starry Night (星月夜)'
    if 'treasurechest' in h_low:
        return 'Treasure Chest (宝箱)'
    if lvl_num == 26 or 'newlevel_26' in h_low:
        return 'Cactus Shape (仙人掌)'
    if h.startswith('OLD_Level_'):
        num = h.replace('OLD_Level_', '')
        return f'Shape Puzzle (Level_{num})'
    if h.startswith('NEW_NewLevel_'):
        num = h.replace('NEW_NewLevel_', '')
        return f'Shape Puzzle (NewLevel_{num})'
    if h.startswith('reason_'):
        return f'Shape Puzzle ({h[:16]})'
    if len(h) >= 12 and re.match(r'^[0-9a-fA-F]+$', h):
        return f'Shape Puzzle ({h[:12]})'
    return f'Shape Puzzle ({h})'

final_manifest = []
for idx, lvl in enumerate(campaign_2000):
    lvl_num = idx + 1
    narr = lvl['ArrowCount']
    xs = lvl['XSize']
    ys = lvl['YSize']
    h = lvl.get('Hash', '')
    is_shape = lvl.get('IsMilestoneShape', False)
    
    final_manifest.append({
        'level_number': lvl_num,
        'arrow_count': narr,
        'width': xs,
        'height': ys,
        'is_shape_level': is_shape,
        'shape_name': get_shape_name(lvl_num, h) if is_shape else None
    })

with open('campaign_levels_2000.json', 'w', encoding='utf-8') as f:
    json.dump(final_manifest, f, indent=2, ensure_ascii=False)

print(f"Generated campaign_levels_2000.json with {len(final_manifest)} levels!", flush=True)
print("PIPELINE COMPLETED SUCCESSFULLY!", flush=True)
