import os
import sys
import json
import gzip
import re
import glob
from collections import defaultdict
import UnityPy

sys.stdout.reconfigure(line_buffering=True, encoding='utf-8')
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

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

def get_geom_signature(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrs = lvl['Arrows']
    fp_items = []
    for a in arrs:
        idxs = a['Indices']
        fp_items.append((idxs[0], idxs[-1], a['Dx'], a['Dy'], a['BendCount']))
    return (xs, ys, tuple(sorted(fp_items)))

print("--- Testing Category 1: Existing 1K Levels ---")
old_files = glob.glob('scratch/old_levels/arrow_native/app/src/main/assets/levels/Level_*.json')
existing_1k_raw = []
for p in old_files:
    try:
        with open(p, 'r', encoding='utf-8') as f:
            d = json.load(f)
            d['Source'] = os.path.basename(p)
            d['Hash'] = 'OLD_' + os.path.basename(p).replace('.json', '')
            existing_1k_raw.append(d)
    except:
        pass

print(f"Total existing 1K raw files: {len(existing_1k_raw)}")

# Deduplicate
seen_1k = set()
existing_1k_unique = []
for lvl in existing_1k_raw:
    sig = get_geom_signature(lvl)
    if sig not in seen_1k:
        seen_1k.add(sig)
        existing_1k_unique.append(lvl)

print(f"Unique existing 1K geometries: {len(existing_1k_unique)}")

# Solvability
existing_1k_solvable = [lvl for lvl in existing_1k_unique if solve_level(lvl)]
print(f"Solvable existing 1K levels: {len(existing_1k_solvable)}")
existing_1k_solvable.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))
print(f"Arrow counts: min={len(existing_1k_solvable[0]['Arrows'])}, max={len(existing_1k_solvable[-1]['Arrows'])}")

print("\n--- Loading Unity Bundles ---")
config_bundle_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(config_bundle_path, 'rb') as f:
    f.seek(32)
    env_cfg = UnityPy.load(f.read())

unity_tables = defaultdict(list)
for obj in env_cfg.objects:
    if obj.type.name == 'TextAsset':
        data = obj.read()
        name = getattr(data, 'm_Name', '')
        b = data.m_Script.encode('utf-8', 'surrogateescape')
        hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
        if hashes:
            unity_tables[name] = hashes

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

unity_raw_map = {}
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
            unity_raw_map[name] = {
                'XSize': xs,
                'YSize': ys,
                'Arrows': arrows_out,
                'MaxHearts': raw.get('MaxHearts', 3),
                'Hash': name,
                'RawName': name
            }
        except:
            pass

print(f"Total Unity assets extracted: {len(unity_raw_map)}")

print("\n--- Testing Category 2: New Unity Normal Levels ---")
# Only normal levels from tables containing 'Normal' or 'Guide' (excluding 'Shape', 'DC', 'Tarot', 'Hard', 'Nightmare', etc.)
normal_hashes = set()
for tname, hashes in unity_tables.items():
    t_low = tname.lower()
    if 'shape' in t_low or 'dc' in t_low or 'daily' in t_low or 'tarot' in t_low or 'nightmare' in t_low:
        continue
    # Exclude Hard / SuperHard unless it's explicitly Normal
    if 'superhard' in t_low or 'hard' in t_low:
        continue
    if 'normal' in t_low or 'guide' in t_low:
        normal_hashes.update(hashes)

print(f"Total normal hashes from normal tables: {len(normal_hashes)}")

# Also exclude any level whose asset name is a known shape
landmark_keywords = ['sphinx', 'taj', 'tower', 'opera', 'arc', 'starry', 'pirate', 'chest', 'shape', 'gallery', 'trophy', 'cat', 'horse', 'robot', 'car', 'falcon', 'fish', 'guitar', 'camera', 'crown', 'butterfly', 'heart', 'star', 'bird', 'flower', 'apple', 'anchor', 'diamond', 'sushi', 'stoneidol', 'thinker', 'sofa', 'zhentan']

normal_candidates = []
for h in normal_hashes:
    if h in unity_raw_map:
        lvl = unity_raw_map[h]
        name = lvl['RawName'].lower()
        if any(k in name for k in landmark_keywords):
            continue
        normal_candidates.append(crop_level(lvl))

seen_normal = set()
unity_normal_unique = []
for lvl in normal_candidates:
    sig = get_geom_signature(lvl)
    if sig not in seen_normal:
        seen_normal.add(sig)
        unity_normal_unique.append(lvl)

print(f"Unique Unity Normal candidates: {len(unity_normal_unique)}")
unity_normal_solvable = [lvl for lvl in unity_normal_unique if solve_level(lvl)]
print(f"Solvable Unity Normal levels: {len(unity_normal_solvable)}")
unity_normal_solvable.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))
print(f"Arrow counts: min={len(unity_normal_solvable[0]['Arrows'])}, max={len(unity_normal_solvable[-1]['Arrows'])}")

print("\n--- Testing Category 3: Shape Puzzle Levels ---")
# From Unity:
# 1. Hashes in Shape tables
# 2. Named pictorial shapes
# 3. Low occupancy silhouette shapes (occ <= 0.65, xs>=18, ys>=18, narr>=12)
shape_hashes = set()
for tname, hashes in unity_tables.items():
    if 'shape' in tname.lower():
        shape_hashes.update(hashes)

print(f"Unity Shape table hashes: {len(shape_hashes)}")

unity_shape_candidates = []
for name, lvl in unity_raw_map.items():
    h = lvl['Hash']
    name_low = name.lower()
    is_in_shape_tables = (h in shape_hashes)
    is_named_pictorial = (
        not re.match(r'^[0-9a-fA-F]{16}$', name) and
        not any(p in name for p in ['pipes_', 'hole_', 'reason_', 'Level_', 'NewLevel_'])
    )
    is_landmark = any(k in name_low for k in landmark_keywords)
    
    xs, ys = lvl['XSize'], lvl['YSize']
    arrs = lvl['Arrows']
    occupied = set(p for a in arrs for p in a['Indices'])
    occ = len(occupied) / max(1, xs * ys)
    is_silhouette = (occ <= 0.65 and xs >= 18 and ys >= 18 and len(arrs) >= 12)
    
    if is_in_shape_tables or is_named_pictorial or is_landmark or is_silhouette:
        cropped = crop_level(lvl)
        cropped['IsShape'] = True
        unity_shape_candidates.append(cropped)

# Also check shape levels from Existing 1K levels
existing_shape_candidates = []
for lvl in existing_1k_solvable:
    xs, ys = lvl['XSize'], lvl['YSize']
    arrs = lvl['Arrows']
    occupied = set(p for a in arrs for p in a['Indices'])
    occ = len(occupied) / max(1, xs * ys)
    # Shapes in 1K: low occupancy silhouette
    if occ <= 0.65 and xs >= 18 and ys >= 18 and len(arrs) >= 12:
        cropped = crop_level(lvl)
        cropped['IsShape'] = True
        existing_shape_candidates.append(cropped)

print(f"Unity Shape candidates: {len(unity_shape_candidates)}")
print(f"Existing 1K Shape candidates: {len(existing_shape_candidates)}")

# Deduplicate combined shapes
seen_shapes = set()
all_shapes_unique = []
for lvl in unity_shape_candidates + existing_shape_candidates:
    sig = get_geom_signature(lvl)
    if sig not in seen_shapes:
        seen_shapes.add(sig)
        all_shapes_unique.append(lvl)

print(f"Combined unique shapes: {len(all_shapes_unique)}")
all_shapes_solvable = [lvl for lvl in all_shapes_unique if solve_level(lvl)]
print(f"Combined solvable shapes: {len(all_shapes_solvable)}")
all_shapes_solvable.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))
print(f"Shape arrow counts: min={len(all_shapes_solvable[0]['Arrows'])}, max={len(all_shapes_solvable[-1]['Arrows'])}")
