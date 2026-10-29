import sys, subprocess, json, gzip
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from render_helper import render_arrows_to_image

sys.stdout.reconfigure(encoding='utf-8')
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

def expand_indices(idxs, xs):
    if len(idxs) < 2: return list(idxs)
    full = [idxs[0]]
    for i in range(len(idxs)-1):
        x1, y1 = idxs[i] % xs, idxs[i] // xs
        x2, y2 = idxs[i+1] % xs, idxs[i+1] // xs
        step_x = 0 if x1 == x2 else (1 if x2 > x1 else -1)
        step_y = 0 if y1 == y2 else (1 if y2 > y1 else -1)
        cur_x, cur_y = x1, y1
        while (cur_x, cur_y) != (x2, y2):
            if cur_x != x2: cur_x += step_x
            elif cur_y != y2: cur_y += step_y
            full.append(cur_y * xs + cur_x)
    return full

shapes = get_pack_levels('test_shape_puzzles')

for l in shapes:
    h = l.get('Hash') or ''
    if '埃菲尔铁塔' in h: # Eiffel Tower (58 arrows, 49x75)
        xs = l['XSize']
        ys = l['YSize']
        arrows = []
        for a in l['Arrows']:
            exp = expand_indices(a['Indices'], xs)
            # Canvas coords:
            coords = [(idx % xs, (ys - 1) - (idx // xs)) for idx in exp]
            arrows.append({'canvas_coords': coords, 'cdx': a['Dx'], 'cdy': -a['Dy']})
        render_arrows_to_image(arrows, xs, ys, 'candidate_eiffel_tower.png')
    elif '拆自行车' in h: # Bicycle (65 arrows, 62x58)
        xs = l['XSize']
        ys = l['YSize']
        arrows = []
        for a in l['Arrows']:
            exp = expand_indices(a['Indices'], xs)
            coords = [(idx % xs, (ys - 1) - (idx // xs)) for idx in exp]
            arrows.append({'canvas_coords': coords, 'cdx': a['Dx'], 'cdy': -a['Dy']})
        render_arrows_to_image(arrows, xs, ys, 'candidate_bicycle.png')
    elif 'Clock-8cd7' in h: # Clock (62 arrows, 51x57)
        xs = l['XSize']
        ys = l['YSize']
        arrows = []
        for a in l['Arrows']:
            exp = expand_indices(a['Indices'], xs)
            coords = [(idx % xs, (ys - 1) - (idx // xs)) for idx in exp]
            arrows.append({'canvas_coords': coords, 'cdx': a['Dx'], 'cdy': -a['Dy']})
        render_arrows_to_image(arrows, xs, ys, 'candidate_clock.png')
    elif '海马-e7ee' in h: # Seahorse (66 arrows, 60x60)
        xs = l['XSize']
        ys = l['YSize']
        arrows = []
        for a in l['Arrows']:
            exp = expand_indices(a['Indices'], xs)
            coords = [(idx % xs, (ys - 1) - (idx // xs)) for idx in exp]
            arrows.append({'canvas_coords': coords, 'cdx': a['Dx'], 'cdy': -a['Dy']})
        render_arrows_to_image(arrows, xs, ys, 'candidate_seahorse.png')
