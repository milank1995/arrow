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

pack0 = decrypt_pack(0)
l1 = pack0[0]
l4 = pack0[3]
l9 = pack0[8]
l14 = pack0[13]
l19 = pack0[18]
l24 = pack0[23]
l29 = pack0[28]
l30 = pack0[29]
print(f"L1: {len(l1['Arrows'])} arrows ({l1['XSize']}x{l1['YSize']})")
print(f"L4 (Shape): {len(l4['Arrows'])} arrows ({l4['XSize']}x{l4['YSize']})")
print(f"L9 (Shape): {len(l9['Arrows'])} arrows ({l9['XSize']}x{l9['YSize']})")
print(f"L14 (Shape): {len(l14['Arrows'])} arrows ({l14['XSize']}x{l14['YSize']})")
print(f"L19 (Shape): {len(l19['Arrows'])} arrows ({l19['XSize']}x{l19['YSize']})")
print(f"L24 (Shape): {len(l24['Arrows'])} arrows ({l24['XSize']}x{l24['YSize']})")
print(f"L29 (Shape): {len(l29['Arrows'])} arrows ({l29['XSize']}x{l29['YSize']})")
print(f"L30 (Chap 1): {len(l30['Arrows'])} arrows ({l30['XSize']}x{l30['YSize']})")

pack4 = decrypt_pack(4)
l5000 = pack4[999]
print(f"L5000 (Finale): {len(l5000['Arrows'])} arrows ({l5000['XSize']}x{l5000['YSize']})")
