import subprocess
import gzip
import json
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

unity = decrypt_blob('62f9bd8a0a265e9946eaab39b69d90e6aab385b0')
for i in range(25, 45):
    l = unity[i]
    print(f"Unity idx {i} (Lvl {i+1}): {len(l['Arrows'])} arrows, {l['XSize']}x{l['YSize']}, shape='{l.get('ShapeName')}', hash='{l.get('Hash')}'")
