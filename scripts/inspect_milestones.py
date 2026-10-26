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

def check_lvl(lvl_num):
    pack_idx = (lvl_num - 1) // 1000
    idx_in_pack = (lvl_num - 1) % 1000
    pack = decrypt_pack(pack_idx)
    lvl_obj = pack[idx_in_pack]
    arrows = lvl_obj.get('Arrows', [])
    x_size = lvl_obj.get('XSize')
    y_size = lvl_obj.get('YSize')
    print(f"Level {lvl_num:5d}: Pack {pack_idx:02d}, Index {idx_in_pack:3d}, Grid {x_size:2d}x{y_size:2d}, Arrow count: {len(arrows):3d}")

for l in [1, 5, 10, 20, 25, 26, 50, 100, 500, 1000, 5000, 10000, 14040]:
    check_lvl(l)
