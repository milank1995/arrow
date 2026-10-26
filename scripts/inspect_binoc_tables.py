import UnityPy, re
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

configs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
with open(configs_path, 'rb') as f:
    f.seek(32)
    env = UnityPy.load(f.read())

hash_to_tables = {}
for obj in env.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
        for h in hashes:
            if h not in hash_to_tables:
                hash_to_tables[h] = []
            hash_to_tables[h].append(name)

binoc_hashes = ['c2f9042ea8ee5ea4', '5eaf58b1d480ffc6', '35334aa8039d2e15', '6cb76e88fd7732c4', '61d1ffee26ac0bf6', 'e3952896195dbc6b', '1c5e97110c6b804a']
for bh in binoc_hashes:
    print(f"Hash {bh}: tables = {hash_to_tables.get(bh, [])}")
