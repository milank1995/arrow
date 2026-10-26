import UnityPy, re, json, gzip
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

# Load current 14,040 levels
current_levels = []
for p in range(15):
    current_levels.extend(decrypt_pack(p))

hash_to_curr_levels = defaultdict(list)
for idx, lvl in enumerate(current_levels):
    hash_to_curr_levels[lvl.get('Hash')].append(idx + 1)

print(f"Loaded {len(current_levels)} current levels.")

# Now load configs to see which tables contain each hash
configs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
with open(configs_path, 'rb') as f:
    f.seek(32)
    env_cfg = UnityPy.load(f.read())

hash_to_tables = defaultdict(list)
for obj in env_cfg.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
        for h in hashes:
            hash_to_tables[h].append(name)

# Identify all shape levels
shape_levels = []
seen_shapes = set()

for idx, lvl in enumerate(current_levels):
    h = lvl.get('Hash')
    xs = lvl.get('XSize')
    ys = lvl.get('YSize')
    arrows = lvl.get('Arrows', [])
    narr = len(arrows)
    
    # Calculate cell occupancy
    occupied_cells = set()
    for a in arrows:
        for c in a.get('Indices', []):
            occupied_cells.add(c)
    tot_cells = xs * ys
    occupancy = len(occupied_cells) / tot_cells if tot_cells > 0 else 1.0
    
    tables = hash_to_tables.get(h, [])
    is_shape_table = any('shape' in t.lower() for t in tables)
    is_guide_shape = any('guide' in t.lower() for t in tables) and (occupancy < 0.70 or xs >= 25 or ys >= 25)
    is_low_occ = (occupancy < 0.65 and xs >= 20 and ys >= 20 and narr >= 20)
    
    if is_shape_table or is_guide_shape or is_low_occ:
        # Check geometric signature
        arrow_sigs = []
        for a in arrows:
            arrow_sigs.append(tuple(sorted(a.get('Indices', []))))
        geom_sig = (xs, ys, tuple(sorted(arrow_sigs)))
        if geom_sig not in seen_shapes:
            seen_shapes.add(geom_sig)
            shape_levels.append({
                'hash': h,
                'xSize': xs,
                'ySize': ys,
                'arrows': narr,
                'occupancy': round(occupancy, 3),
                'curr_levels': hash_to_curr_levels.get(h, []),
                'tables': [t for t in tables if any(k in t.lower() for k in ['shape', 'guide', 'cat', 'super', 'hard', 'relief', 'warmup'])]
            })

print(f"Total unique shape puzzles found: {len(shape_levels)}")
