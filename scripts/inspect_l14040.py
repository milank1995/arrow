import json
import gzip
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding

AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'

def decrypt_bytes(encrypted_bytes):
    iv = encrypted_bytes[:16]
    ciphertext = encrypted_bytes[16:]
    cipher = Cipher(algorithms.AES(AES_KEY), modes.CBC(iv))
    decryptor = cipher.decryptor()
    padded_data = decryptor.update(ciphertext) + decryptor.finalize()
    unpadder = padding.PKCS7(128).unpadder()
    compressed = unpadder.update(padded_data) + unpadder.finalize()
    return gzip.decompress(compressed)

with open('arrow_native/app/src/main/assets/levels/level_pack_14.dat', 'rb') as f:
    enc = f.read()

levels = json.loads(decrypt_bytes(enc).decode('utf-8'))
print(f"Total levels in pack 14: {len(levels)}")
last_lvl = levels[-1]
print("Last level (Level 14040):")
print("  XSize:", last_lvl.get('XSize'), "YSize:", last_lvl.get('YSize'))
print("  Hash:", last_lvl.get('Hash'))
print("  Category:", last_lvl.get('Category'))
print("  ArrowCount:", len(last_lvl.get('Arrows', [])))
for i, a in enumerate(last_lvl.get('Arrows', [])):
    print(f"   arrow {i}: head=({a.get('X')}, {a.get('Y')}), len={len(a.get('Indices', []))}, dx={a.get('Dx')}, dy={a.get('Dy')}")
