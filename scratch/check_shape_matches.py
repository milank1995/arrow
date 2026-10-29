import json
import gzip
import subprocess
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

shapes = decrypt_blob('df660515e10e0d3fb09bc7374ac2e40025da06de')
large_p = decrypt_blob('e7b51b086143380b756ef7f35b7e7b612962ec50')
one_k = decrypt_blob('24d00416f1df3786d07da493d94eb3e53df74701')
unity_n = decrypt_blob('62f9bd8a0a265e9946eaab39b69d90e6aab385b0')

with open('scratch/curated_150_plan_v3.json', encoding='utf-8') as f:
    plan = json.load(f)

# Collect all shapes by name
shape_dict = {}
for s in shapes + large_p:
    name = s.get('ShapeName')
    if name:
        name_lower = name.strip().lower()
        if name_lower not in shape_dict:
            shape_dict[name_lower] = []
        shape_dict[name_lower].append(s)

print(f"Loaded {len(shapes)} shapes, {len(large_p)} large puzzles, {len(one_k)} 1k, {len(unity_n)} unity_n")
print(f"Unique shape names: {len(shape_dict)}")

# Now test matching each planned shape
plan_shapes = [p for p in plan if p['is_shape']]
for p in plan_shapes:
    lvl = p['level']
    name = p['name']
    target_arr = p['arrows']
    name_l = name.strip().lower()
    
    matches = shape_dict.get(name_l, [])
    if not matches:
        # substring search
        matches = [s for k, v in shape_dict.items() if name_l in k or k in name_l for s in v]
    
    print(f"L{lvl} '{name}' (target {target_arr}): found {len(matches)} matches: {[(m.get('ShapeName'), len(m['Arrows'])) for m in matches[:5]]}")
