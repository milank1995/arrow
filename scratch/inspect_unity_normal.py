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

manifest_raw = subprocess.check_output(['git', 'show', 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_unity_normal/pack_manifest.json'], encoding='utf-8')
manifest = json.loads(manifest_raw)
print(f"Unity normal manifest keys: {manifest.keys() if isinstance(manifest, dict) else len(manifest)}")


# Check packs 0 to 8
tree = subprocess.check_output(['git', 'ls-tree', 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_unity_normal'], encoding='utf-8')
pack_blobs = {}
for line in tree.strip().splitlines():
    parts = line.split()
    if len(parts) >= 4 and parts[3].startswith('level_pack_'):
        pack_blobs[parts[3]] = parts[2]

print("Pack blobs:", pack_blobs)

# Check pack 0
pack0 = decrypt_blob(pack_blobs['level_pack_00.dat'])
print(f"Pack 0 has {len(pack0)} levels.")
print("Sample level 0:", {k: v for k, v in pack0[0].items() if k != 'Arrows'})
print("Sample level 4 (L5):", {k: v for k, v in pack0[4].items() if k != 'Arrows'}, "Arrows count:", len(pack0[4]['Arrows']))
print("Sample level 9 (L10):", {k: v for k, v in pack0[9].items() if k != 'Arrows'}, "Arrows count:", len(pack0[9]['Arrows']))
print("Sample level 19 (L20):", {k: v for k, v in pack0[19].items() if k != 'Arrows'}, "Arrows count:", len(pack0[19]['Arrows']))

# Print first 25 levels arrow count in pack 0:
pack0_counts = [len(l['Arrows']) for l in pack0[:25]]
print("Pack0 first 25 arrow counts:", pack0_counts)
