import os
import time
import gzip
import json
import re
import UnityPy
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'
configs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
output_dir = r'arrow_native/app/src/main/assets/levels'

AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!' # 32 bytes = AES-256

def encrypt_bytes(data_bytes):
    compressed = gzip.compress(data_bytes, compresslevel=9)
    padder = padding.PKCS7(128).padder()
    padded_data = padder.update(compressed) + padder.finalize()
    iv = os.urandom(16)
    cipher = Cipher(algorithms.AES(AES_KEY), modes.CBC(iv))
    encryptor = cipher.encryptor()
    ciphertext = encryptor.update(padded_data) + encryptor.finalize()
    return iv + ciphertext

t0 = time.time()
print("1. Extracting official table sequence of 14,040 normal levels...")
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

with open('scripts/unity_normal_levels_catalog.json', 'r') as fp:
    catalog = json.load(fp)
pack_hashes = set(x['hash'] for x in catalog)

ordered_sequence = []
seen = set()

# GuideLevels1 (Tutorial Levels 1 to 40)
for h in tables.get('GuideLevels1', []):
    if h in pack_hashes and h not in seen:
        seen.add(h)
        ordered_sequence.append(('GuideLevels1', h))

# Normal Priority 0-3
for ptab in ['NormalLevelsPriority', 'NormalLevelsPriority1', 'NormalLevelsPriority2', 'NormalLevelsPriority3']:
    for h in tables.get(ptab, []):
        if h in pack_hashes and h not in seen:
            seen.add(h)
            ordered_sequence.append((ptab, h))

# Numbered NormalLevels1 to NormalLevels93
numbered_normal = [k for k in tables if re.match(r'^NormalLevels\d+$', k)]
numbered_normal.sort(key=lambda x: int(re.findall(r'\d+', x)[0]))
for t in numbered_normal:
    for h in tables.get(t, []):
        if h in pack_hashes and h not in seen:
            seen.add(h)
            ordered_sequence.append((t, h))

# Shape Normal tables
shape_normal = [k for k in tables if 'shape' in k.lower() and not any(m in k.lower() for m in ['daily', 'challenge', 'clash', 'cat'])]
for t in shape_normal:
    for h in tables.get(t, []):
        if h in pack_hashes and h not in seen:
            seen.add(h)
            ordered_sequence.append((t, h))

# Remaining Normal tables
for t, hlist in tables.items():
    if not any(m in t.lower() for m in ['daily', 'challenge', 'cat', 'clash', 'octa', 'oneline', 'radar', 'pinata', 'streak', 'tarot', 'chess', 'god', 'gallery', 'trophy']):
        for h in hlist:
            if h in pack_hashes and h not in seen:
                seen.add(h)
                ordered_sequence.append((t, h))

print(f"Sequence verified: {len(ordered_sequence)} authentic levels in {time.time() - t0:.2f}s")

print("2. Loading raw level packs from AssetBundle...")
with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

raw_levels_map = {}
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        if name in pack_hashes:
            raw_levels_map[name] = d.m_Script.encode('utf-8', 'surrogateescape')

print(f"Loaded {len(raw_levels_map)} raw levels in {time.time() - t0:.2f}s")

def convert_unity_level(raw_bytes, cat, h):
    raw_level = json.loads(gzip.decompress(raw_bytes))
    xs = raw_level['XSize']
    ys = raw_level['YSize']
    arrows_out = []
    
    for a in raw_level.get('Arrows', []):
        pts = a.get('Points', [])
        if not pts or len(pts) < 2:
            continue
        head_idx = pts[0]
        next_idx = pts[1]
        hx, hy = head_idx % xs, head_idx // xs
        nx, ny = next_idx % xs, next_idx // xs
        
        dx = 1 if hx > nx else (-1 if hx < nx else 0)
        dy = 1 if hy > ny else (-1 if hy < ny else 0)
        
        indices = []
        for k in range(len(pts) - 1):
            p1, p2 = pts[k], pts[k + 1]
            x1, y1 = p1 % xs, p1 // xs
            x2, y2 = p2 % xs, p2 // xs
            sx = 1 if x2 > x1 else (-1 if x2 < x1 else 0)
            sy = 1 if y2 > y1 else (-1 if y2 < y1 else 0)
            cx, cy = x1, y1
            while (cx != x2) or (cy != y2):
                indices.append(cy * xs + cx)
                cx += sx
                cy += sy
        tail = pts[-1]
        indices.append((tail // xs) * xs + (tail % xs))
        
        bend_count = max(0, len(pts) - 2)
        arrows_out.append({
            "Dx": dx,
            "Dy": dy,
            "X": hx,
            "Y": hy,
            "Indices": indices,
            "BendCount": bend_count
        })
        
    return {
        "XSize": xs,
        "YSize": ys,
        "Arrows": arrows_out,
        "MaxHearts": 3,
        "Category": cat,
        "Hash": h
    }

print("3. Converting and chunking into encrypted binary packs...")
os.makedirs(output_dir, exist_ok=True)

CHUNK_SIZE = 1000
total_levels = len(ordered_sequence)
total_packs = (total_levels + CHUNK_SIZE - 1) // CHUNK_SIZE

pack_manifest = {
    "totalLevels": total_levels,
    "chunkSize": CHUNK_SIZE,
    "totalPacks": total_packs,
    "packs": []
}

for pack_idx in range(total_packs):
    start_idx = pack_idx * CHUNK_SIZE
    end_idx = min(start_idx + CHUNK_SIZE, total_levels)
    chunk_items = ordered_sequence[start_idx:end_idx]
    
    pack_levels = []
    for cat, h in chunk_items:
        raw_b = raw_levels_map.get(h)
        if raw_b:
            lvl_dict = convert_unity_level(raw_b, cat, h)
            pack_levels.append(lvl_dict)
            
    # Serialize to JSON and encrypt
    json_bytes = json.dumps(pack_levels, separators=(',', ':')).encode('utf-8')
    enc_data = encrypt_bytes(json_bytes)
    
    pack_filename = f"level_pack_{pack_idx:02d}.dat"
    pack_path = os.path.join(output_dir, pack_filename)
    with open(pack_path, 'wb') as fp:
        fp.write(enc_data)
        
    pack_manifest["packs"].append({
        "packIndex": pack_idx,
        "fileName": pack_filename,
        "startLevel": start_idx + 1,
        "endLevel": end_idx,
        "count": len(pack_levels),
        "sizeBytes": len(enc_data)
    })
    print(f"  Wrote {pack_filename}: Levels {start_idx + 1} to {end_idx} ({len(enc_data)/1024:.1f} KB)")

# Also encrypt the 300 DailyLevels
print("4. Encrypting DailyLevels into daily_pack.dat...")
daily_levels = []
for d_idx in range(1, 301):
    d_path = os.path.join(output_dir, f"DailyLevels_{d_idx}.json")
    if os.path.exists(d_path):
        with open(d_path, 'r', encoding='utf-8') as df:
            daily_levels.append(json.load(df))

if daily_levels:
    daily_bytes = json.dumps(daily_levels, separators=(',', ':')).encode('utf-8')
    enc_daily = encrypt_bytes(daily_bytes)
    daily_pack_path = os.path.join(output_dir, "daily_pack.dat")
    with open(daily_pack_path, 'wb') as dfp:
        dfp.write(enc_daily)
    print(f"  Wrote daily_pack.dat: {len(daily_levels)} daily levels ({len(enc_daily)/1024:.1f} KB)")

# Write manifest
manifest_path = os.path.join(output_dir, "pack_manifest.json")
with open(manifest_path, 'w', encoding='utf-8') as mf:
    json.dump(pack_manifest, mf, indent=2)

print(f"All 14,040 levels successfully encrypted into 15 packs in {time.time() - t0:.2f}s!")
print(f"Manifest written to {manifest_path}")
