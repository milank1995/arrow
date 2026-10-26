import os
import json
import re
import UnityPy

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
configs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'

# 1. Analyze Old Levels (1,001 levels)
old_dir = 'arrow_native/app/src/main/assets/levels'
old_levels = []
for i in range(1, 1002):
    fname = f'Level_{i}.json'
    fpath = os.path.join(old_dir, fname)
    if os.path.exists(fpath):
        with open(fpath, 'r', encoding='utf-8') as f:
            d = json.load(f)
            arr_count = len(d.get('Arrows', []))
            old_levels.append({
                'levelNumber': i,
                'fileName': fname,
                'xSize': d.get('XSize'),
                'ySize': d.get('YSize'),
                'arrowCount': arr_count,
                'maxHearts': d.get('MaxHearts'),
                'difficulty': d.get('Difficulty'),
                'diffScore': d.get('DiffScore'),
                'diffDna': d.get('DiffDNA'),
                'arrows': d.get('Arrows', [])
            })

print(f"Loaded {len(old_levels)} old levels.")

# Arrow count stats for Old Levels
old_arrow_counts = [l['arrowCount'] for l in old_levels]
min_old = min(old_arrow_counts)
max_old = max(old_arrow_counts)
min_old_lvls = [l['levelNumber'] for l in old_levels if l['arrowCount'] == min_old]
max_old_lvls = [l['levelNumber'] for l in old_levels if l['arrowCount'] == max_old]

print(f"Old Levels: Min arrows = {min_old} (Levels: {min_old_lvls[:10]}), Max arrows = {max_old} (Levels: {max_old_lvls[:10]})")

# 2. Analyze New Levels (1,533 levels)
new_dir = 'arrow_native/app/src/main/assets/new_levels'
new_levels = []
manifest_path = os.path.join(new_dir, 'manifest.json')
manifest = []
if os.path.exists(manifest_path):
    with open(manifest_path, 'r', encoding='utf-8') as f:
        manifest = json.load(f)

for i in range(1, 1534):
    fname = f'NewLevel_{i}.json'
    fpath = os.path.join(new_dir, fname)
    if os.path.exists(fpath):
        with open(fpath, 'r', encoding='utf-8') as f:
            d = json.load(f)
            arr_count = len(d.get('Arrows', []))
            meta = manifest[i-1] if i-1 < len(manifest) else {}
            new_levels.append({
                'levelNumber': i,
                'fileName': fname,
                'xSize': d.get('XSize'),
                'ySize': d.get('YSize'),
                'arrowCount': arr_count,
                'maxHearts': d.get('MaxHearts'),
                'category': meta.get('category'),
                'hash': meta.get('originalHash'),
                'arrows': d.get('Arrows', [])
            })

print(f"Loaded {len(new_levels)} new levels.")
new_arrow_counts = [l['arrowCount'] for l in new_levels]
min_new = min(new_arrow_counts)
max_new = max(new_arrow_counts)
min_new_lvls = [l['levelNumber'] for l in new_levels if l['arrowCount'] == min_new]
max_new_lvls = [l['levelNumber'] for l in new_levels if l['arrowCount'] == max_new]

print(f"New Levels: Min arrows = {min_new} (Levels: {min_new_lvls[:10]}), Max arrows = {max_new} (Levels: {max_new_lvls[:10]})")

# 3. Shape Analysis via Unity Config Tables
print("Loading Shape and Milestone tables from Unity configs...")
with open(configs_path, 'rb') as f:
    f.seek(32)
    env_cfg = UnityPy.load(f.read())

shape_tables = {}
for obj in env_cfg.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        if any(k in name.lower() for k in ['shape', 'milestone', 'hard', 'special']):
            b = d.m_Script.encode('utf-8', 'surrogateescape')
            hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
            if hashes:
                shape_tables[name] = hashes

print(f"Found {len(shape_tables)} shape/milestone tables.")

# Match new_levels against shape_tables
new_shape_matches = []
for l in new_levels:
    h = l.get('hash')
    if not h: continue
    matched_tabs = [tname for tname, thashes in shape_tables.items() if h in thashes]
    # Filter to specifically 'Shape' tables
    shape_tabs = [t for t in matched_tabs if 'shape' in t.lower()]
    if shape_tabs:
        new_shape_matches.append((l['levelNumber'], l['arrowCount'], l['xSize'], l['ySize'], h, shape_tabs))

print(f"New levels matching specific Shape tables: {len(new_shape_matches)}")
for m in new_shape_matches[:15]:
    print(f"  Level {m[0]}: {m[1]} arrows, {m[2]}x{m[3]}, hash={m[4]}, tables={m[5]}")
