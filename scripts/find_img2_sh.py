import UnityPy
import gzip
import json
import re

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
config_bundle_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(config_bundle_path, 'rb') as f:
    f.seek(32)
    env = UnityPy.load(f.read())

# Find all SuperHard configs
super_hard_hashes = {}
for obj in env.objects:
    if obj.type.name == 'TextAsset':
        data = obj.read()
        name = getattr(data, 'm_Name', '')
        if 'superhard' in name.lower() and not 'shape' in name.lower() and not 'cat' in name.lower() and not '3d' in name.lower():
            b = data.m_Script.encode('utf-8', 'surrogateescape')
            all_h = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
            if all_h:
                super_hard_hashes[name] = all_h

print(f"Found {len(super_hard_hashes)} SuperHard configs:")
for name, hashes in sorted(super_hard_hashes.items()):
    print(f"  {name}: {len(hashes)} levels")

# Now load pack data for all SuperHard levels to find Image 2
all_sh_set = set(h for hashes in super_hard_hashes.values() for h in hashes)
print(f"Total unique SuperHard level hashes: {len(all_sh_set)}")

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

sh_levels = {}
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        data = obj.read()
        name = getattr(data, 'm_Name', '')
        if name in all_sh_set:
            raw = data.m_Script.encode('utf-8', 'surrogateescape')
            try:
                sh_levels[name] = json.loads(gzip.decompress(raw))
            except Exception:
                pass

print(f"Loaded {len(sh_levels)} SuperHard levels from pack.")

# Now check each for Image 2 features:
# Image 2:
# Size ~20-30 wide, 35-50 high, ~60-85 arrows
# Concentric U-turn boxes at bottom!
# Let's check which level has:
# Arrow heads at bottom-left, concentric boxes
img2_matches = []
for h, lvl in sh_levels.items():
    xs = lvl.get('XSize', 0)
    ys = lvl.get('YSize', 0)
    narrs = len(lvl.get('Arrows', []))
    if 20 <= xs <= 35 and 35 <= ys <= 55 and 55 <= narrs <= 95:
        # check bottom row (ys-1)
        bottom_heads = []
        for a in lvl['Arrows']:
            pts = a['Points']
            head = pts[0]
            hx, hy = head % xs, head // xs
            if hy >= ys - 3:
                bottom_heads.append((hx, hy))
        img2_matches.append((h, xs, ys, narrs, len(bottom_heads)))

print(f"Matching SuperHard levels for Image 2: {len(img2_matches)}")
for m in img2_matches[:25]:
    print(" ", m)
