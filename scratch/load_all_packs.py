import subprocess
import json
import gzip
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'

def decrypt_blob(blob_id):
    p = subprocess.Popen(['git', 'cat-file', 'blob', blob_id], stdout=subprocess.PIPE)
    data, _ = p.communicate()
    iv, ct = data[:16], data[16:]
    cipher = Cipher(algorithms.AES(AES_KEY), modes.CBC(iv))
    dec = cipher.decryptor()
    pt = dec.update(ct) + dec.finalize()
    pad_len = pt[-1]
    pt = pt[:-pad_len]
    return json.loads(gzip.decompress(pt))

def get_pack_levels(prefix):
    tree = subprocess.check_output(['git', 'ls-tree', f'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/{prefix}'], encoding='utf-8')
    levels = []
    for line in tree.strip().splitlines():
        parts = line.split()
        if len(parts) >= 4 and parts[3].startswith('level_pack_') and parts[3].endswith('.dat'):
            print(f"Loading {prefix}/{parts[3]} (blob {parts[2]})...")
            pack = decrypt_blob(parts[2])
            levels.extend(pack)
    return levels

shapes = get_pack_levels('test_shape_puzzles')
large = get_pack_levels('test_large_puzzles')
one_k = get_pack_levels('test_existing_1k')
unity = get_pack_levels('test_unity_normal')

print(f"\nTotal loaded: Shapes={len(shapes)}, Large={len(large)}, 1K={len(one_k)}, UnityNormal={len(unity)}")

# Check shapes with names
named_shapes = {}
for idx, s in enumerate(shapes):
    n = s.get('ShapeName', '').strip()
    if n:
        named_shapes[n.lower()] = (idx, s)

print(f"Named shapes in test_shape_puzzles: {len(named_shapes)}")

# Check shapes in 1K
named_1k = {}
for idx, s in enumerate(one_k):
    n = s.get('ShapeName', '').strip()
    if n:
        named_1k[n.lower()] = (idx, s)
print(f"Named shapes in 1K: {len(named_1k)}")

# Check shapes in large
named_large = {}
for idx, s in enumerate(large):
    n = s.get('ShapeName', '').strip()
    if n:
        named_large[n.lower()] = (idx, s)
print(f"Named shapes in large: {len(named_large)}")
