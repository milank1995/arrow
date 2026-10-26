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

# Check specifically recognizable shapes in the first 100 levels and milestone shape tables
test_milestones = [
    5, 10, 15, 20, 25, 26, 30, 35, 40, 45, 50, 60, 75, 80, 90, 100, 120, 150, 200, 250, 300, 500, 1000, 14040
]

for lvl_num in test_milestones:
    pack_idx = (lvl_num - 1) // 1000
    idx_in_pack = (lvl_num - 1) % 1000
    pack = decrypt_pack(pack_idx)
    lvl = pack[idx_in_pack]
    xs = lvl.get('XSize')
    ys = lvl.get('YSize')
    arrows = lvl.get('Arrows', [])
    occupied = set()
    for a in arrows:
        for idx in a.get('Indices', []):
            ux = idx % xs
            uy = idx // xs
            occupied.add((ux, uy))
    occ = len(occupied) / (xs * ys)
    print(f"Level {lvl_num:5d}: Hash={lvl.get('Hash')}, Grid={xs:2d}x{ys:2d}, Arrows={len(arrows):3d}, Occupancy={occ:.2f}")
