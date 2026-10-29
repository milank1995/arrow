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

targets = {
    'Clock-8cd7': 'clean_clock.png',
    '中国结-3251': 'clean_chinese_knot.png',
    '苹果-7bf0': 'clean_apple.png',
    'winecat-189c': 'clean_winecat.png',
    '郁金香-a5a4': 'clean_tulip.png',
}

for l in shapes:
    h = l.get('Hash') or ''
    for k, out_file in targets.items():
        if k in h:
            xs = l['XSize']
            ys = l['YSize']
            arrows = []
            for a in l['Arrows']:
                exp = expand_indices(a['Indices'], xs)
                coords = [(idx % xs, (ys - 1) - (idx // xs)) for idx in exp]
                arrows.append({'canvas_coords': coords, 'cdx': a['Dx'], 'cdy': -a['Dy']})
            render_arrows_to_image(arrows, xs, ys, out_file)
