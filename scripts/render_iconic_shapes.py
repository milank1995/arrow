import json, gzip
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

# Check specifically:
# L5, L10, L15, L20, L26, L30, L8606, L8607, L8608, L8609, L8610, L8611, L8613, L8617, L14040
target_levels = [5, 10, 15, 20, 26, 30, 8606, 8607, 8608, 8609, 8610, 8611, 8613, 8617, 14040]

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
    
    # Scale down to 24 wide, 12 tall
    out_lines = []
    sy = max(1, ys // 12)
    sx = max(1, xs // 24)
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

for lvl_num in target_levels:
    lvl = get_level(lvl_num)
    xs = lvl['XSize']
    ys = lvl['YSize']
    narr = len(lvl['Arrows'])
    print(f"\n==========================================")
    print(f"Level {lvl_num} ({xs}x{ys}, {narr} arrows, hash={lvl['Hash']})")
    print(f"==========================================")
    print(render_compact(lvl))
