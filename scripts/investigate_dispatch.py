import UnityPy

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
config_core_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\config_core_50b351747bc696328699b2e6aa5a5b33.ab'

with open(config_core_path, 'rb') as f:
    f.seek(32)
    env = UnityPy.load(f.read())

for obj in env.objects:
    if obj.type.name == 'TextAsset':
        data = obj.read()
        if getattr(data, 'm_Name', '') == 'config':
            b = data.m_Script.encode('utf-8', 'surrogateescape')
            pos = 7186599
            chunk = b[pos-100:pos+200]
            print(chunk.hex(' '))
            import re
            words = re.findall(rb'[a-zA-Z0-9_]{3,}', chunk)
            print([w.decode('ascii', errors='ignore') for w in words])
