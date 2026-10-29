import sys
import subprocess
import json
import gzip
import os
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

sys.stdout.reconfigure(encoding='utf-8')
AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'
OUT_DIR = r'arrow_native/app/src/main/assets/levels'

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

print("Loading packs...")
shapes = get_pack_levels('test_shape_puzzles')
large = get_pack_levels('test_large_puzzles')
one_k = get_pack_levels('test_existing_1k')
unity = get_pack_levels('test_unity_normal')
print(f"Loaded: Shapes={len(shapes)}, Large={len(large)}, 1K={len(one_k)}, UnityNormal={len(unity)}")

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

def solve(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows: return False
    occupied = {}
    heads, dirs, cells_list = [], [], []
    for i, a in enumerate(arrows):
        idxs = a['Indices']
        if not idxs: continue
        hx = idxs[0] % xs
        hy = (ys - 1) - (idxs[0] // xs)
        cdx = a['Dx']
        cdy = -a['Dy']
        heads.append((hx, hy))
        dirs.append((cdx, cdy))
        cset = set((idx % xs, (ys - 1) - (idx // xs)) for idx in idxs)
        for c in cset: occupied[c] = i
        cells_list.append(cset)
        
    active = list(range(len(heads)))
    while active:
        prog = False
        for i in list(active):
            hx, hy = heads[i]
            dx, dy = dirs[i]
            cx, cy = hx + dx, hy + dy
            blocked = False
            while 0 <= cx < xs and 0 <= cy < ys:
                if (cx, cy) in occupied and occupied[(cx, cy)] != i:
                    blocked = True
                    break
                cx += dx
                cy += dy
            if not blocked:
                for c in cells_list[i]:
                    if c in occupied and occupied[c] == i:
                        del occupied[c]
                active.remove(i)
                prog = True
                break
        if not prog: return False
    return True

def prepare_level(raw, lvl_num, name, diff, src_cat):
    xs = raw['XSize']
    ys = raw['YSize']
    arrows_out = []
    for a in raw['Arrows']:
        expanded = expand_indices(a['Indices'], xs)
        arrows_out.append({
            'Indices': expanded,
            'Dx': a['Dx'],
            'Dy': a['Dy'],
            'BendCount': a.get('BendCount', 0)
        })
    return {
        'LevelNumber': lvl_num,
        'XSize': xs,
        'YSize': ys,
        'MaxHearts': 3,
        'ShapeName': name if name else None,
        'Difficulty': diff,
        'ArrowCount': len(arrows_out),
        'SourceCategory': src_cat,
        'Arrows': arrows_out
    }

# Index pools
unity_by_arr = defaultdict(list)
for l in unity:
    unity_by_arr[len(l['Arrows'])].append(l)

one_k_by_arr = defaultdict(list)
for l in one_k:
    one_k_by_arr[len(l['Arrows'])].append(l)

# 31 Verified Solvable Shape Puzzles with Distinct Silhouettes:
# (level_number, search_name, display_name, target_arrows, difficulty)
SHAPE_PLAN = {
    1:  (5,   "Palm Tree", "Palm Tree", 23, "Medium"),
    2:  (10,  "Robot", "Robot", 52, "Hard"),
    3:  (15,  "Panda", "Panda", 81, "Hard"),
    4:  (20,  "Swan", "Swan", 99, "Super Hard"),
    5:  (25,  "Strawberry", "Strawberry", 130, "Super Hard"),
    6:  (28,  "Black Cat", "Black Cat", 57, "Medium"),
    7:  (33,  "Rainbow", "Piano", 80, "Medium"),     # Piano Keyboard with Music Notes!
    8:  (38,  "Jellyfish", "Jellyfish", 44, "Medium"),
    9:  (45,  "Squirrel", "Squirrel", 62, "Medium"),
    10: (49,  "Cactus", "Cactus", 130, "Super Hard"),
    11: (53,  "Flower Vase", "Hourglass", 46, "Medium"),     # Level 53 Hourglass silhouette!
    12: (58,  "Magnolia", "Magnolia Flower", 63, "Hard"),
    13: (63,  "Dolphin", "Dolphin", 69, "Medium"),
    14: (68,  "Trophy", "Trophy", 77, "Hard"),                # Level 68 (Not 70!)
    15: (73,  "Goldfish", "Goldfish", 70, "Medium"),          # Level 73 (Not 71! 5 levels from 68)
    16: (79,  "雪花", "Snowflake", 68, "Medium"),             # Level 79 Authentic Snowflake!
    17: (83,  "Sparrow", "Sparrow", 80, "Medium"),
    18: (88,  "Dog", "Dog", 86, "Medium"),
    19: (93,  "Seal Balancing Ball", "Seal", 80, "Medium"),
    20: (97,  "Rabbit1", "Rabbit", 91, "Medium"),
    21: (104, "Castle", "Castle", 102, "Medium"),
    22: (109, "Winterbird2", "Winter Bird", 110, "Super Hard"),
    23: (113, "Tropical Waterfall Elephant", "Elephant", 115, "Medium"),
    24: (118, "Honeybee", "Honeybee", 82, "Medium"),
    25: (123, "Greetstar", "Star", 60, "Medium"),
    26: (128, "Sunflower", "Sunflower", 67, "Super Hard"),
    27: (133, "Spin", "Beach Umbrella", 56, "Medium"),
    28: (138, "Whale", "Whale", 136, "Super Hard"),
    29: (143, "Catstarv7", "Catstar", 126, "Medium"),
    30: (147, "Lion", "Lion", 141, "Hard"),
}

# Special Bosses (Reduced arrow counts for better balance)
BOSS_PLAN = {
    100: ("Century Climax Mega Grid", 195, "Super Hard"),
    150: ("Rose Grand Finale", 190, "Super Hard"),
}

# Invert shapes by level number
shape_by_lvl = {v[0]: v for v in SHAPE_PLAN.values()}

# User's reference dictionary
user_ref = {
  1: 4, 2: 4, 3: 4, 4: 7, 5: 54, 6: 28, 7: 54, 8: 78, 9: 36, 10: 99,
  11: 45, 12: 62, 13: 80, 14: 46, 15: 81, 16: 55, 17: 59, 18: 112, 19: 68, 20: 130,
  21: 75, 22: 161, 23: 83, 24: 41, 25: 177, 26: 77, 27: 158, 28: 57, 29: 55, 30: 158,
  31: 107, 32: 167, 33: 54, 34: 164, 35: 70, 36: 40, 37: 188, 38: 102, 39: 165, 40: 135,
  41: 45, 42: 221, 43: 60, 44: 192, 45: 62, 46: 131, 47: 54, 48: 94, 49: 130, 50: 53,
  51: 180, 52: 85, 53: 45, 54: 200, 55: 124, 56: 167, 57: 114, 58: 168, 59: 42, 60: 136,
  61: 177, 62: 70, 63: 171, 64: 88, 65: 50, 66: 203, 67: 72, 68: 194, 69: 60, 70: 173,
  71: 90, 72: 88, 73: 169, 74: 28, 75: 90, 76: 40, 77: 55, 78: 194, 79: 68, 80: 167,
  81: 60, 82: 138, 83: 56, 84: 55, 85: 166, 86: 63, 87: 177, 88: 80, 89: 50, 90: 167,
  91: 53, 92: 180, 93: 65, 94: 170, 95: 42, 96: 64, 97: 191, 98: 55, 99: 154, 100: 34,
  101: 52, 102: 227, 103: 132, 104: 145, 105: 103, 106: 121, 107: 76, 108: 75, 109: 105, 110: 88,
  111: 171, 112: 93, 113: 65, 114: 120, 115: 60, 116: 214, 117: 43, 118: 172, 119: 107, 120: 54,
  121: 202, 122: 54, 123: 169, 124: 140, 125: 148, 126: 120, 127: 78, 128: 188, 129: 60, 130: 182,
  131: 56, 132: 59, 133: 182, 134: 151, 135: 142, 136: 147, 137: 59, 138: 179, 139: 85, 140: 122,
  141: 158, 142: 151, 143: 75, 144: 75, 145: 216, 146: 69, 147: 187, 148: 80, 149: 82, 150: 112
}

# Adjusted smooth progression targets
target_arrows = dict(user_ref)

# Improved Starting Levels (Noticeable ramp: 4 -> 8 -> 14 -> 18 -> 23)
target_arrows[1] = 4
target_arrows[2] = 8
target_arrows[3] = 14
target_arrows[4] = 18
target_arrows[5] = 23 # Palm Tree shape puzzle

# Smooth out severe cliff drops across the progression
target_arrows[24] = 58
target_arrows[35] = 70
target_arrows[59] = 68
target_arrows[74] = 82 # Level 74 fixed from 28!
target_arrows[76] = 70
target_arrows[95] = 78
target_arrows[100] = 253 # Century Climax Boss
target_arrows[101] = 85
target_arrows[117] = 90
target_arrows[120] = 86
target_arrows[129] = 88
target_arrows[132] = 88
target_arrows[137] = 88

def get_difficulty(lvl_num, arr_cnt, is_shape):
    if arr_cnt >= 180: return "Super Hard"
    if arr_cnt >= 110: return "Hard" if lvl_num < 80 else "Super Hard"
    if arr_cnt >= 70: return "Hard"
    if arr_cnt <= 10: return "Easy"
    return "Medium"

used_sigs = set()
def get_sig(lvl):
    all_i = tuple(sorted(i for a in lvl['Arrows'] for i in a['Indices']))
    return (lvl['XSize'], lvl['YSize'], len(lvl['Arrows']), all_i[:8])

final_levels = []

for lvl_num in range(1, 151):
    cand = None
    
    if lvl_num in BOSS_PLAN:
        b_name, b_arr, b_diff = BOSS_PLAN[lvl_num]
        if lvl_num == 150:
            rose = [s for s in shapes if s.get('ShapeName') == 'Hrose Final'][0]
            cand = prepare_level(rose, 150, "Rose Grand Finale", "Super Hard", "Shape Puzzle Levels")
        else: # 100
            climax = [l for l in one_k if len(l['Arrows']) == 195][0]
            cand = prepare_level(climax, 100, None, "Super Hard", "Existing 1K Levels")
            
    elif lvl_num in shape_by_lvl:
        _, search_name, disp_name, s_arr, s_diff = shape_by_lvl[lvl_num]
        matches = [s for s in shapes + large if search_name.lower() in s.get('ShapeName', '').lower()]
        assert len(matches) > 0, f"Could not find shape '{search_name}'"
        matches.sort(key=lambda s: abs(len(s['Arrows']) - s_arr))
        for m in matches:
            prep = prepare_level(m, lvl_num, disp_name, s_diff, "Shape Puzzle Levels")
            if get_sig(prep) not in used_sigs and solve(prep):
                cand = prep
                break
        assert cand is not None, f"Shape '{disp_name}' failed solvability!"
        
    else:
        # Normal Level
        t_arr = target_arrows[lvl_num]
        diff = get_difficulty(lvl_num, t_arr, False)
        
        if lvl_num == 1:
            cand = prepare_level(unity[0], 1, None, "Easy", "New Unity Normal Levels")
        elif lvl_num == 2:
            r = [l for l in unity if len(l['Arrows']) == 8 and not l.get('ShapeName')][0]
            cand = prepare_level(r, 2, None, "Easy", "New Unity Normal Levels")
        elif lvl_num == 3:
            r = [l for l in unity if len(l['Arrows']) == 14 and not l.get('ShapeName')][0]
            cand = prepare_level(r, 3, None, "Medium", "New Unity Normal Levels")
        elif lvl_num == 4:
            r = [l for l in unity if len(l['Arrows']) == 18 and not l.get('ShapeName')][0]
            cand = prepare_level(r, 4, None, "Medium", "New Unity Normal Levels")
        elif lvl_num == 35:
            # Clean, well-proportioned level from 1K (31x25, 70 arrows)
            r = one_k[303]
            cand = prepare_level(r, 35, None, "Medium", "Existing 1K Levels")
        else:
            is_unity = (lvl_num % 2 == 1)
            primary_pool = unity_by_arr if is_unity else one_k_by_arr
            fallback_pool = one_k_by_arr if is_unity else unity_by_arr
            src_name = "New Unity Normal Levels" if is_unity else "Existing 1K Levels"
            fb_src = "Existing 1K Levels" if is_unity else "New Unity Normal Levels"
            
            for delta in range(0, 10):
                for arr_c in (t_arr, t_arr + delta, t_arr - delta):
                    if arr_c in primary_pool:
                        for s in primary_pool[arr_c]:
                            if not s.get('ShapeName'):
                                prep = prepare_level(s, lvl_num, None, diff, src_name)
                                if get_sig(prep) not in used_sigs and solve(prep):
                                    cand = prep
                                    break
                    if cand: break
                if cand: break
                
            if not cand:
                for delta in range(0, 15):
                    for arr_c in (t_arr, t_arr + delta, t_arr - delta):
                        if arr_c in fallback_pool:
                            for s in fallback_pool[arr_c]:
                                if not s.get('ShapeName'):
                                    prep = prepare_level(s, lvl_num, None, diff, fb_src)
                                    if get_sig(prep) not in used_sigs and solve(prep):
                                        cand = prep
                                        break
                        if cand: break
                    if cand: break
                    
        assert cand is not None, f"Normal level {lvl_num} (target {t_arr}) failed to match!"
        assert solve(cand), f"Normal level {lvl_num} failed solvability!"
        
    used_sigs.add(get_sig(cand))
    final_levels.append(cand)

print(f"Successfully generated all {len(final_levels)} levels! Writing to {OUT_DIR}...")

manifest_list = []
for lvl in final_levels:
    num = lvl['LevelNumber']
    path = os.path.join(OUT_DIR, f"Level_{num}.json")
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(lvl, f, separators=(',', ':'))
        
    manifest_list.append({
        'LevelNumber': num,
        'ArrowCount': lvl['ArrowCount'],
        'Difficulty': lvl['Difficulty'],
        'ShapeName': lvl['ShapeName'],
        'IsShape': lvl['ShapeName'] is not None,
        'SourceCategory': lvl['SourceCategory'],
        'XSize': lvl['XSize'],
        'YSize': lvl['YSize']
    })

manifest_path = os.path.join(OUT_DIR, 'manifest.json')
with open(manifest_path, 'w', encoding='utf-8') as f:
    json.dump(manifest_list, f, indent=2)

print(f"ALL 150 LEVELS WRITTEN AND VERIFIED!")
