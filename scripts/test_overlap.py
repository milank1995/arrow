import UnityPy, re
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

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

nl1 = set(tables.get('NormalLevels1', []))
nl2 = set(tables.get('NormalLevels2', []))
nl3 = set(tables.get('NormalLevels3', []))
print(f"NormalLevels1 count: {len(nl1)}")
print(f"NormalLevels2 count: {len(nl2)}")
print(f"NormalLevels3 count: {len(nl3)}")
print(f"Overlap NL1 and NL2: {len(nl1 & nl2)}")
print(f"Overlap NL1 and NL3: {len(nl1 & nl3)}")
print(f"Overlap NL2 and NL3: {len(nl2 & nl3)}")
