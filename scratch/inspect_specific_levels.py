import sys
import subprocess
import json
import gzip
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

sys.stdout.reconfigure(encoding='utf-8')
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
            pack = decrypt_blob(parts[2])
            levels.extend(pack)
    return levels

shapes = get_pack_levels('test_shape_puzzles')
large = get_pack_levels('test_large_puzzles')
one_k = get_pack_levels('test_existing_1k')
unity = get_pack_levels('test_unity_normal')

# 1. Check L3 and L4 in one_k and unity
print("1K levels with 8 arrows:", len([l for l in one_k if len(l['Arrows']) == 8]))
print("1K levels with 12 arrows:", len([l for l in one_k if len(l['Arrows']) == 12]))
print("Unity levels with 8 arrows:", len([l for l in unity if len(l['Arrows']) == 8]))
print("Unity levels with 12 arrows:", len([l for l in unity if len(l['Arrows']) == 12]))

# 2. Check Sofa in large
sofas = [l for l in large if 'sofa' in l.get('ShapeName', '').lower()]
print("Sofa levels in large:", [(l.get('ShapeName'), len(l['Arrows'])) for l in sofas])

# 3. Check shapes around 60 arrows
shapes_60 = [l for l in shapes if len(l['Arrows']) == 60]
print("Shapes with exactly 60 arrows:", [(l.get('ShapeName'), len(l['Arrows'])) for l in shapes_60])

# 4. Check shapes around 70 arrows
shapes_70 = [l for l in shapes if len(l['Arrows']) == 70]
print("Shapes with exactly 70 arrows:", [(l.get('ShapeName'), len(l['Arrows'])) for l in shapes_70])

# 5. Check shapes around 64 arrows
shapes_64 = [l for l in shapes if len(l['Arrows']) == 64]
print("Shapes with exactly 64 arrows:", [(l.get('ShapeName'), len(l['Arrows'])) for l in shapes_64])

# 6. Check shapes around 80 arrows
shapes_80 = [l for l in shapes if len(l['Arrows']) == 80]
print("Shapes with exactly 80 arrows:", [(l.get('ShapeName'), len(l['Arrows'])) for l in shapes_80])

# 7. Check shapes around 54 arrows
shapes_54 = [l for l in shapes if len(l['Arrows']) == 54]
print("Shapes with exactly 54 arrows:", [(l.get('ShapeName'), len(l['Arrows'])) for l in shapes_54])

# 8. Check Large 250 arrows (L100)
large_250 = [l for l in large if len(l['Arrows']) == 250]
print("Large with 250 arrows:", [(l.get('ShapeName'), len(l['Arrows'])) for l in large_250])
if not large_250:
    print("Large puzzle arrow counts:", sorted(set(len(l['Arrows']) for l in large)))
