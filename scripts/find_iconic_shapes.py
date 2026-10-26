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

def render_ascii(xs, ys, arrows, max_w=40, max_h=20):
    grid = [[' ' for _ in range(xs)] for _ in range(ys)]
    for a in arrows:
        for idx in a.get('Indices', []):
            x = idx % xs
            y = idx // xs
            grid[y][x] = '#'
    
    # Downsample to thumbnail
    thumb = []
    sy = max(1, ys // max_h)
    sx = max(1, xs // max_w)
    for y in range(0, ys, sy):
        row = ""
        for x in range(0, xs, sx):
            # check if any cell in block is occupied
            has_cell = any(
                0 <= y+dy < ys and 0 <= x+dx < xs and grid[y+dy][x+dx] == '#'
                for dy in range(sy) for dx in range(sx)
            )
            row += "█" if has_cell else " "
        thumb.append(row)
    return "\n".join(thumb)

# Scan through levels to find iconic shapes
shapes_found = []
seen_sigs = set()

for idx, lvl in enumerate(all_levels):
    xs = lvl.get('XSize')
    ys = lvl.get('YSize')
    arrows = lvl.get('Arrows', [])
    narr = len(arrows)
    
    occupied = set()
    for a in arrows:
        for c in a.get('Indices', []):
            occupied.add(c)
    tot = xs * ys
    occ = len(occupied) / tot if tot > 0 else 1.0
    
    # Criteria for iconic silhouette
    if occ <= 0.70 and xs >= 20 and ys >= 20 and narr >= 20:
        sig = (xs, ys, occ, narr)
        # Avoid slight duplicate variations
        shape_type = (xs, ys, round(occ, 2), narr)
        if shape_type not in seen_sigs:
            seen_sigs.add(shape_type)
            shapes_found.append({
                'level': idx + 1,
                'hash': lvl.get('Hash'),
                'grid': f"{xs}x{ys}",
                'arrows': narr,
                'occ': round(occ, 2),
                'xs': xs,
                'ys': ys,
                'arrows_raw': arrows
            })

print(f"Total iconic shape candidates found: {len(shapes_found)}")
# Print first 30 with basic info
for i, s in enumerate(shapes_found[:35]):
    print(f"{i+1:2d}. Level {s['level']:5d}: Grid={s['grid']:7s}, Arrows={s['arrows']:3d}, Occupancy={s['occ']:.2f}, Hash={s['hash']}")
