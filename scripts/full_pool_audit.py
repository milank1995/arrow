import os
import json
import gzip
import re
import UnityPy

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'
configs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'

def compute_geometry_fingerprint(xs, ys, arrows):
    # Normalize arrow representations: sort arrows by head (X, Y) and Dx, Dy, and length
    norm_arrows = []
    for a in arrows:
        indices = tuple(a.get('Indices', []))
        norm_arrows.append((a.get('X'), a.get('Y'), a.get('Dx'), a.get('Dy'), len(indices), indices))
    norm_arrows.sort()
    return (xs, ys, len(arrows), tuple((x, y, dx, dy, ln) for x, y, dx, dy, ln, _ in norm_arrows))

print("1. Auditing Unity APK Configs and Packs...")
with open(configs_path, 'rb') as f:
    f.seek(32)
    env_cfg = UnityPy.load(f.read())

normal_config_hashes = set()
for obj in env_cfg.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        if any(k in name for k in ['NormalLevels', 'GuideLevels', 'NormalPriority', 'MilestoneNormal', 'Normal_']) or not any(k in name.lower() for k in ['daily', 'challenge', 'cat', 'clash', 'octa', 'oneline', 'radar', 'pinata', 'streak', 'tarot', 'chess', 'god', 'gallery', 'trophy']):
            b = d.m_Script.encode('utf-8', 'surrogateescape')
            for h in re.findall(rb'[a-f0-9]{16}', b):
                normal_config_hashes.add(h.decode('ascii'))

print(f"Total authentic normal level hashes referenced in configs: {len(normal_config_hashes)}")

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

unity_levels_by_name = {}
unity_levels_by_fp = {}
duplicate_fp_in_unity = 0

for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        raw = d.m_Script.encode('utf-8', 'surrogateescape')
        try:
            raw_level = json.loads(gzip.decompress(raw))
            xs = raw_level['XSize']
            ys = raw_level['YSize']
            arrows = []
            for a in raw_level.get('Arrows', []):
                pts = a.get('Points', [])
                if len(pts) < 2:
                    continue
                hx, hy = pts[0] % xs, pts[0] // xs
                nx, ny = pts[1] % xs, pts[1] // xs
                dx = 1 if hx > nx else (-1 if hx < nx else 0)
                dy = 1 if hy > ny else (-1 if hy < ny else 0)
                indices = []
                for k in range(len(pts) - 1):
                    p1, p2 = pts[k], pts[k + 1]
                    x1, y1 = p1 % xs, p1 // xs
                    x2, y2 = p2 % xs, p2 // xs
                    step_x = 1 if x2 > x1 else (-1 if x2 < x1 else 0)
                    step_y = 1 if y2 > y1 else (-1 if y2 < y1 else 0)
                    cx, cy = x1, y1
                    while (cx != x2) or (cy != y2):
                        indices.append(cy * xs + cx)
                        cx += step_x
                        cy += step_y
                indices.append((pts[-1] // xs) * xs + (pts[-1] % xs))
                arrows.append({'X': hx, 'Y': hy, 'Dx': dx, 'Dy': dy, 'Indices': indices})
            
            fp = compute_geometry_fingerprint(xs, ys, arrows)
            unity_levels_by_name[name] = fp
            if fp in unity_levels_by_fp:
                duplicate_fp_in_unity += 1
            else:
                unity_levels_by_fp[fp] = name
        except Exception:
            pass

print(f"Total TextAssets parsed from levelpacks_newuser: {len(unity_levels_by_name)}")
print(f"Unique geometry fingerprints in levelpacks_newuser: {len(unity_levels_by_fp)} (Duplicates: {duplicate_fp_in_unity})")

# Check normal levels subset
normal_levels_in_pack = {k: v for k, v in unity_levels_by_name.items() if k in normal_config_hashes}
print(f"Authentic normal levels available in pack: {len(normal_levels_in_pack)}")

# 2. Check existing project levels in levels/
old_levels_dir = 'arrow_native/app/src/main/assets/levels'
old_files = os.listdir(old_levels_dir)
print(f"\n2. Auditing old assets/levels/ ({len(old_files)} files)...")

old_matching_unity = 0
old_matching_normal = 0
old_by_fp = {}

for f in old_files:
    path = os.path.join(old_levels_dir, f)
    with open(path, 'r', encoding='utf-8') as fp:
        d = json.load(fp)
        xs = d.get('XSize', 0)
        ys = d.get('YSize', 0)
        arrows = d.get('Arrows', [])
        geom_fp = compute_geometry_fingerprint(xs, ys, arrows)
        old_by_fp[f] = geom_fp
        if geom_fp in unity_levels_by_fp:
            old_matching_unity += 1
            unity_name = unity_levels_by_fp[geom_fp]
            if unity_name in normal_config_hashes:
                old_matching_normal += 1

print(f"Old levels matching ANY Unity pack level: {old_matching_unity} / {len(old_files)}")
print(f"Old levels matching an authentic NORMAL Unity level: {old_matching_normal} / {len(old_files)}")

# 3. Check new_levels/
new_levels_dir = 'arrow_native/app/src/main/assets/new_levels'
new_files = [f for f in os.listdir(new_levels_dir) if f.startswith('NewLevel_')]
print(f"\n3. Auditing assets/new_levels/ ({len(new_files)} files)...")

new_matching_unity = 0
new_matching_normal = 0

for f in new_files:
    path = os.path.join(new_levels_dir, f)
    with open(path, 'r', encoding='utf-8') as fp:
        d = json.load(fp)
        xs = d.get('XSize', 0)
        ys = d.get('YSize', 0)
        arrows = d.get('Arrows', [])
        geom_fp = compute_geometry_fingerprint(xs, ys, arrows)
        if geom_fp in unity_levels_by_fp:
            new_matching_unity += 1
            unity_name = unity_levels_by_fp[geom_fp]
            if unity_name in normal_config_hashes:
                new_matching_normal += 1

print(f"New levels matching ANY Unity pack level: {new_matching_unity} / {len(new_files)}")
print(f"New levels matching an authentic NORMAL Unity level: {new_matching_normal} / {len(new_files)}")
