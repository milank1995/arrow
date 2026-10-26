import UnityPy, re, json, gzip
from collections import defaultdict

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

raw_levels = []
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        try:
            lvl = json.loads(gzip.decompress(b))
            lvl['Hash'] = name
            raw_levels.append(lvl)
        except Exception:
            pass

print(f"Total raw levels in asset bundle: {len(raw_levels)}")

# Deduplicate by geometry
unique_by_geom = {}
for lvl in raw_levels:
    xs = lvl.get('XSize')
    ys = lvl.get('YSize')
    arrows = lvl.get('Arrows', [])
    arrow_sigs = []
    for a in arrows:
        pts = tuple(sorted(a.get('Points', [])))
        arrow_sigs.append((pts, len(pts)))
    geom_key = (xs, ys, tuple(sorted(arrow_sigs)))
    if geom_key not in unique_by_geom:
        unique_by_geom[geom_key] = lvl

print(f"Total truly unique geometry levels: {len(unique_by_geom)}")

# Analyze arrow count distribution of unique levels
unique_list = list(unique_by_geom.values())
arrow_counts = [len(l.get('Arrows', [])) for l in unique_list]
print(f"Arrow counts: min={min(arrow_counts)}, max={max(arrow_counts)}, avg={sum(arrow_counts)/len(arrow_counts):.1f}")

# Check arrow count brackets
brackets = defaultdict(int)
for c in arrow_counts:
    bracket = (c // 10) * 10
    brackets[bracket] += 1

print("\nArrow count distribution:")
for b in sorted(brackets):
    print(f"  {b:3d} - {b+9:3d} arrows: {brackets[b]:5d} levels")
