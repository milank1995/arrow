import UnityPy, re, json, gzip
from collections import defaultdict

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

configs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(configs_path, 'rb') as f:
    f.seek(32)
    env_cfg = UnityPy.load(f.read())

tables = {}
for obj in env_cfg.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
        if hashes:
            tables[name] = hashes

print(f"Total tables: {len(tables)}")

# Look at AllLevel table if it exists
if 'AllLevel' in tables:
    print(f"AllLevel table exists with {len(tables['AllLevel'])} hashes!")

# Let's inspect other master tables
for name in tables:
    if any(k in name.lower() for k in ['all', 'master', 'main', 'order', 'sequence', 'progression']):
        print(f"Candidate master table: {name} ({len(tables[name])} hashes)")
