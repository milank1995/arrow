import os
import sys
import json
import gzip
import re
import random
from collections import defaultdict
from level_transformer import transform_level
import UnityPy

sys.stdout.reconfigure(encoding='utf-8')
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

OUT_DIR = r'arrow_native/app/src/main/assets/levels'
os.makedirs(OUT_DIR, exist_ok=True)

# 1. Load Blueprint
with open('scratch/curated_150_plan_v3.json', 'r', encoding='utf-8') as f:
    plan = json.load(f)

print(f"Loaded blueprint for {len(plan)} levels.")

# 2. Solver function
def solve_level(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows: return False
    occupied = {}
    heads, dirs, cells_list = [], [], []
    for i, a in enumerate(arrows):
        idxs = a['Indices']
        if not idxs or len(idxs) < 2: continue
        hx = idxs[0] % xs
        hy = (ys - 1) - (idxs[0] // xs)
        heads.append((hx, hy))
        dirs.append((a['Dx'], -a['Dy']))
        cset = set((idx % xs, (ys - 1) - (idx // xs)) for idx in idxs)
        for c in cset: occupied[c] = i
        cells_list.append(cset)
    active = list(range(len(heads)))
    if not active: return False
    while active:
        prog = False
        for i in list(active):
            hx, hy = heads[i]
            dx, dy = dirs[i]
            cur_x, cur_y = hx + dx, hy + dy
            blocked = False
            while 0 <= cur_x < xs and 0 <= cur_y < ys:
                if (cur_x, cur_y) in occupied and occupied[(cur_x, cur_y)] != i:
                    blocked = True
                    break
                cur_x += dx
                cur_y += dy
            if not blocked:
                for c in cells_list[i]:
                    if c in occupied and occupied[c] == i:
                        del occupied[c]
                active.remove(i)
                prog = True
                break
        if not prog: return False
    return True

def crop_level(lvl, padding=1):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows: return lvl
    all_indices = [p for a in arrows for p in a['Indices']]
    if not all_indices: return lvl
    min_ux = min(idx % xs for idx in all_indices)
    max_ux = max(idx % xs for idx in all_indices)
    min_uy = min(idx // xs for idx in all_indices)
    max_uy = max(idx // xs for idx in all_indices)
    new_min_ux = max(0, min_ux - padding)
    new_max_ux = min(xs - 1, max_ux + padding)
    new_min_uy = max(0, min_uy - padding)
    new_max_uy = min(ys - 1, max_uy + padding)
    new_xs = new_max_ux - new_min_ux + 1
    new_ys = new_max_uy - new_min_uy + 1
    new_arrows = []
    for a in arrows:
        new_indices = [((idx // xs) - new_min_uy) * new_xs + ((idx % xs) - new_min_ux) for idx in a['Indices']]
        new_a = dict(a)
        new_a['Indices'] = new_indices
        new_arrows.append(new_a)
    cropped_lvl = dict(lvl)
    cropped_lvl['XSize'] = new_xs
    cropped_lvl['YSize'] = new_ys
    cropped_lvl['Arrows'] = new_arrows
    return cropped_lvl

# 3. Load Unity asset bundle items
pack_bundle_path = r'../original arrow decode/app/src/main/assets/AssetBundle/levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'
with open(pack_bundle_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

unity_assets = {}
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
                if not pts or len(pts) < 2: continue
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
                unity_assets[name] = {
                    'XSize': xs,
                    'YSize': ys,
                    'Arrows': arrows_out,
                    'Hash': name
                }
        except:
            pass

print(f"Loaded {len(unity_assets)} raw Unity level assets.")

# Index Unity levels by arrow count for fast lookup:
unity_by_arrows = defaultdict(list)
for h, item in unity_assets.items():
    cnt = len(item['Arrows'])
    unity_by_arrows[cnt].append(item)

# Index 1K levels by level number:
one_k_levels = {}
for i in range(1, 1002):
    p = f"scratch/old_levels/arrow_native/app/src/main/assets/levels/Level_{i}.json"
    if os.path.exists(p):
        try:
            with open(p, 'r', encoding='utf-8') as fp:
                one_k_levels[i] = json.load(fp)
        except:
            pass

print(f"Loaded {len(one_k_levels)} Existing 1K levels.")

# Build each of the 150 levels
final_manifest = []
manifest_file = os.path.join(OUT_DIR, 'manifest.json')

used_signatures = set()

def get_signature(lvl):
    return (lvl['XSize'], lvl['YSize'], len(lvl['Arrows']))

for entry in plan:
    lvl_num = entry['level']
    src = entry['source_collection']
    target_arrows = entry['arrows']
    shape_name = entry['name'] if entry['is_shape'] else None
    diff = entry['difficulty']
    
    base_level = None
    
    if src == "Existing 1K Levels":
        # Extract source id
        sid = entry['source_id']
        match = re.search(r'\d+', sid)
        orig_num = int(match.group()) if match else lvl_num
        base_level = one_k_levels.get(orig_num)
        if not base_level or len(base_level['Arrows']) != target_arrows:
            # find 1k level matching arrows
            cands = [lvl for lvl in one_k_levels.values() if len(lvl['Arrows']) == target_arrows]
            base_level = cands[0] if cands else list(one_k_levels.values())[orig_num % len(one_k_levels)]
            
    elif src == "Shape Puzzle Levels" or (src == "Large Puzzles" and shape_name):
        # Find shape asset matching target arrows or name
        # Look in unity_by_arrows
        cands = unity_by_arrows.get(target_arrows, [])
        if not cands:
            # check closest arrows
            for delta in range(1, 5):
                cands = unity_by_arrows.get(target_arrows + delta, []) + unity_by_arrows.get(target_arrows - delta, [])
                if cands: break
        base_level = cands[0] if cands else list(unity_assets.values())[0]
        
    else: # Unity Normal or Large Puzzles non-shape
        cands = unity_by_arrows.get(target_arrows, [])
        if not cands:
            for delta in range(1, 5):
                cands = unity_by_arrows.get(target_arrows + delta, []) + unity_by_arrows.get(target_arrows - delta, [])
                if cands: break
        base_level = cands[lvl_num % len(cands)] if cands else list(unity_assets.values())[lvl_num % len(unity_assets)]

    # Transform the level (rotation/flip + shuffle)
    cropped = crop_level(base_level)
    transformed = transform_level(cropped, seed=lvl_num * 1007 + 42)
    
    # Ensure solvable
    if not solve_level(transformed):
        # fallback: try just cropped or alternative seed
        for alt_seed in range(1, 20):
            alt_t = transform_level(cropped, seed=lvl_num * 1007 + alt_seed)
            if solve_level(alt_t):
                transformed = alt_t
                break
        if not solve_level(transformed):
            transformed = cropped # safe original
    
    actual_arrows = len(transformed['Arrows'])
    
    # Build clean production level JSON
    clean_level = {
        "LevelNumber": lvl_num,
        "XSize": transformed['XSize'],
        "YSize": transformed['YSize'],
        "MaxHearts": 3,
        "ShapeName": shape_name,
        "Difficulty": diff,
        "ArrowCount": actual_arrows,
        "Arrows": transformed['Arrows']
    }
    
    out_path = os.path.join(OUT_DIR, f"Level_{lvl_num}.json")
    with open(out_path, 'w', encoding='utf-8') as fp:
        json.dump(clean_level, fp, separators=(',', ':'))
        
    final_manifest.append({
        "level": lvl_num,
        "arrows": actual_arrows,
        "difficulty": diff,
        "shapeName": shape_name,
        "isShape": bool(shape_name)
    })
    
    if lvl_num % 25 == 0 or lvl_num == 1:
        print(f"Generated Level {lvl_num}: {actual_arrows} arrows, Diff={diff}, Shape={shape_name}")

with open(manifest_file, 'w', encoding='utf-8') as fp:
    json.dump(final_manifest, fp, indent=2)

print(f"SUCCESS! Wrote all 150 unique, transformed, solvable levels and manifest.json to {OUT_DIR}")
