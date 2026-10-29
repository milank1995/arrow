import sys
import subprocess
import json
sys.stdout.reconfigure(encoding='utf-8')
import gzip
from collections import defaultdict
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
            pack = decrypt_blob(parts[2])
            levels.extend(pack)
    return levels

shapes = get_pack_levels('test_shape_puzzles')
large = get_pack_levels('test_large_puzzles')
all_shape_levels = shapes + large

with open('scratch/curated_150_plan_v3.json', encoding='utf-8') as f:
    plan = json.load(f)

plan_shapes = [p for p in plan if p['is_shape']]

# Map clean names to shape objects
shape_objs = []
for s in all_shape_levels:
    n = s.get('ShapeName', '').strip()
    if n and not n.startswith('Shape #'): # Prefer named recognizable shapes
        shape_objs.append((n, len(s['Arrows']), s))

print(f"Total recognizable named shapes: {len(shape_objs)}")

# For each plan shape, let's list the top candidate shapes:
for ps in plan_shapes:
    lvl = ps['level']
    pname = ps['name']
    target_arr = ps['arrows']
    
    # 1. Exact name match
    exact_name = [s for s in shape_objs if s[0].lower() == pname.lower()]
    # 2. Substring match
    sub_name = [s for s in shape_objs if (pname.lower() in s[0].lower() or s[0].lower() in pname.lower()) and s not in exact_name]
    # 3. Closest arrow count named shapes
    close_arrow = sorted(shape_objs, key=lambda s: abs(s[1] - target_arr))[:3]
    
    cand = None
    if exact_name:
        exact_name.sort(key=lambda s: abs(s[1] - target_arr))
        cand = exact_name[0]
        match_type = "EXACT NAME"
    elif sub_name:
        sub_name.sort(key=lambda s: abs(s[1] - target_arr))
        cand = sub_name[0]
        match_type = "SUBSTRING NAME"
    else:
        cand = close_arrow[0]
        match_type = "BEST CLOSE ARROW"
        
    print(f"L{lvl:3d}: Plan '{pname}' ({target_arr:3d} arr) -> [{match_type}] '{cand[0]}' ({cand[1]} arr)")
