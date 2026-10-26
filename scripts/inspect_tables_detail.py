import UnityPy
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
import re, json
from collections import defaultdict

configs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
with open(configs_path, 'rb') as f:
    f.seek(32)
    env = UnityPy.load(f.read())

tables = {}
for obj in env.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
        if hashes:
            tables[name] = hashes

print(f"Total tables: {len(tables)}")
for name, hlist in sorted(tables.items()):
    if 'guide' in name.lower() or 'priority' in name.lower() or 'shape' in name.lower():
        print(f"  {name:30s}: {len(hlist)} hashes")
