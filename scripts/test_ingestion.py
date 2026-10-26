import os
import glob
import json
import gzip
import time
import UnityPy
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

print("--- 1. Ingesting Existing Levels (Old 1001 + New 1533) ---")
t0 = time.time()
old_dir = "scratch/old_levels/arrow_native/app/src/main/assets/levels"
new_dir = "scratch/old_levels/arrow_native/app/src/main/assets/new_levels"

existing_levels = []

for p in glob.glob(os.path.join(old_dir, "Level_*.json")):
    try:
        with open(p, 'r', encoding='utf-8') as f:
            d = json.load(f)
            d['Source'] = os.path.basename(p)
            existing_levels.append(d)
    except Exception:
        pass

for p in glob.glob(os.path.join(new_dir, "NewLevel_*.json")):
    try:
        with open(p, 'r', encoding='utf-8') as f:
            d = json.load(f)
            d['Source'] = os.path.basename(p)
            existing_levels.append(d)
    except Exception:
        pass

print(f"Loaded {len(existing_levels)} existing levels in {time.time() - t0:.2f}s")

print("--- 2. Ingesting Unity Levels ---")
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'
with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

unity_levels = []
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
            unity_levels.append({
                'XSize': xs,
                'YSize': ys,
                'Arrows': arrows_out,
                'MaxHearts': raw.get('MaxHearts', 3),
                'Hash': name,
                'Source': 'Unity_' + name
            })
        except Exception:
            pass

print(f"Loaded {len(unity_levels)} Unity levels in {time.time() - t0:.2f}s")
print(f"Total candidate levels from all sources: {len(existing_levels) + len(unity_levels)}")
