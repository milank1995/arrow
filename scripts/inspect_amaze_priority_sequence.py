import UnityPy, re, json, gzip
from collections import defaultdict

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

# In Amaze Go:
# GuideLevels1: 40 levels
# NormalLevelsPriority: 492 levels
# NormalLevelsPriority1: 500 levels
# NormalLevelsPriority2: 447 levels
# NormalLevelsPriority3: 500 levels
# Total across Priority 0-3 + GuideLevels1 = 1979 levels! ALMOST EXACTLY 2,000 LEVELS!
priority_sequence = []
seen = set()

for h in unity_tables.get('GuideLevels1', []):
    if h not in seen:
        seen.add(h)
        priority_sequence.append(('GuideLevels1', h))

for ptab in ['NormalLevelsPriority', 'NormalLevelsPriority1', 'NormalLevelsPriority2', 'NormalLevelsPriority3']:
    for h in unity_tables.get(ptab, []):
        if h not in seen:
            seen.add(h)
            priority_sequence.append((ptab, h))

print(f"Total authentic Amaze Go Priority levels: {len(priority_sequence)} levels!")

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

raw_map = {}
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        raw_map[name] = d.m_Script.encode('utf-8', 'surrogateescape')

# Analyze the first 50 levels and blocks of 100
arrow_counts = []
for tab, h in priority_sequence:
    if h in raw_map:
        raw = json.loads(gzip.decompress(raw_map[h]))
        arrow_counts.append(len(raw.get('Arrows', [])))

print(f"Loaded {len(arrow_counts)} level arrow counts.")
for b in range(0, min(1000, len(arrow_counts)), 100):
    chunk = arrow_counts[b:b+100]
    print(f"Levels {b+1:4d} - {b+100:4d}: min={min(chunk):2d}, avg={sum(chunk)/len(chunk):4.1f}, max={max(chunk):3d}")
