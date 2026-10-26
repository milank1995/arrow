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

# Check other clusters of shape levels
clusters = [
    6400, 6417, 6423, 6457, 6458, 6470, 6471, 6472, 6473,
    8612, 8614, 8615, 8616, 8618, 8619, 8620,
    9870, 10029, 12008
]

cached_packs = {}
def get_level(lvl_num):
    pack_idx = (lvl_num - 1) // 1000
    idx = (lvl_num - 1) % 1000
    if pack_idx not in cached_packs:
        cached_packs[pack_idx] = decrypt_pack(pack_idx)
    return cached_packs[pack_idx][idx]

def render_compact(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    grid = [[' ' for _ in range(xs)] for _ in range(ys)]
    for a in arrows:
        for idx in a.get('Indices', []):
            grid[idx // xs][idx % xs] = '#'
    
    out_lines = []
    sy = max(1, ys // 10)
    sx = max(1, xs // 20)
    for y in range(0, ys, sy):
        line = ""
        for x in range(0, xs, sx):
            occupied = False
            for dy in range(sy):
                for dx in range(sx):
                    if 0 <= y+dy < ys and 0 <= x+dx < xs and grid[y+dy][x+dx] == '#':
                        occupied = True
                        break
                if occupied: break
            line += "*" if occupied else " "
        out_lines.append(line.rstrip())
    return "\n".join(out_lines)

for lvl_num in clusters:
    lvl = get_level(lvl_num)
    xs = lvl['XSize']
    ys = lvl['YSize']
    narr = len(lvl['Arrows'])
    print(f"\n--- Level {lvl_num} ({xs}x{ys}, {narr} arr, hash={lvl['Hash'][:8]}) ---")
    print(render_compact(lvl))
