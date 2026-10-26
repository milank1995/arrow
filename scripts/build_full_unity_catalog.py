import time
import gzip
import json
import re
import UnityPy

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'
configs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'

t_start = time.time()
print("1. Loading configs and mapping tables...")
with open(configs_path, 'rb') as f:
    f.seek(32)
    env_cfg = UnityPy.load(f.read())

hash_to_tables = {}
normal_hashes = set()
for obj in env_cfg.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        # Exclude Daily, Challenge, Cat, Clash, Octa, Oneline, Radar, Pinata, Streak, Tarot, Chess, God, Gallery, Trophy
        is_normal = False
        if any(k in name for k in ['NormalLevels', 'GuideLevels', 'NormalPriority', 'MilestoneNormal', 'Normal_', 'Shape', 'Warmup', 'Relief']):
            is_normal = True
        elif not any(k in name.lower() for k in ['daily', 'challenge', 'cat', 'clash', 'octa', 'oneline', 'radar', 'pinata', 'streak', 'tarot', 'chess', 'god', 'gallery', 'trophy']):
            is_normal = True
            
        if is_normal:
            b = d.m_Script.encode('utf-8', 'surrogateescape')
            for h in re.findall(rb'[a-f0-9]{16}', b):
                h_str = h.decode('ascii')
                normal_hashes.add(h_str)
                if h_str not in hash_to_tables:
                    hash_to_tables[h_str] = []
                hash_to_tables[h_str].append(name)

print(f"Total normal hashes in configs: {len(normal_hashes)}")

print("2. Loading pack and parsing level geometry...")
with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

catalog = []
arrow_counts = []
shape_levels = []
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        if name in normal_hashes:
            raw = d.m_Script.encode('utf-8', 'surrogateescape')
            try:
                data = json.loads(gzip.decompress(raw))
                xs = data.get('XSize', 0)
                ys = data.get('YSize', 0)
                arrows = data.get('Arrows', [])
                narrs = len(arrows)
                
                # Calculate bends & occupied cells
                occupied_cells = set()
                total_bends = 0
                max_bends = 0
                for a in arrows:
                    pts = a.get('Points', [])
                    bends = max(0, len(pts) - 2)
                    total_bends += bends
                    max_bends = max(max_bends, bends)
                    # Expand points to cells
                    for k in range(len(pts) - 1):
                        p1, p2 = pts[k], pts[k + 1]
                        x1, y1 = p1 % xs, p1 // xs
                        x2, y2 = p2 % xs, p2 // xs
                        sx = 1 if x2 > x1 else (-1 if x2 < x1 else 0)
                        sy = 1 if y2 > y1 else (-1 if y2 < y1 else 0)
                        cx, cy = x1, y1
                        while (cx != x2) or (cy != y2):
                            occupied_cells.add(cy * xs + cx)
                            cx += sx
                            cy += sy
                    if pts:
                        tail = pts[-1]
                        occupied_cells.add((tail // xs) * xs + (tail % xs))
                        
                tot_cells = max(1, xs * ys)
                occupancy = len(occupied_cells) / tot_cells
                
                tables = hash_to_tables.get(name, [])
                is_shape = any('shape' in t.lower() for t in tables) or (occupancy < 0.65 and xs >= 15 and ys >= 15)
                
                entry = {
                    'hash': name,
                    'xSize': xs,
                    'ySize': ys,
                    'arrowCount': narrs,
                    'totalBends': total_bends,
                    'maxBends': max_bends,
                    'occupancy': round(occupancy, 3),
                    'isShape': is_shape,
                    'tables': tables
                }
                catalog.append(entry)
                arrow_counts.append(narrs)
                if is_shape:
                    shape_levels.append(entry)
            except Exception:
                pass

output_catalog_path = 'scripts/unity_normal_levels_catalog.json'
with open(output_catalog_path, 'w', encoding='utf-8') as fp:
    json.dump(catalog, fp)

elapsed = time.time() - t_start
print(f"Extraction and catalog creation complete in {elapsed:.2f} seconds!")
print(f"Total levels cataloged: {len(catalog)}")
print(f"Min arrows: {min(arrow_counts)}, Max arrows: {max(arrow_counts)}, Average arrows: {sum(arrow_counts)/len(arrow_counts):.1f}")
print(f"Total shape / custom silhouette levels identified: {len(shape_levels)}")
