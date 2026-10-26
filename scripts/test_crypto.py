import os
import gzip
import json
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding

# Secret Key (32 bytes = AES-256)
AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!' # 32 bytes

def encrypt_pack(levels_data):
    # 1. Compress JSON with Gzip
    json_bytes = json.dumps(levels_data, separators=(',', ':')).encode('utf-8')
    compressed = gzip.compress(json_bytes, compresslevel=9)
    
    # 2. PKCS7 Padding
    padder = padding.PKCS7(128).padder()
    padded_data = padder.update(compressed) + padder.finalize()
    
    # 3. Random 16-byte IV
    iv = os.urandom(16)
    
    # 4. AES-256-CBC Encrypt
    cipher = Cipher(algorithms.AES(AES_KEY), modes.CBC(iv))
    encryptor = cipher.encryptor()
    ciphertext = encryptor.update(padded_data) + encryptor.finalize()
    
    # Return IV + ciphertext
    return iv + ciphertext

def decrypt_pack(encrypted_bytes):
    iv = encrypted_bytes[:16]
    ciphertext = encrypted_bytes[16:]
    cipher = Cipher(algorithms.AES(AES_KEY), modes.CBC(iv))
    decryptor = cipher.decryptor()
    padded_data = decryptor.update(ciphertext) + decryptor.finalize()
    
    unpadder = padding.PKCS7(128).unpadder()
    compressed = unpadder.update(padded_data) + unpadder.finalize()
    
    json_bytes = gzip.decompress(compressed)
    return json.loads(json_bytes.decode('utf-8'))

# Test with dummy data
sample = [{'level': 1, 'xSize': 5, 'ySize': 5, 'arrows': []}]
enc = encrypt_pack(sample)
dec = decrypt_pack(enc)
assert dec == sample
print("Encryption & Decryption test passed! Encrypted length:", len(enc))
