import UnityPy
import re
import json
import gzip
import os
import glob

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'
with open(packs_path, 'rb') as f:
    f.seek(32)
    env = UnityPy.load(f.read())

named_dict = {}
for obj in env.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        if re.match(r'^[0-9a-fA-F]{11,16}$', name):
            continue
        if re.match(r'^(reason|rule|hole|pipes|level|test|guide|sample)[-_0-9a-zA-Z]+$', name):
            continue
        
        try:
            b = d.m_Script.encode('utf-8', 'surrogateescape')
            raw = json.loads(gzip.decompress(b))
            named_dict[name] = raw
        except Exception as e:
            pass

print(f"Parsed {len(named_dict)} named Unity levels!")

# Look at the names
categories = {}
for name, raw in sorted(named_dict.items()):
    clean = re.sub(r'[-_][0-9a-fA-F]{16}$', '', name)
    clean = re.sub(r'[-_][0-9a-fA-F]{8,}$', '', clean)
    arr_count = len(raw.get('Arrows', []))
    xs = raw.get('XSize', 0)
    ys = raw.get('YSize', 0)
    print(f"{clean:25s} | Raw: {name:35s} | {arr_count:3d} arr | {xs}x{ys}")
