import os
import sys
import json
import gzip
import re
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend
import UnityPy

sys.stdout.reconfigure(encoding='utf-8')
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'
BASE_OUT = r'arrow_native/app/src/main/assets/levels'

SHAPE_NAMES_MAP = {
    'sofa': 'Sofa',
    'thinker': 'The Thinker',
    'stoneidol': 'Stone Idol',
    'treasurechest': 'Treasure Chest',
    'thestarrynight': 'The Starry Night',
    'arcdetriomphe': 'Arc de Triomphe',
    'pirate': 'Pirate Ship',
    'sydneyopera': 'Sydney Opera House',
    'tajmahal': 'Taj Mahal',
    'towerofpisa': 'Leaning Tower of Pisa',
    'zhentan': 'Detective',
    'sphinx': 'The Great Sphinx'
}

def encrypt_pack(json_obj):
    raw_json = json.dumps(json_obj, separators=(',', ':')).encode('utf-8')
    gzipped = gzip.compress(raw_json)
    pad_len = 16 - (len(gzipped) % 16)
    padded = gzipped + bytes([pad_len] * pad_len)
    iv = os.urandom(16)
    cipher = Cipher(algorithms.AES(AES_KEY), modes.CBC(iv), backend=default_backend())
    encryptor = cipher.encryptor()
    ciphertext = encryptor.update(padded) + encryptor.finalize()
    return iv + ciphertext

def solve_level(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows: return False
    occupied = {}
    heads, dirs, cells_list = [], [], []
    for a in arrows:
        idxs = a['Indices']
        if not idxs or len(idxs) < 2: continue
        i = len(heads)
        hx, hy = idxs[0] % xs, (ys - 1) - (idxs[0] // xs)
        heads.append((hx, hy))
        dirs.append((a['Dx'], -a['Dy']))
        cset = set((idx % xs, (ys - 1) - (idx // xs)) for idx in idxs)
        for c in cset: occupied[c] = i
        cells_list.append(cset)
    active = list(range(len(heads)))
    if not active: return False
    while active:
        prog = False
        for i in list(active):
            hx, hy = heads[i]
            dx, dy = dirs[i]
            cur_x, cur_y = hx + dx, hy + dy
            blocked = False
            while 0 <= cur_x < xs and 0 <= cur_y < ys:
                if (cur_x, cur_y) in occupied and occupied[(cur_x, cur_y)] != i:
                    blocked = True
                    break
                cur_x += dx
                cur_y += dy
            if not blocked:
                for c in cells_list[i]:
                    if c in occupied and occupied[c] == i:
                        del occupied[c]
                active.remove(i)
                prog = True
                break
        if not prog: return False
    return True

def crop_level(lvl, padding=1):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows: return lvl
    all_indices = [p for a in arrows for p in a['Indices']]
    if not all_indices: return lvl
    min_ux = min(idx % xs for idx in all_indices)
    max_ux = max(idx % xs for idx in all_indices)
    min_uy = min(idx // xs for idx in all_indices)
    max_uy = max(idx // xs for idx in all_indices)
    if min_ux == 0 and min_uy == 0 and max_ux == xs - 1 and max_uy == ys - 1:
        return lvl
    new_min_ux = max(0, min_ux - padding)
    new_max_ux = min(xs - 1, max_ux + padding)
    new_min_uy = max(0, min_uy - padding)
    new_max_uy = min(ys - 1, max_uy + padding)
    new_xs = new_max_ux - new_min_ux + 1
    new_ys = new_max_uy - new_min_uy + 1
    new_arrows = []
    for a in arrows:
        new_indices = [((idx // xs) - new_min_uy) * new_xs + ((idx % xs) - new_min_ux) for idx in a['Indices']]
        new_a = dict(a)
        new_a['Indices'] = new_indices
        new_arrows.append(new_a)
    cropped_lvl = dict(lvl)
    cropped_lvl['XSize'] = new_xs
    cropped_lvl['YSize'] = new_ys
    cropped_lvl['Arrows'] = new_arrows
    return cropped_lvl

packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'
with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

raw_large = []
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        try:
            raw = json.loads(gzip.decompress(b))
            arr_count = len(raw.get('Arrows', []))
            if 250 <= arr_count <= 450:
                xs = raw['XSize']
                ys = raw['YSize']
                arrows_out = []
                for a in raw.get('Arrows', []):
                    pts = a.get('Points', [])
                    if not pts or len(pts) < 2: continue
                    hx, hy = pts[0] % xs, pts[0] // xs
                    nx, ny = pts[1] % xs, pts[1] // xs
                    dx = 1 if hx > nx else (-1 if hx < nx else 0)
                    dy = 1 if hy > ny else (-1 if hy < ny else 0)
                    arrows_out.append({
                        'Indices': pts,
                        'Dx': dx,
                        'Dy': dy,
                        'BendCount': len(pts) - 2 if len(pts) > 2 else 0
                    })
                if len(arrows_out) >= 250:
                    shape_name = ""
                    lower_name = name.lower()
                    for k, v in SHAPE_NAMES_MAP.items():
                        if k in lower_name:
                            shape_name = v
                            break
                    lvl_obj = {
                        'XSize': xs,
                        'YSize': ys,
                        'Arrows': arrows_out,
                        'Hash': name,
                        'ShapeName': shape_name,
                        'MaxHearts': raw.get('MaxHearts', 3)
                    }
                    raw_large.append(lvl_obj)
        except:
            pass

print(f"Total raw large candidates: {len(raw_large)}")

# Deduplicate
def get_sig(lvl):
    return (lvl['XSize'], lvl['YSize'], tuple(sorted(a['Indices'][0] for a in lvl['Arrows'])))

seen = set()
unique_large = []
for lvl in raw_large:
    c = crop_level(lvl)
    sig = get_sig(c)
    if sig not in seen:
        seen.add(sig)
        if solve_level(c):
            unique_large.append(c)

print(f"100% Solvable unique large puzzles: {len(unique_large)}")

# Sort by arrow count, then by grid area
unique_large.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))

# Write encrypted pack
target_dir = os.path.join(BASE_OUT, "test_large_puzzles")
os.makedirs(target_dir, exist_ok=True)
for f in os.listdir(target_dir):
    if f.endswith('.dat'):
        os.remove(os.path.join(target_dir, f))

pack_data = []
manifest_summary = []
for idx, lvl in enumerate(unique_large):
    lvl_num = idx + 1
    pack_data.append({
        'XSize': lvl['XSize'],
        'YSize': lvl['YSize'],
        'MaxHearts': lvl.get('MaxHearts', 3),
        'Hash': lvl.get('Hash', ''),
        'ShapeName': lvl.get('ShapeName', ''),
        'Arrows': lvl['Arrows']
    })
    manifest_summary.append({
        'Level': lvl_num,
        'ArrowCount': len(lvl['Arrows']),
        'XSize': lvl['XSize'],
        'YSize': lvl['YSize'],
        'ShapeName': lvl.get('ShapeName', ''),
        'Hash': lvl.get('Hash', '')
    })

encrypted_bytes = encrypt_pack(pack_data)
file_name = "level_pack_00.dat"
with open(os.path.join(target_dir, file_name), 'wb') as f:
    f.write(encrypted_bytes)

pack_manifest = {
    'totalLevels': len(unique_large),
    'chunkSize': 1000,
    'packsCount': 1,
    'packs': [{
        'packIndex': 0,
        'fileName': file_name,
        'levelsCount': len(unique_large),
        'startLevel': 1,
        'endLevel': len(unique_large),
        'fileSizeBytes': len(encrypted_bytes)
    }]
}
with open(os.path.join(target_dir, 'pack_manifest.json'), 'w', encoding='utf-8') as f:
    json.dump(pack_manifest, f, indent=2)

with open('manifest_large_puzzles.json', 'w', encoding='utf-8') as f:
    json.dump(manifest_summary, f, indent=2, ensure_ascii=False)

print(f"Successfully generated {target_dir}/{file_name} ({len(encrypted_bytes)/1024:.1f} KB)")
print(f"Successfully written manifest_large_puzzles.json with {len(manifest_summary)} levels")
print(f"Arrow range: {manifest_summary[0]['ArrowCount']} to {manifest_summary[-1]['ArrowCount']}")
shape_count = sum(1 for m in manifest_summary if m['ShapeName'])
print(f"Levels with Shape Name: {shape_count} of {len(manifest_summary)}")
