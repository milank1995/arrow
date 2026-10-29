import os
import sys
import json
import gzip
import time
import glob
import re
from collections import defaultdict
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend
import UnityPy

sys.stdout.reconfigure(encoding='utf-8')
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
AES_KEY = b'AmazeGo_Arrow_Security_Key_2026!'

BASE_OUT = r'arrow_native/app/src/main/assets/levels'

ZH_EN_MAP = {
    '泰姬陵': 'Taj Mahal',
    '比萨斜塔': 'Tower of Pisa',
    '狮身人面像': 'The Great Sphinx',
    '埃菲尔铁塔': 'Eiffel Tower',
    '凯旋门': 'Arc de Triomphe',
    '悉尼歌剧院': 'Sydney Opera House',
    '天坛': 'Temple of Heaven',
    '城堡': 'Castle',
    '长城': 'Great Wall',
    '自由女神': 'Statue of Liberty',
    '金字塔': 'Pyramid',
    '侦探': 'Detective',
    '奔马': 'Galloping Horse',
    '小猫大学生': 'College Cat',
    '叶间猫': 'Leaf Cat',
    '望星星的猫': 'Stargazer Cat',
    '望月猫': 'Moonlit Cat',
    '电波小猫': 'Electric Cat',
    '盒子里的猫': 'Box Cat',
    '盘腿猫': 'Sitting Cat',
    '睡觉猫咪': 'Sleeping Cat',
    '招财猫': 'Lucky Cat',
    '青苹果猫猫': 'Green Apple Cat',
    '太极猫咪': 'Taiji Cat',
    '寿司猫': 'Sushi Cat',
    '雨伞猫改': 'Umbrella Cat',
    '黑猫': 'Black Cat',
    '小象改': 'Little Elephant',
    '热带瀑布大象': 'Tropical Waterfall Elephant',
    '变色龙修改': 'Chameleon',
    '考拉': 'Koala',
    '熊猫': 'Panda',
    '狐狸': 'Fox',
    '独角兽': 'Unicorn',
    '狮子': 'Lion',
    '黑豹': 'Black Panther',
    '海豚': 'Dolphin',
    '海马': 'Seahorse',
    '海螺': 'Conch',
    '海豹顶球': 'Seal Balancing Ball',
    '水母': 'Jellyfish',
    '鲸鱼': 'Whale',
    '金鱼改': 'Goldfish',
    '鲤鱼': 'Koi Fish',
    '龙虾xg': 'Lobster',
    '螃蟹': 'Crab',
    '巨蟹': 'Cancer (Crab)',
    '双鱼': 'Pisces (Fish)',
    '白羊': 'Aries (Ram)',
    '金牛': 'Taurus (Bull)',
    '双子座': 'Gemini (Twins)',
    '狮子座': 'Leo (Lion)',
    '处女座': 'Virgo (Maiden)',
    '天秤': 'Libra (Scales)',
    '天蝎座': 'Scorpio (Scorpion)',
    '射手座': 'Sagittarius (Archer)',
    '摩羯座': 'Capricorn (Sea-Goat)',
    '水瓶改': 'Aquarius (Water-Bearer)',
    '天鹅': 'Swan',
    '白鸽': 'White Dove',
    '蓝鸟': 'Bluebird',
    '鹦鹉': 'Parrot',
    '巨嘴鸟': 'Toucan',
    '小鸭洗澡': 'Ducking Bath',
    '红掌拨清波': 'Goose Swimming',
    '蝴蝶': 'Butterfly',
    '手托蝴蝶': 'Hand with Butterfly',
    '蝶翼': 'Butterfly Wings',
    '蜜蜂': 'Honeybee',
    '螳螂': 'Mantis',
    '蜗牛': 'Snail',
    '向日葵': 'Sunflower',
    '百合花': 'Lily',
    '郁金香': 'Tulip',
    '花瓶': 'Flower Vase',
    '仙人掌': 'Cactus',
    '苹果': 'Apple',
    '香蕉': 'Banana',
    '梨子': 'Pear',
    '桃子': 'Peach',
    '西瓜': 'Watermelon',
    '草莓': 'Strawberry',
    '柠檬': 'Lemon',
    '牛油果': 'Avocado',
    '汉堡': 'Burger',
    '披萨': 'Pizza',
    '寿司': 'Sushi',
    '蛋糕': 'Cake',
    '甜甜圈': 'Donut',
    '冰淇凌': 'Ice Cream',
    '鸡尾酒': 'Cocktail',
    '咖啡': 'Coffee',
    '咖啡豆': 'Coffee Beans',
    '水壶': 'Kettle',
    '唱片机': 'Gramophone',
    '唱片': 'Vinyl Record',
    '吉他': 'Guitar',
    '钢琴': 'Piano',
    '彩虹钢琴': 'Rainbow Piano',
    '帆船': 'Sailboat',
    '海盗船': 'Pirate Ship',
    '热气球': 'Hot Air Balloon',
    '月亮船': 'Moon Boat',
    '月窗': 'Moon Window',
    '日出山脉': 'Sunrise Mountains',
    '雪山': 'Snow Mountain',
    '雪花': 'Snowflake',
    '礼物': 'Gift Box',
    '钻石': 'Diamond',
    '红唇': 'Red Lips',
    '比心': 'Finger Heart',
    '羽翼': 'Angel Wings',
    '宝藏': 'Treasure',
    '风扇小兔': 'Fan Bunny',
    '飞马': 'Pegasus',
    '机器人': 'Robot',
    '游戏机': 'Game Console',
    '魔法青蛙': 'Magic Frog',
    '绿洲骆驼': 'Oasis Camel',
}

def clean_shape_name(raw_name):
    for zh, en in ZH_EN_MAP.items():
        if zh in raw_name:
            return en
    s = re.sub(r'[-_][0-9a-fA-F]{11,16}$', '', raw_name)
    s = re.sub(r'[-_][0-9a-fA-F]{8,}$', '', s)
    s = re.sub(r'_v\d+.*$', '', s)
    s = re.sub(r'v\d+.*$', '', s)
    s = re.sub(r'#\d+.*$', '', s)
    s = re.sub(r'pro$', '', s)
    s = re.sub(r'xg$', '', s)
    s = s.replace('_', ' ').replace('-', ' ').strip()
    s = re.sub(r'([a-z])([A-Z])', r'\1 \2', s)
    return s.title()

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
        if not idxs: continue
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

def get_signature(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    items = []
    for a in lvl['Arrows']:
        idxs = a['Indices']
        items.append((idxs[0], idxs[-1], a['Dx'], a['Dy'], a.get('BendCount', 0)))
    return (xs, ys, tuple(sorted(items)))

def write_category_packs(subfolder, level_list, chunk_size=1000):
    target_dir = os.path.join(BASE_OUT, subfolder)
    os.makedirs(target_dir, exist_ok=True)
    
    # Remove existing dat files in folder
    for f in glob.glob(os.path.join(target_dir, "*.dat")):
        os.remove(f)
        
    chunks = [level_list[i:i + chunk_size] for i in range(0, len(level_list), chunk_size)]
    pack_manifest = {
        'totalLevels': len(level_list),
        'chunkSize': chunk_size,
        'packsCount': len(chunks),
        'packs': []
    }
    
    for pack_idx, chunk in enumerate(chunks):
        pack_data = []
        for local_idx, lvl in enumerate(chunk):
            pack_data.append({
                'XSize': lvl['XSize'],
                'YSize': lvl['YSize'],
                'MaxHearts': lvl.get('MaxHearts', 3),
                'Hash': lvl.get('Hash', ''),
                'ShapeName': lvl.get('ShapeName', ''),
                'Arrows': lvl['Arrows']
            })
            
        encrypted_bytes = encrypt_pack(pack_data)
        file_name = f"level_pack_{pack_idx:02d}.dat"
        file_path = os.path.join(target_dir, file_name)
        with open(file_path, 'wb') as f:
            f.write(encrypted_bytes)
            
        pack_manifest['packs'].append({
            'packIndex': pack_idx,
            'fileName': file_name,
            'levelsCount': len(chunk),
            'startLevel': pack_idx * chunk_size + 1,
            'endLevel': pack_idx * chunk_size + len(chunk),
            'fileSizeBytes': len(encrypted_bytes)
        })
        print(f"[{subfolder}] Wrote {file_name}: {len(chunk)} levels ({len(encrypted_bytes)/1024:.1f} KB)")
        
    with open(os.path.join(target_dir, 'pack_manifest.json'), 'w', encoding='utf-8') as f:
        json.dump(pack_manifest, f, indent=2)
        
    manifest_summary = []
    for idx, lvl in enumerate(level_list):
        manifest_summary.append({
            'Level': idx + 1,
            'ArrowCount': len(lvl['Arrows']),
            'XSize': lvl['XSize'],
            'YSize': lvl['YSize'],
            'Hash': lvl.get('Hash', ''),
            'ShapeName': lvl.get('ShapeName', ''),
            'Source': lvl.get('Source', '')
        })
    return manifest_summary

print("==========================================================")
print("  STEP 1: INGESTING EXISTING 1K LEVELS                    ")
print("==========================================================")
old_files = glob.glob('scratch/old_levels/arrow_native/app/src/main/assets/levels/Level_*.json')
existing_1k_raw = []
for p in old_files:
    try:
        with open(p, 'r', encoding='utf-8') as f:
            d = json.load(f)
            d['Source'] = os.path.basename(p)
            d['Hash'] = 'OLD_' + os.path.basename(p).replace('.json', '')
            existing_1k_raw.append(d)
    except:
        pass

print(f"Ingested {len(existing_1k_raw)} existing 1K files.")

seen_1k = set()
existing_1k_solvable = []
for lvl in existing_1k_raw:
    c = crop_level(lvl)
    sig = get_signature(c)
    if sig not in seen_1k:
        seen_1k.add(sig)
        if solve_level(c):
            existing_1k_solvable.append(c)

print(f"100% solvable unique existing 1K levels: {len(existing_1k_solvable)}")
existing_1k_solvable.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))
print(f"Sorted 1K arrow range: {len(existing_1k_solvable[0]['Arrows'])} to {len(existing_1k_solvable[-1]['Arrows'])}")

print("==========================================================")
print("  STEP 2: INGESTING RECOGNIZABLE SHAPE PUZZLES            ")
print("==========================================================")
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'
with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

unity_shape_candidates = []
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        if re.match(r'^[0-9a-fA-F]{11,16}$', name): continue
        if re.match(r'^(reason|rule|hole|pipes|level|test|sample)[-_0-9a-zA-Z]+$', name): continue
        if re.match(r'^[0-9]{4,}[-_]', name): continue
        if re.match(r'^(H|N|SH)-[0-9a-fA-F]+$', name): continue
        if re.match(r'^(QuadraPass|PipelineGuide)', name): continue
        
        try:
            b = d.m_Script.encode('utf-8', 'surrogateescape')
            raw = json.loads(gzip.decompress(b))
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
            if arrows_out:
                unity_shape_candidates.append({
                    'XSize': xs,
                    'YSize': ys,
                    'Arrows': arrows_out,
                    'Hash': name,
                    'ShapeName': clean_shape_name(name),
                    'Source': 'Unity'
                })
        except:
            pass

print(f"Ingested {len(unity_shape_candidates)} named Unity shape candidates.")

# Existing 1K shape levels (every 5th level)
existing_1k_shapes = []
for p in old_files:
    bn = os.path.basename(p)
    num = int(bn.replace('Level_', '').replace('.json', ''))
    if num % 5 == 0:
        try:
            with open(p, 'r', encoding='utf-8') as f:
                d = json.load(f)
                d['Hash'] = f"Level_{num}"
                d['ShapeName'] = f"Shape #{num}"
                d['Source'] = 'Existing1K'
                existing_1k_shapes.append(d)
        except:
            pass

print(f"Ingested {len(existing_1k_shapes)} Existing 1K shape levels.")

# NewLevels shape levels (every 5th level + Level 26 Cactus)
new_levels_shapes = []
new_files = glob.glob('scratch/old_levels/arrow_native/app/src/main/assets/new_levels/NewLevel_*.json')
for p in new_files:
    bn = os.path.basename(p)
    num = int(bn.replace('NewLevel_', '').replace('.json', ''))
    if num % 5 == 0 or num == 26:
        try:
            with open(p, 'r', encoding='utf-8') as f:
                d = json.load(f)
                d['Hash'] = f"NewLevel_{num}"
                d['ShapeName'] = 'Cactus' if num == 26 else f'Shape #{num}'
                d['Source'] = 'NewLevels'
                new_levels_shapes.append(d)
        except:
            pass

print(f"Ingested {len(new_levels_shapes)} NewLevels shape levels.")

# Deduplicate all true shapes
seen_shape_signatures = set()
curated_shapes = []
for s in unity_shape_candidates + existing_1k_shapes + new_levels_shapes:
    cropped = crop_level(s)
    sig = get_signature(cropped)
    if sig not in seen_shape_signatures:
        seen_shape_signatures.add(sig)
        if solve_level(cropped):
            curated_shapes.append(cropped)

print(f"Total curated, 100% solvable, unique recognizable shape levels: {len(curated_shapes)}")
curated_shapes.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))
print(f"Shape arrow range: {len(curated_shapes[0]['Arrows'])} to {len(curated_shapes[-1]['Arrows'])}")

print("==========================================================")
print("  STEP 3: INGESTING NEW UNITY NORMAL LEVELS               ")
print("==========================================================")
config_bundle_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
with open(config_bundle_path, 'rb') as f:
    f.seek(32)
    env_cfg = UnityPy.load(f.read())

unity_tables = defaultdict(list)
for obj in env_cfg.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
        if hashes:
            unity_tables[name] = hashes

normal_table_hashes = set()
for tname, hashes in unity_tables.items():
    if tname.startswith('NormalLevels') or tname.startswith('GuideLevels'):
        normal_table_hashes.update(hashes)

print(f"Identified {len(normal_table_hashes)} unique hashes from NormalLevels and GuideLevels tables.")

# Load all unity assets from bundle
unity_assets = {}
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        try:
            raw = json.loads(gzip.decompress(b))
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
            if arrows_out:
                unity_assets[name] = {
                    'XSize': xs,
                    'YSize': ys,
                    'Arrows': arrows_out,
                    'Hash': name,
                    'Source': 'Unity_' + name
                }
        except:
            pass

print(f"Loaded {len(unity_assets)} playable levels from Unity packs bundle.")

# Filter normal levels: in normal_table_hashes, NOT in seen_shape_signatures
seen_normal_signatures = set()
unity_normal_solvable = []

for h in normal_table_hashes:
    if h in unity_assets:
        item = unity_assets[h]
        cropped = crop_level(item)
        sig = get_signature(cropped)
        # MUST NOT be in shapes
        if sig in seen_shape_signatures:
            continue
        if sig not in seen_normal_signatures:
            seen_normal_signatures.add(sig)
            if solve_level(cropped):
                unity_normal_solvable.append(cropped)

print(f"Unique, 100% solvable Unity Normal levels: {len(unity_normal_solvable)}")
unity_normal_solvable.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))
print(f"Sorted Unity Normal arrow range: {len(unity_normal_solvable[0]['Arrows'])} to {len(unity_normal_solvable[-1]['Arrows'])}")

print("==========================================================")
print("  STEP 4: WRITING ENCRYPTED PACKS & MANIFESTS             ")
print("==========================================================")
# Category 1: Existing 1K
m1 = write_category_packs("test_existing_1k", existing_1k_solvable, chunk_size=1000)
with open('manifest_existing_1k.json', 'w', encoding='utf-8') as f:
    json.dump(m1, f, indent=2)

# Category 2: Unity Normal
m2 = write_category_packs("test_unity_normal", unity_normal_solvable, chunk_size=1000)
with open('manifest_unity_normal.json', 'w', encoding='utf-8') as f:
    json.dump(m2, f, indent=2)

# Category 3: Shape Puzzles
m3 = write_category_packs("test_shape_puzzles", curated_shapes, chunk_size=1000)
with open('manifest_shape_puzzles.json', 'w', encoding='utf-8') as f:
    json.dump(m3, f, indent=2)

print("==========================================================")
print(f"  ALL 3 CATEGORIES SUCCESSFULLY BUILT & ENCRYPTED!        ")
print(f"  Category 1 (Existing 1K):    {len(m1)} levels ({m1[0]['ArrowCount']} to {m1[-1]['ArrowCount']} arr)")
print(f"  Category 2 (Unity Normal):   {len(m2)} levels ({m2[0]['ArrowCount']} to {m2[-1]['ArrowCount']} arr)")
print(f"  Category 3 (Shape Puzzles):  {len(m3)} shapes ({m3[0]['ArrowCount']} to {m3[-1]['ArrowCount']} arr)")
print("==========================================================")
