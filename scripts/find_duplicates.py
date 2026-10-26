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
    all_levels.extend(decrypt_pack(p))

print("Finding all 50x18 or 54-arrow or similar levels:")
for idx, lvl in enumerate(all_levels):
    xs = lvl.get('XSize')
    ys = lvl.get('YSize')
    narr = len(lvl.get('Arrows', []))
    # Level 5 is 50x18, 54 arrows
    if (xs == 50 and ys == 18) or (narr == 54 and xs > 40):
        print(f"Level {idx+1}: Hash={lvl.get('Hash')}, Grid={xs}x{ys}, Arrows={narr}")

# Also check other shape levels that appear multiple times
# Let's inspect the 301 geometry duplicates!
print("\nInspecting first 15 geometry duplicate groups:")
geom_to_indices = defaultdict(list)
for idx, lvl in enumerate(all_levels):
    xs = lvl.get('XSize')
    ys = lvl.get('YSize')
    arrows = lvl.get('Arrows', [])
    arrow_sigs = []
    for a in arrows:
        indices = tuple(sorted(a.get('Indices', [])))
        arrow_sigs.append((indices, a.get('Dx'), a.get('Dy')))
    sig = (xs, ys, tuple(sorted(arrow_sigs)))
    geom_to_indices[sig].append((idx + 1, lvl.get('Hash'), len(arrows)))

dup_groups = [v for v in geom_to_indices.values() if len(v) > 1]
dup_groups.sort(key=lambda v: len(v), reverse=True)
print(f"Total duplicate groups: {len(dup_groups)}")
for g in dup_groups[:15]:
    levels_str = ", ".join(f"L{lvl_num}" for lvl_num, h, n in g)
    print(f"  {len(g)} occurrences ({g[0][2]} arrows): {levels_str} (hashes: {[h for _, h, _ in g]})")
