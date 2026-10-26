import json, gzip, re, UnityPy
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'

def decrypt_pack(pack_idx):
    path = f'arrow_native/app/src/main/assets/levels/level_pack_{pack_idx:02d}.dat'
    with open(path, 'rb') as f:
        data = f.read()
    iv = data[:16]
    ciphertext = data[16:]
    cipher = Cipher(algorithms.AES(AES_KEY), modes.CBC(iv), backend=default_backend())
    decryptor = cipher.decryptor()
    padded = decryptor.update(ciphertext) + decryptor.finalize()
    pad_len = padded[-1]
    gzipped = padded[:-pad_len]
    raw_json = gzip.decompress(gzipped).decode('utf-8')
    return json.loads(raw_json)

# 1. Current Pack 00 (Levels 1 to 50)
pack0 = decrypt_pack(0)
print("=== Current Implementation: Levels 1 to 50 ===")
for i in range(50):
    lvl = pack0[i]
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    narr = len(arrows)
    occupied = set()
    for a in arrows:
        for idx in a.get('Indices', []):
            occupied.add(idx)
    occ = len(occupied) / (xs * ys)
    is_shape = occ <= 0.70 and xs >= 18 and ys >= 18 and narr >= 12
    shape_str = f"SHAPE (occ={occ:.2f})" if is_shape else f"regular (occ={occ:.2f})"
    print(f"Level {i+1:2d}: {narr:2d} arrows ({xs:2d}x{ys:2d}) - {shape_str} - Hash={lvl.get('Hash')}")

# 2. Authentic Unity Amaze Go Levels 1 to 50
# In Unity Amaze Go:
# GuideLevels1: 40 levels
# NormalLevelsPriority: 492 levels
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

unity_first_50_hashes = []
for h in unity_tables.get('GuideLevels1', []):
    unity_first_50_hashes.append(('GuideLevels1', h))
for h in unity_tables.get('NormalLevelsPriority', []):
    if len(unity_first_50_hashes) >= 50:
        break
    unity_first_50_hashes.append(('NormalLevelsPriority', h))

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

unity_raw_map = {}
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        unity_raw_map[name] = d.m_Script.encode('utf-8', 'surrogateescape')

print("\n=== Authentic Unity 'Amaze Go': Levels 1 to 50 ===")
for idx, (tab, h) in enumerate(unity_first_50_hashes[:50]):
    if h in unity_raw_map:
        raw = json.loads(gzip.decompress(unity_raw_map[h]))
        xs = raw['XSize']
        ys = raw['YSize']
        narr = len(raw.get('Arrows', []))
        occupied = set()
        for a in raw.get('Arrows', []):
            for p in a.get('Points', []):
                occupied.add(p)
        occ = len(occupied) / (xs * ys)
        is_shape = occ <= 0.70 and xs >= 18 and ys >= 18 and narr >= 12
        shape_str = f"SHAPE (occ={occ:.2f})" if is_shape else f"regular (occ={occ:.2f})"
        print(f"Level {idx+1:2d}: {narr:2d} arrows ({xs:2d}x{ys:2d}) - {shape_str} - [{tab}] Hash={h}")
