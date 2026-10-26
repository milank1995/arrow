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

all_levels = []
for p in range(15):
    all_levels.extend(decrypt_pack(p))

print(f"Total levels: {len(all_levels)}")
chunk_size = 500
for start in range(0, len(all_levels), chunk_size):
    end = min(start + chunk_size, len(all_levels))
    counts = [len(all_levels[i].get('Arrows', [])) for i in range(start, end)]
    avg = sum(counts) / len(counts)
    min_c = min(counts)
    max_c = max(counts)
    print(f"Levels {start+1:5d} - {end:5d}: min={min_c:3d}, avg={avg:5.1f}, max={max_c:3d}")
