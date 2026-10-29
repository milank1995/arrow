import os
import sys
import json
import gzip
import glob
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend

sys.stdout.reconfigure(encoding='utf-8')
AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'

def decrypt_pack(file_path):
    with open(file_path, 'rb') as f:
        raw = f.read()
    iv = raw[:16]
    ct = raw[16:]
    cipher = Cipher(algorithms.AES(AES_KEY), modes.CBC(iv), backend=default_backend())
    decryptor = cipher.decryptor()
    pt = decryptor.update(ct) + decryptor.finalize()
    pad = pt[-1]
    pt = pt[:-pad]
    return json.loads(gzip.decompress(pt).decode('utf-8'))

def get_signature(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    items = []
    for a in lvl['Arrows']:
        idxs = a['Indices']
        items.append((idxs[0], idxs[-1], a['Dx'], a['Dy'], a.get('BendCount', 0)))
    return (xs, ys, tuple(sorted(items)))

categories = {
    'existing_1k': r'arrow_native/app/src/main/assets/levels/test_existing_1k',
    'unity_normal': r'arrow_native/app/src/main/assets/levels/test_unity_normal',
    'shape_puzzles': r'arrow_native/app/src/main/assets/levels/test_shape_puzzles',
    'large_puzzles': r'arrow_native/app/src/main/assets/levels/test_large_puzzles'
}

cat_levels = {}
for cat_name, folder in categories.items():
    cat_levels[cat_name] = []
    dat_files = sorted(glob.glob(os.path.join(folder, "level_pack_*.dat")))
    for df in dat_files:
        levels = decrypt_pack(df)
        cat_levels[cat_name].extend(levels)
    print(f"Loaded {cat_name}: {len(cat_levels[cat_name])} levels")

# Check for duplicates across categories
sig_to_cats = defaultdict(list)
for cat_name, lvls in cat_levels.items():
    for idx, lvl in enumerate(lvls):
        sig = get_signature(lvl)
        sig_to_cats[sig].append((cat_name, idx + 1, lvl.get('Hash', ''), lvl.get('ShapeName', '')))

# Pairwise overlap
cat_names = list(categories.keys())
overlap_found = False
for i in range(len(cat_names)):
    for j in range(i + 1, len(cat_names)):
        c1, c2 = cat_names[i], cat_names[j]
        # find duplicates
        dups = []
        for sig, occurrences in sig_to_cats.items():
            in_c1 = [o for o in occurrences if o[0] == c1]
            in_c2 = [o for o in occurrences if o[0] == c2]
            if in_c1 and in_c2:
                dups.append((in_c1, in_c2))
        print(f"Duplicates between [{c1}] and [{c2}]: {len(dups)}")
        if dups:
            overlap_found = True
            for d1, d2 in dups[:15]:
                print(f"   {c1} #{d1[0][1]} ({d1[0][2]}, {d1[0][3]}) == {c2} #{d2[0][1]} ({d2[0][2]}, {d2[0][3]})")
            if len(dups) > 15:
                print(f"   ... and {len(dups)-15} more")
