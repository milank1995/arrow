import json, gzip
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend

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

all_levels = []
for p in range(15):
    pack = decrypt_pack(p)
    all_levels.extend(pack)

print(f"Total levels loaded from encrypted packs: {len(all_levels)}")

# Check duplicate hashes
hash_to_indices = defaultdict(list)
for idx, lvl in enumerate(all_levels):
    h = lvl.get('Hash')
    hash_to_indices[h].append(idx + 1)

duplicates = {h: idxs for h, idxs in hash_to_indices.items() if len(idxs) > 1}
print(f"Total unique hashes: {len(hash_to_indices)}")
print(f"Duplicate hashes count: {len(duplicates)}")
for h, idxs in list(duplicates.items())[:10]:
    print(f"  Hash {h}: appears at levels {idxs}")

# Check geometry duplicates (even if hashes were different, do any have identical arrows?)
geom_to_indices = defaultdict(list)
for idx, lvl in enumerate(all_levels):
    xs = lvl.get('XSize')
    ys = lvl.get('YSize')
    arrows = lvl.get('Arrows', [])
    # signature: grid size + sorted arrow cell sets
    arrow_sigs = []
    for a in arrows:
        indices = tuple(sorted(a.get('Indices', [])))
        arrow_sigs.append((indices, a.get('Dx'), a.get('Dy')))
    sig = (xs, ys, tuple(sorted(arrow_sigs)))
    geom_to_indices[sig].append(idx + 1)

geom_duplicates = {sig: idxs for sig, idxs in geom_to_indices.items() if len(idxs) > 1}
print(f"Total unique geometries: {len(geom_to_indices)}")
print(f"Geometry duplicate groups: {len(geom_duplicates)}")

# Look specifically for Binoculars: Level 5
lvl5 = all_levels[4] # 0-indexed
print(f"\nLevel 5: Hash={lvl5.get('Hash')}, Arrows={len(lvl5.get('Arrows'))}, Grid={lvl5.get('XSize')}x{lvl5.get('YSize')}")
lvl5_h = lvl5.get('Hash')
print(f"Where does Level 5 hash {lvl5_h} appear: {hash_to_indices.get(lvl5_h)}")

# Check what levels are in the 13,000 - 14,040 range
print("\nAnalyzing arrow counts in range 13,000 - 14,040:")
arrow_counts_late = [len(all_levels[i].get('Arrows')) for i in range(12999, len(all_levels))]
print(f"Range 13000-14040: min={min(arrow_counts_late)}, max={max(arrow_counts_late)}, avg={sum(arrow_counts_late)/len(arrow_counts_late):.1f}")
under_20 = [13000 + i for i, c in enumerate(arrow_counts_late) if c < 20]
print(f"Number of levels with < 20 arrows in 13000-14040: {len(under_20)}")
print(f"First 10 levels with < 20 arrows: {under_20[:10]}")
for lvl_num in under_20[:5]:
    l = all_levels[lvl_num - 1]
    print(f"  Level {lvl_num}: Hash={l.get('Hash')}, Arrows={len(l.get('Arrows'))}, Grid={l.get('XSize')}x{l.get('YSize')}")
