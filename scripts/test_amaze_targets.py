import json, gzip, re, UnityPy
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

# 1. Load the authentic Amaze Go arrow sequence from GuideLevels1 + NormalLevelsPriority
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

amaze_sequence = []
seen = set()
for h in unity_tables.get('GuideLevels1', []):
    if h not in seen:
        seen.add(h)
        amaze_sequence.append(('GuideLevels1', h))

for ptab in ['NormalLevelsPriority', 'NormalLevelsPriority1', 'NormalLevelsPriority2', 'NormalLevelsPriority3', 'NormalLevels1']:
    for h in unity_tables.get(ptab, []):
        if h not in seen:
            seen.add(h)
            amaze_sequence.append((ptab, h))
        if len(amaze_sequence) >= 2000:
            break
    if len(amaze_sequence) >= 2000:
        break

print(f"Loaded {len(amaze_sequence)} Amaze Go priority sequence levels.")

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

raw_map = {}
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        raw_map[name] = d.m_Script.encode('utf-8', 'surrogateescape')

# Extract target arrow counts from Amaze Go sequence
amaze_arrow_targets = []
for tab, h in amaze_sequence:
    if h in raw_map:
        raw = json.loads(gzip.decompress(raw_map[h]))
        amaze_arrow_targets.append(len(raw.get('Arrows', [])))
    else:
        # Fallback extrapolation
        amaze_arrow_targets.append(60)

print(f"Extracted {len(amaze_arrow_targets)} target arrow counts matching Amaze Go progression.")
print("Amaze Go Targets for first 50 levels:")
for i in range(50):
    is_milestone = ((i + 1) % 5 == 0)
    print(f"Level {i+1:2d}: {amaze_arrow_targets[i]:3d} arrows {'(MILESTONE SHAPE *)' if is_milestone else ''}")
