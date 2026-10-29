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

OUTPUT_BASE = r'arrow_native/app/src/main/assets/levels'

def encrypt_pack(json_obj):
    raw_json = json.dumps(json_obj, separators=(',', ':')).encode('utf-8')
    gzipped = gzip.compress(raw_json, compresslevel=9)
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

def get_geom_signature(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrs = lvl['Arrows']
    fp_items = []
    for a in arrs:
        idxs = a['Indices']
        fp_items.append((idxs[0], idxs[-1], a['Dx'], a['Dy'], a['BendCount']))
    return (xs, ys, tuple(sorted(fp_items)))

def write_category_packs(subfolder_name, level_list, chunk_size=1000):
    dest_dir = os.path.join(OUTPUT_BASE, subfolder_name)
    os.makedirs(dest_dir, exist_ok=True)
    
    total = len(level_list)
    num_chunks = (total + chunk_size - 1) // chunk_size
    manifest_packs = []
    
    print(f"\nWriting {total} levels to {dest_dir} in {num_chunks} chunks...")
    for pack_idx in range(num_chunks):
        start = pack_idx * chunk_size
        end = min(start + chunk_size, total)
        chunk = level_list[start:end]
        
        chunk_json_objs = []
        for offset, lvl in enumerate(chunk):
            lvl_num = start + offset + 1
            chunk_json_objs.append({
                'Level': lvl_num,
                'XSize': lvl['XSize'],
                'YSize': lvl['YSize'],
                'MaxHearts': lvl.get('MaxHearts', 3),
                'Hash': lvl.get('Hash', f'{subfolder_name}_{lvl_num:05d}'),
                'Arrows': lvl['Arrows']
            })
            
        encrypted_bytes = encrypt_pack(chunk_json_objs)
        pack_file_name = f"level_pack_{pack_idx:02d}.dat"
        out_file = os.path.join(dest_dir, pack_file_name)
        with open(out_file, 'wb') as f:
            f.write(encrypted_bytes)
            
        manifest_packs.append({
            'packIndex': pack_idx,
            'fileName': pack_file_name,
            'startLevel': start + 1,
            'endLevel': end,
            'count': len(chunk_json_objs),
            'sizeBytes': len(encrypted_bytes)
        })
        print(f"  Wrote {pack_file_name}: {len(chunk_json_objs)} levels ({len(encrypted_bytes):,} bytes)")
        
    manifest = {
        'category': subfolder_name,
        'totalLevels': total,
        'chunkSize': chunk_size,
        'totalPacks': num_chunks,
        'packs': manifest_packs
    }
    
    manifest_path = os.path.join(dest_dir, "pack_manifest.json")
    with open(manifest_path, 'w', encoding='utf-8') as f:
        json.dump(manifest, f, indent=2)
    print(f"  Saved manifest: {manifest_path}")

print("==========================================================")
print("  STEP 1: INGESTING EXISTING 1K LEVELS                    ")
print("==========================================================")
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

print(f"Ingested {len(existing_1k_raw)} existing 1K files.")

# Deduplicate & crop
seen_1k = set()
existing_1k_cropped = []
for lvl in existing_1k_raw:
    c = crop_level(lvl)
    sig = get_geom_signature(c)
    if sig not in seen_1k:
        seen_1k.add(sig)
        existing_1k_cropped.append(c)

print(f"Unique cropped existing 1K geometries: {len(existing_1k_cropped)}")
existing_1k_solvable = [lvl for lvl in existing_1k_cropped if solve_level(lvl)]
print(f"100% solvable existing 1K levels: {len(existing_1k_solvable)}")

# Sort from smallest to largest arrow count (then by grid area)
existing_1k_solvable.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))
print(f"Sorted 1K arrow range: {len(existing_1k_solvable[0]['Arrows'])} to {len(existing_1k_solvable[-1]['Arrows'])}")

print("==========================================================")
print("  STEP 2: INGESTING UNITY NORMAL & SHAPE LEVELS           ")
print("==========================================================")
config_bundle_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(config_bundle_path, 'rb') as f:
    f.seek(32)
    env_cfg = UnityPy.load(f.read())

unity_tables = defaultdict(list)
for obj in env_cfg.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
        if hashes:
            unity_tables[name] = hashes

print(f"Loaded {len(unity_tables)} config tables.")

# Identify shape table hashes
shape_table_hashes = set()
for tname, hashes in unity_tables.items():
    if 'shape' in tname.lower():
        shape_table_hashes.update(hashes)

# Identify daily challenge hashes
daily_table_hashes = set()
for tname, hashes in unity_tables.items():
    if 'dc' in tname.lower() or 'daily' in tname.lower():
        daily_table_hashes.update(hashes)

# Identify special level hashes (Tarot, Nightmare, SuperHard, etc.)
special_table_hashes = set()
for tname, hashes in unity_tables.items():
    t_low = tname.lower()
    if any(k in t_low for k in ['tarot', 'nightmare', 'superhard', 'hard']):
        special_table_hashes.update(hashes)

# Identify normal hashes (from NormalLevels* and GuideLevels*)
normal_table_hashes = set()
for tname, hashes in unity_tables.items():
    t_low = tname.lower()
    # Exclude shape, daily, tarot, nightmare, superhard, hard
    if any(k in t_low for k in ['shape', 'dc', 'daily', 'tarot', 'nightmare', 'superhard', 'hard']):
        continue
    if 'normal' in t_low or 'guide' in t_low:
        normal_table_hashes.update(hashes)

print(f"Normal hashes: {len(normal_table_hashes)}, Shape table hashes: {len(shape_table_hashes)}, Daily hashes: {len(daily_table_hashes)}")

# Ingest all Unity assets
with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

unity_assets = {}
landmark_keywords = ['sphinx', 'taj', 'tower', 'opera', 'arc', 'starry', 'pirate', 'chest', 'shape', 'gallery', 'trophy', 'cat', 'horse', 'robot', 'car', 'falcon', 'fish', 'guitar', 'camera', 'crown', 'butterfly', 'heart', 'star', 'bird', 'flower', 'apple', 'anchor', 'diamond', 'sushi', 'stoneidol', 'thinker', 'sofa', 'zhentan']

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
                name_low = name.lower()
                is_named = (
                    not re.match(r'^[0-9a-fA-F]{16}$', name) and
                    not any(p in name for p in ['pipes_', 'hole_', 'reason_', 'Level_', 'NewLevel_'])
                )
                is_landmark = any(k in name_low for k in landmark_keywords)
                unity_assets[name] = {
                    'XSize': xs,
                    'YSize': ys,
                    'Arrows': arrows_out,
                    'MaxHearts': raw.get('MaxHearts', 3),
                    'Hash': name,
                    'RawName': name,
                    'IsNamed': is_named,
                    'IsLandmark': is_landmark
                }
        except:
            pass

print(f"Total Unity level assets extracted: {len(unity_assets)}")

print("==========================================================")
print("  STEP 3: CURATING NEW UNITY NORMAL LEVELS                ")
print("==========================================================")
# Exclude: shape tables, named pictorial shapes, landmark keywords, daily tables, special tables
unity_normal_raw = []
for h in normal_table_hashes:
    if h in unity_assets:
        item = unity_assets[h]
        if item['IsNamed'] or item['IsLandmark']:
            continue
        if h in shape_table_hashes or h in daily_table_hashes or h in special_table_hashes:
            continue
        unity_normal_raw.append(crop_level(item))

# Deduplicate
seen_normal = set()
unity_normal_unique = []
for lvl in unity_normal_raw:
    sig = get_geom_signature(lvl)
    if sig not in seen_normal:
        seen_normal.add(sig)
        unity_normal_unique.append(lvl)

print(f"Unique cropped normal candidates: {len(unity_normal_unique)}")
unity_normal_solvable = [lvl for lvl in unity_normal_unique if solve_level(lvl)]
print(f"100% solvable Unity Normal levels: {len(unity_normal_solvable)}")

# Sort from smallest to largest arrow count
unity_normal_solvable.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))
print(f"Sorted Unity Normal arrow range: {len(unity_normal_solvable[0]['Arrows'])} to {len(unity_normal_solvable[-1]['Arrows'])}")

print("==========================================================")
print("  STEP 4: CURATING SHAPE PUZZLE LEVELS                    ")
print("==========================================================")
# 1. Shapes from Unity:
# - All levels in shape_table_hashes
# - All named pictorial shapes & landmark shapes
# - Low occupancy silhouettes (occ <= 0.65, xs>=18, ys>=18, narr>=12)
unity_shapes = []
for name, item in unity_assets.items():
    h = item['Hash']
    in_shape_table = (h in shape_table_hashes)
    is_named = item['IsNamed'] or item['IsLandmark']
    
    xs, ys = item['XSize'], item['YSize']
    arrs = item['Arrows']
    occupied = set(p for a in arrs for p in a['Indices'])
    occ = len(occupied) / max(1, xs * ys)
    is_silhouette = (occ <= 0.65 and xs >= 18 and ys >= 18 and len(arrs) >= 12)
    
    if in_shape_table or is_named or is_silhouette:
        cropped = crop_level(item)
        cropped['IsShape'] = True
        unity_shapes.append(cropped)

print(f"Unity shape candidates: {len(unity_shapes)}")

# 2. Shapes from Existing 1K:
existing_shapes = []
for lvl in existing_1k_solvable:
    xs, ys = lvl['XSize'], lvl['YSize']
    arrs = lvl['Arrows']
    occupied = set(p for a in arrs for p in a['Indices'])
    occ = len(occupied) / max(1, xs * ys)
    if occ <= 0.65 and xs >= 18 and ys >= 18 and len(arrs) >= 12:
        c = dict(lvl)
        c['IsShape'] = True
        existing_shapes.append(c)

print(f"Existing 1K shape candidates: {len(existing_shapes)}")

# Combine & deduplicate against each other
seen_shapes = set()
combined_shapes_unique = []
for s in unity_shapes + existing_shapes:
    sig = get_geom_signature(s)
    if sig not in seen_shapes:
        seen_shapes.add(sig)
        if solve_level(s):
            combined_shapes_unique.append(s)

print(f"Combined unique 100% solvable shapes: {len(combined_shapes_unique)}")

# Sort from smallest to largest arrow count (then grid area)
combined_shapes_unique.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))
print(f"Sorted Shape arrow range: {len(combined_shapes_unique[0]['Arrows'])} to {len(combined_shapes_unique[-1]['Arrows'])}")

print("==========================================================")
print("  STEP 5: PACKAGING & ENCRYPTING THREE TEST CATEGORIES    ")
print("==========================================================")
# Category 1: test_existing_1k
write_category_packs("test_existing_1k", existing_1k_solvable, chunk_size=1000)

# Category 2: test_unity_normal
write_category_packs("test_unity_normal", unity_normal_solvable, chunk_size=1000)

# Category 3: test_shape_puzzles
write_category_packs("test_shape_puzzles", combined_shapes_unique, chunk_size=1000)

print("==========================================================")
print("  STEP 6: GENERATING MANIFEST JSON FILES                  ")
print("==========================================================")
def make_manifest(level_list, is_shape_cat=False):
    manifest = []
    for idx, lvl in enumerate(level_list):
        lvl_num = idx + 1
        narr = len(lvl['Arrows'])
        xs = lvl['XSize']
        ys = lvl['YSize']
        h = lvl.get('Hash', '')
        
        shape_name = None
        if is_shape_cat:
            h_low = h.lower()
            if 'sphinx' in h_low: shape_name = 'The Great Sphinx (狮身人面像)'
            elif 'zhentan' in h_low: shape_name = 'Detective (侦探)'
            elif 'towerofpisa' in h_low or 'tower' in h_low: shape_name = 'Tower of Pisa (比萨斜塔)'
            elif 'tajmahal' in h_low or 'taj' in h_low: shape_name = 'Taj Mahal (泰姬陵)'
            elif 'sydneyopera' in h_low or 'opera' in h_low: shape_name = 'Sydney Opera House (悉尼歌剧院)'
            elif 'pirate' in h_low: shape_name = 'Pirate Ship (海盗船)'
            elif 'arcde' in h_low or 'arc_de' in h_low: shape_name = 'Arc de Triomphe (凯旋门)'
            elif 'starry' in h_low: shape_name = 'The Starry Night (星月夜)'
            elif 'treasurechest' in h_low or 'chest' in h_low: shape_name = 'Treasure Chest (宝箱)'
            elif 'cactus' in h_low or 'newlevel_26' in h_low: shape_name = 'Cactus Shape (仙人掌)'
            elif h.startswith('OLD_Level_'): shape_name = f'Shape Puzzle ({h})'
            elif not re.match(r'^[0-9a-fA-F]{16}$', h): shape_name = f'{h}'
            else: shape_name = f'Shape Puzzle ({h[:12]})'
            
        manifest.append({
            'level_number': lvl_num,
            'arrow_count': narr,
            'width': xs,
            'height': ys,
            'is_shape_level': is_shape_cat,
            'shape_name': shape_name
        })
    return manifest

with open('manifest_existing_1k.json', 'w', encoding='utf-8') as f:
    json.dump(make_manifest(existing_1k_solvable, is_shape_cat=False), f, indent=2, ensure_ascii=False)
print(f"Generated manifest_existing_1k.json ({len(existing_1k_solvable)} levels)")

with open('manifest_unity_normal.json', 'w', encoding='utf-8') as f:
    json.dump(make_manifest(unity_normal_solvable, is_shape_cat=False), f, indent=2, ensure_ascii=False)
print(f"Generated manifest_unity_normal.json ({len(unity_normal_solvable)} levels)")

with open('manifest_shape_puzzles.json', 'w', encoding='utf-8') as f:
    json.dump(make_manifest(combined_shapes_unique, is_shape_cat=True), f, indent=2, ensure_ascii=False)
print(f"Generated manifest_shape_puzzles.json ({len(combined_shapes_unique)} levels)")

print("\nPIPELINE COMPLETED SUCCESSFULLY!")
