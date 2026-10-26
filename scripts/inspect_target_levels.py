import os
import sys
sys.path.append('.')
import json
from scripts.inspect_l14040 import decrypt_bytes

target_levels = [1, 5, 10, 15, 20, 25, 26, 30, 50, 100, 500, 1000, 5000, 10000, 14040]

for lvl in target_levels:
    pidx = (lvl - 1) // 1000
    idx = (lvl - 1) % 1000
    with open(f'arrow_native/app/src/main/assets/levels/level_pack_{pidx:02d}.dat', 'rb') as f:
        enc = f.read()
    pack = json.loads(decrypt_bytes(enc).decode('utf-8'))
    ldata = pack[idx]
    xs = ldata['XSize']
    ys = ldata['YSize']
    arrs = ldata['Arrows']
    print(f"Level {lvl:5d}: Grid={xs:2d}x{ys:2d}, ArrowCount={len(arrs):3d}, Category={ldata.get('Category')}, Hash={ldata.get('Hash')}")
