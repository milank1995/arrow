import os
import sys
import json
import gzip
import glob
import re
import UnityPy

sys.stdout.reconfigure(encoding='utf-8')
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

all_large = []

# 1. Existing 1K
old_dir = "scratch/old_levels/arrow_native/app/src/main/assets/levels"
for p in glob.glob(os.path.join(old_dir, "Level_*.json")):
    try:
        with open(p, 'r', encoding='utf-8') as f:
            d = json.load(f)
            arr_count = len(d.get('Arrows', []))
            if 250 <= arr_count <= 450:
                all_large.append({
                    'Source': 'Existing 1K',
                    'File': os.path.basename(p),
                    'ArrowCount': arr_count,
                    'XSize': d['XSize'],
                    'YSize': d['YSize'],
                    'Name': os.path.basename(p).replace('.json', '')
                })
    except:
        pass

# 2. Scratch New Levels
new_dir = "scratch/old_levels/arrow_native/app/src/main/assets/new_levels"
for p in glob.glob(os.path.join(new_dir, "NewLevel_*.json")):
    try:
        with open(p, 'r', encoding='utf-8') as f:
            d = json.load(f)
            arr_count = len(d.get('Arrows', []))
            if 250 <= arr_count <= 450:
                all_large.append({
                    'Source': 'NewLevels',
                    'File': os.path.basename(p),
                    'ArrowCount': arr_count,
                    'XSize': d['XSize'],
                    'YSize': d['YSize'],
                    'Name': os.path.basename(p).replace('.json', '')
                })
    except:
        pass

# 3. Unity Asset Bundle
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
            arr_count = len(raw.get('Arrows', []))
            if 250 <= arr_count <= 450:
                all_large.append({
                    'Source': 'Unity AssetBundle',
                    'File': name,
                    'ArrowCount': arr_count,
                    'XSize': raw['XSize'],
                    'YSize': raw['YSize'],
                    'Name': name
                })
        except:
            pass

print(f"Total raw candidates with 250-450 arrows found in entire universe: {len(all_large)}")

# Check against our 3 manifests
with open('manifest_existing_1k.json', 'r', encoding='utf-8') as f:
    m_1k = json.load(f)
with open('manifest_unity_normal.json', 'r', encoding='utf-8') as f:
    m_norm = json.load(f)
with open('manifest_shape_puzzles.json', 'r', encoding='utf-8') as f:
    m_shape = json.load(f)

# Sort all large puzzles by arrow count
all_large.sort(key=lambda x: x['ArrowCount'])

print("\n--- ALL RAW PUZZLES WITH 250-450 ARROWS ---")
for idx, p in enumerate(all_large):
    # Search in categories
    in_cat = None
    lvl_num = None
    for item in m_shape:
        if p['Name'] in item.get('Hash', '') or item.get('Hash', '') in p['Name']:
            in_cat = f"Shape Puzzles (Level #{item['Level']})"
            break
    if not in_cat:
        for item in m_norm:
            if p['Name'] in item.get('Hash', '') or item.get('Hash', '') in p['Name']:
                in_cat = f"Unity Normal (Level #{item['Level']})"
                break
    if not in_cat:
        for item in m_1k:
            if p['Name'] in item.get('Hash', '') or item.get('Hash', '') in p['Name']:
                in_cat = f"Existing 1K (Level #{item['Level']})"
                break
    status = in_cat if in_cat else "MISSING / NOT IN CURRENT 3 CATEGORIES"
    print(f"#{idx+1:2d}: {p['ArrowCount']:3d} arr | Grid {p['XSize']}x{p['YSize']} | Source: {p['Source']} | Name: {p['Name']} -> {status}")
