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

def solve_level(lvl_obj):
    x_size = lvl_obj['XSize']
    y_size = lvl_obj['YSize']
    arrows = lvl_obj['Arrows']
    
    # Each arrow has Indices and Dx, Dy
    # Arrow can fly if ray from head along Dx, Dy doesn't hit any other arrow's indices
    # Let's track active arrows and their occupied cells
    active_arrows = list(range(len(arrows)))
    occupied = {} # (x, y) -> arrow_idx
    
    arrow_heads = []
    arrow_dirs = []
    arrow_cells = []
    
    for i, arr in enumerate(arrows):
        dx = arr['Dx']
        dy = -arr['Dy'] # canvas dy
        indices = arr['Indices']
        cells = set()
        for idx in indices:
            ux = idx % x_size
            uy = idx // x_size
            cx = ux
            cy = (y_size - 1) - uy
            cells.add((cx, cy))
            occupied[(cx, cy)] = i
        # head is last index reversed: in LevelLoader body is indicesArray reversed, head is body.last() -> so indices[0] is head!
        head_idx = indices[0]
        hx = head_idx % x_size
        hy = (y_size - 1) - (head_idx // x_size)
        arrow_heads.append((hx, hy))
        arrow_dirs.append((dx, dy))
        arrow_cells.append(cells)
        
    cleared = 0
    total = len(arrows)
    
    while active_arrows:
        progress = False
        for a_idx in list(active_arrows):
            hx, hy = arrow_heads[a_idx]
            dx, dy = arrow_dirs[a_idx]
            # Raycast
            cur_x = hx + dx
            cur_y = hy + dy
            blocked = False
            while 0 <= cur_x < x_size and 0 <= cur_y < y_size:
                if (cur_x, cur_y) in occupied and occupied[(cur_x, cur_y)] != a_idx:
                    blocked = True
                    break
                cur_x += dx
                cur_y += dy
            if not blocked:
                # Can clear!
                for c in arrow_cells[a_idx]:
                    if c in occupied and occupied[c] == a_idx:
                        del occupied[c]
                active_arrows.remove(a_idx)
                cleared += 1
                progress = True
                break
        if not progress:
            return False, cleared, total
            
    return True, cleared, total

test_levels = [1, 5, 10, 20, 25, 26, 50, 100, 500, 1000, 5000, 10000, 14040]
cached_packs = {}
for lvl_num in test_levels:
    p_idx = (lvl_num - 1) // 1000
    i_idx = (lvl_num - 1) % 1000
    if p_idx not in cached_packs:
        cached_packs[p_idx] = decrypt_pack(p_idx)
    lvl_obj = cached_packs[p_idx][i_idx]
    solvable, cleared, total = solve_level(lvl_obj)
    status = "SOLVABLE" if solvable else f"FAILED ({cleared}/{total})"
    print(f"Level {lvl_num:5d}: {status} (Total arrows: {total})")
