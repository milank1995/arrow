import gzip
import json
import re
from collections import defaultdict
import UnityPy

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
configs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(configs_path, 'rb') as f:
    f.seek(32)
    env_cfg = UnityPy.load(f.read())

unity_tables = {}
for obj in env_cfg.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
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
        unity_raw_map[name] = d.m_Script.encode('utf-8', 'surrogateescape')

def analyze_pool(title, hashes):
    arrow_counts = []
    arrow_lengths = [] # points or length
    arrow_points_counts = []
    grid_widths = []
    grid_heights = []
    grid_areas = []
    
    for h in hashes:
        if h in unity_raw_map:
            raw = json.loads(gzip.decompress(unity_raw_map[h]))
            xs = raw.get('XSize', 0)
            ys = raw.get('YSize', 0)
            arrs = raw.get('Arrows', [])
            arrow_counts.append(len(arrs))
            grid_widths.append(xs)
            grid_heights.append(ys)
            grid_areas.append(xs * ys)
            for a in arrs:
                pts = a.get('Points', [])
                arrow_points_counts.append(len(pts))
                # Length in cells (including head to tail)
                cell_len = 0
                for k in range(len(pts) - 1):
                    p1, p2 = pts[k], pts[k+1]
                    x1, y1 = p1 % xs, p1 // xs
                    x2, y2 = p2 % xs, p2 // xs
                    cell_len += abs(x2 - x1) + abs(y2 - y1)
                arrow_lengths.append(cell_len + 1 if pts else 0)
    
    if not arrow_counts:
        return None
    return {
        'count': len(arrow_counts),
        'min_arrows': min(arrow_counts),
        'max_arrows': max(arrow_counts),
        'avg_arrows': sum(arrow_counts) / len(arrow_counts),
        'min_arr_len': min(arrow_lengths) if arrow_lengths else 0,
        'max_arr_len': max(arrow_lengths) if arrow_lengths else 0,
        'avg_arr_len': sum(arrow_lengths) / len(arrow_lengths) if arrow_lengths else 0,
        'min_pts': min(arrow_points_counts) if arrow_points_counts else 0,
        'max_pts': max(arrow_points_counts) if arrow_points_counts else 0,
        'min_grid': (min(grid_widths), min(grid_heights)),
        'max_grid': (max(grid_widths), max(grid_heights)),
        'min_area': min(grid_areas),
        'max_area': max(grid_areas),
    }

# All tables containing "Normal" or "Guide"
all_normal_hashes = set()
for tname, hashes in unity_tables.items():
    if 'normal' in tname.lower() or 'guide' in tname.lower():
        all_normal_hashes.update(hashes)

# Specific subsets:
groups = {
    'GuideLevels1 (Authentic Levels 1-40)': unity_tables.get('GuideLevels1', []),
    'NormalLevelsPriority (Levels 41-532 Campaign Priority)': unity_tables.get('NormalLevelsPriority', []),
    'All NormalLevelsPriority (Prio 0, 1, 2, 3)': (
        unity_tables.get('NormalLevelsPriority', []) + 
        unity_tables.get('NormalLevelsPriority1', []) + 
        unity_tables.get('NormalLevelsPriority2', []) + 
        unity_tables.get('NormalLevelsPriority3', [])
    ),
    'NormalLevels (Base Table)': unity_tables.get('NormalLevels', []),
    'All Normal & Guide Tables Combined (Full Unity Normal Pool)': list(all_normal_hashes),
}

for title, hashes in groups.items():
    stats = analyze_pool(title, hashes)
    print(f"=== {title} ===")
    if stats:
        print(f"  Total levels resolved: {stats['count']}")
        print(f"  Arrow Count per level: min = {stats['min_arrows']}, max = {stats['max_arrows']}, avg = {stats['avg_arrows']:.1f}")
        print(f"  Individual Arrow Length (in grid cells): min = {stats['min_arr_len']}, max = {stats['max_arr_len']}, avg = {stats['avg_arr_len']:.1f}")
        print(f"  Arrow Waypoints/Points per arrow: min = {stats['min_pts']}, max = {stats['max_pts']}")
        print(f"  Grid Dimensions (width x height): min = {stats['min_grid'][0]}x{stats['min_grid'][1]}, max = {stats['max_grid'][0]}x{stats['max_grid'][1]}")
    print()
