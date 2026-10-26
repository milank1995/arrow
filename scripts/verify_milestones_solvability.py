import json
from scripts.inspect_l14040 import decrypt_bytes
from scripts.test_solvability import is_solvable

test_lvls = [1, 5, 10, 15, 20, 25, 26, 30, 50, 100, 500, 1000, 5000, 10000, 14040]

print("--- Verifying Milestone Levels in Encrypted Packs ---")
for lvl in test_lvls:
    pidx = (lvl - 1) // 1000
    idx = (lvl - 1) % 1000
    with open(f'arrow_native/app/src/main/assets/levels/level_pack_{pidx:02d}.dat', 'rb') as f:
        enc = f.read()
    pack = json.loads(decrypt_bytes(enc).decode('utf-8'))
    ldata = pack[idx]
    
    xs = ldata['XSize']
    ys = ldata['YSize']
    arrs = ldata['Arrows']
    
    # Check solvability
    solv, reason = is_solvable(ldata)
    
    print(f"Level {lvl:5d}: Grid={xs:2d}x{ys:2d}, ArrowCount={len(arrs):3d}, Solvable={solv} ({reason}), Hash={ldata.get('Hash')}")
