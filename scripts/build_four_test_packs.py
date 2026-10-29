import os
import sys
import json
import gzip
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
    '彩虹吉他': 'Rainbow Guitar',
    '萨克斯': 'Saxophone',
    '小提琴': 'Violin',
    '唱片机': 'Gramophone',
    '胶片唱机': 'Vinyl Record Player',
    '热气球': 'Hot Air Balloon',
    '火箭': 'Rocket',
    '雨伞': 'Umbrella',
    '雨伞修改': 'Umbrella',
    '墨镜': 'Sunglasses',
    '跑鞋': 'Sneaker',
    '自行车': 'Bicycle',
    '小火车': 'Toy Train',
    '摩托车': 'Motorcycle',
    '汽车': 'Automobile',
    '相机': 'Camera',
    '台灯': 'Desk Lamp',
    '水壶': 'Kettle',
    '茶壶': 'Teapot',
    '马克杯': 'Mug',
    '钻石': 'Diamond',
    '王冠': 'Crown',
    '沙漏': 'Hourglass',
    '灯塔': 'Lighthouse',
    '风车': 'Windmill',
    '城堡': 'Castle',
    '比萨斜塔': 'Tower of Pisa',
    '埃菲尔铁塔': 'Eiffel Tower',
    '悉尼歌剧院': 'Sydney Opera House',
    '凯旋门': 'Arc de Triomphe',
    '泰姬陵': 'Taj Mahal',
    '斯芬克斯': 'Sphinx',
    '狮身人面像': 'The Great Sphinx',
    '大本钟': 'Big Ben',
    '星空': 'The Starry Night',
    '长城': 'The Great Wall',
    '罗马斗兽场': 'Colosseum',
    '巨石阵': 'Stonehenge',
    '自由女神': 'Statue of Liberty',
    '海盗船': 'Pirate Ship',
    '藏宝箱': 'Treasure Chest',
    '指南针': 'Compass',
    '望远镜': 'Telescope',
    '椰树': 'Palm Tree',
    '小树': 'Little Tree',
    '松树': 'Pine Tree',
    '蘑菇': 'Mushroom',
    '彩虹': 'Rainbow',
    '枫叶': 'Maple Leaf',
    '四叶草': 'Four-Leaf Clover',
    '玫瑰': 'Rose',
    '太阳花': 'Sun Flower',
    '康乃馨': 'Carnation',
    '小提琴修改': 'Violin',
    '电吉他': 'Electric Guitar',
    '钢琴': 'Piano',
    '口琴': 'Harmonica',
    '留声机': 'Gramophone',
    '滑板': 'Skateboard',
    '轮滑鞋': 'Roller Skate',
    '降落伞': 'Parachute',
    '帆船': 'Sailboat',
    '游艇': 'Yacht',
    '潜水艇': 'Submarine',
    '飞机': 'Airplane',
    '直升机': 'Helicopter',
    '飞碟': 'UFO',
    '机器人': 'Robot',
    '太空人': 'Astronaut',
    '卫星': 'Satellite',
    '地球仪': 'Globe',
    '天平': 'Balance Scale',
    '显微镜': 'Microscope',
    '试管': 'Test Tube',
    '温度计': 'Thermometer',
    '放大镜': 'Magnifying Glass',
    '扳手': 'Wrench',
    '锤子': 'Hammer',
    '螺丝刀': 'Screwdriver',
    '锯子': 'Hand Saw',
    '剪刀': 'Scissors',
    '闹钟': 'Alarm Clock',
    '怀表': 'Pocket Watch',
    '沙滩伞': 'Beach Umbrella',
    '救生圈': 'Lifebuoy',
    '锚': 'Anchor',
    '舵': "Ship's Wheel",
    '宝剑': 'Sword',
    '盾牌': 'Shield',
    '弓箭': 'Bow and Arrow',
    '头盔': 'Helmet',
    '皇冠': 'Royal Crown',
    '权杖': 'Scepter',
    '奖杯': 'Trophy',
    '勋章': 'Medal',
    '书本': 'Book',
    '羽毛笔': 'Quill Pen',
    '墨水瓶': 'Ink Bottle',
    '画架': 'Easel',
    '调色盘': 'Palette',
    '吉他': 'Guitar',
    '风铃': 'Wind Chime',
    '捕梦网': 'Dreamcatcher',
    '折扇': 'Folding Fan',
    '灯笼': 'Lantern',
    '中国结': 'Chinese Knot',
    '风筝': 'Kite',
    '毽子': 'Shuttlecock',
    '不倒翁': 'Tumbler Toy',
    '木马': 'Rocking Horse',
    '雪人': 'Snowman',
    '圣诞树': 'Christmas Tree',
    '姜饼人': 'Gingerbread Man',
    '糖果': 'Candy',
    '棒棒糖': 'Lollipop',
    '巧克力': 'Chocolate',
    '爆米花': 'Popcorn',
    '薯条': 'French Fries',
    '热狗': 'Hot Dog',
    '三明治': 'Sandwich',
    '可乐': 'Cola',
    '咖啡': 'Coffee',
    '茶杯': 'Teacup',
    '酒杯': 'Wine Glass',
    '高脚杯': 'Goblet',
    '奶瓶': 'Baby Bottle',
    '奶嘴': 'Pacifier',
    '气球': 'Balloon',
    '风车玩具': 'Pinwheel',
    '魔方': "Rubik's Cube",
    '扑克牌': 'Playing Card',
    '骰子': 'Dice',
    '麻将': 'Mahjong Tile',
    '棋盘': 'Chess Board',
    '足球': 'Soccer Ball',
    '篮球': 'Basketball',
    '排球': 'Volleyball',
    '网球': 'Tennis Ball',
    '棒球': 'Baseball',
    '保龄球': 'Bowling Pin',
    '高尔夫': 'Golf Club',
    '哑铃': 'Dumbbell',
    '瑜伽': 'Yoga Pose',
    '靶心': 'Bullseye',
    '奖牌': 'Medal Ribbon',
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

def clean_shape_name(raw_name):
    lower = raw_name.lower()
    for k, v in ZH_EN_MAP.items():
        if k in lower:
            return v
    # If it is a hex hash or internal id, it is not a named shape
    if re.match(r'^[0-9a-fA-F]{11,16}$', raw_name) or re.match(r'^(hole|pipes|reason|rule|sample|test)[-_0-9a-zA-Z]+$', raw_name):
        return ""
    clean = re.sub(r'[-_][0-9a-fA-F]{16}$', '', raw_name)
    clean = re.sub(r'[-_][0-9a-fA-F]{8,}$', '', clean)
    clean = re.sub(r'[-_]v[0-9]+$', '', clean, flags=re.IGNORECASE)
    for zh, en in ZH_EN_MAP.items():
        if zh in clean:
            clean = clean.replace(zh, en)
    clean = re.sub(r'([a-z])([A-Z])', r'\1 \2', clean)
    clean = clean.replace('_', ' ').replace('-', ' ')
    clean = ' '.join(w.capitalize() for w in clean.split())
    clean = re.sub(r'\s+', ' ', clean).strip()
    return clean if clean else ""

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
            bn = os.path.basename(p)
            num = int(bn.replace('Level_', '').replace('.json', ''))
            d['Source'] = bn
            d['Hash'] = 'OLD_' + bn.replace('.json', '')
            d['ShapeName'] = f"Shape #{num}" if num % 5 == 0 else ""
            existing_1k_raw.append(d)
    except:
        pass

# Category 1: Existing 1K levels (Keep ALL 1,001 levels)
seen_1k_signatures = set()
existing_1k_solvable = []
for lvl in existing_1k_raw:
    c = crop_level(lvl)
    sig = get_signature(c)
    if sig not in seen_1k_signatures:
        seen_1k_signatures.add(sig)
        if solve_level(c):
            existing_1k_solvable.append(c)

print(f"Category 1 (Existing 1K): {len(existing_1k_solvable)} levels")
existing_1k_solvable.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))

print("==========================================================")
print("  STEP 2: INGESTING LARGE PUZZLES (250–450 ARROWS)        ")
print("==========================================================")
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
                    shape_name = clean_shape_name(name)
                    lvl_obj = {
                        'XSize': xs,
                        'YSize': ys,
                        'Arrows': arrows_out,
                        'Hash': name,
                        'ShapeName': shape_name,
                        'MaxHearts': raw.get('MaxHearts', 3),
                        'Source': 'UnityLarge'
                    }
                    raw_large.append(lvl_obj)
        except:
            pass

seen_large_signatures = set()
large_puzzles_solvable = []
for lvl in raw_large:
    c = crop_level(lvl)
    sig = get_signature(c)
    if sig not in seen_1k_signatures and sig not in seen_large_signatures:
        seen_large_signatures.add(sig)
        if solve_level(c):
            large_puzzles_solvable.append(c)

print(f"Category 4 (Large Puzzles): {len(large_puzzles_solvable)} levels (250–437 arrows)")
large_puzzles_solvable.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))

print("==========================================================")
print("  STEP 3: INGESTING SHAPE PUZZLES (< 250 ARROWS, NO 1K)   ")
print("==========================================================")
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

seen_shape_signatures = set()
curated_shapes = []
for s in unity_shape_candidates + new_levels_shapes:
    cropped = crop_level(s)
    sig = get_signature(cropped)
    # MUST NOT BE IN EXISTING 1K!
    if sig in seen_1k_signatures:
        continue
    # MUST NOT BE IN LARGE PUZZLES (those >= 250 are cleanly in Large Puzzles!)
    if sig in seen_large_signatures or len(cropped['Arrows']) >= 250:
        continue
    if sig not in seen_shape_signatures:
        seen_shape_signatures.add(sig)
        if solve_level(cropped):
            curated_shapes.append(cropped)

print(f"Category 3 (Shape Puzzles): {len(curated_shapes)} recognizable shapes (< 250 arrows)")
curated_shapes.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))

print("==========================================================")
print("  STEP 4: INGESTING UNITY NORMAL (ZERO DUPLICATES)        ")
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
                    'ShapeName': clean_shape_name(name),
                    'Source': 'Unity_' + name
                }
        except:
            pass

seen_normal_signatures = set()
unity_normal_solvable = []

for h in normal_table_hashes:
    if h in unity_assets:
        item = unity_assets[h]
        cropped = crop_level(item)
        sig = get_signature(cropped)
        # MUST NOT be in Existing 1K, MUST NOT be in Shapes, MUST NOT be in Large!
        if sig in seen_1k_signatures or sig in seen_shape_signatures or sig in seen_large_signatures:
            continue
        if sig not in seen_normal_signatures:
            seen_normal_signatures.add(sig)
            if solve_level(cropped):
                unity_normal_solvable.append(cropped)

print(f"Category 2 (Unity Normal): {len(unity_normal_solvable)} levels")
unity_normal_solvable.sort(key=lambda x: (len(x['Arrows']), x['XSize'] * x['YSize']))

print("==========================================================")
print("  STEP 5: WRITING ALL 4 ENCRYPTED PACKS & MANIFESTS       ")
print("==========================================================")
m1 = write_category_packs("test_existing_1k", existing_1k_solvable, chunk_size=1000)
with open('manifest_existing_1k.json', 'w', encoding='utf-8') as f:
    json.dump(m1, f, indent=2)

m2 = write_category_packs("test_unity_normal", unity_normal_solvable, chunk_size=1000)
with open('manifest_unity_normal.json', 'w', encoding='utf-8') as f:
    json.dump(m2, f, indent=2)

m3 = write_category_packs("test_shape_puzzles", curated_shapes, chunk_size=1000)
with open('manifest_shape_puzzles.json', 'w', encoding='utf-8') as f:
    json.dump(m3, f, indent=2)

m4 = write_category_packs("test_large_puzzles", large_puzzles_solvable, chunk_size=1000)
with open('manifest_large_puzzles.json', 'w', encoding='utf-8') as f:
    json.dump(m4, f, indent=2)

print("==========================================================")
print("  STEP 6: ZERO CROSS-CATEGORY OVERLAP VERIFICATION        ")
print("==========================================================")
sigs1 = set(get_signature(x) for x in existing_1k_solvable)
sigs2 = set(get_signature(x) for x in unity_normal_solvable)
sigs3 = set(get_signature(x) for x in curated_shapes)
sigs4 = set(get_signature(x) for x in large_puzzles_solvable)

print(f"Overlap 1 (Existing 1K) & 2 (Unity Normal):  {len(sigs1 & sigs2)}")
print(f"Overlap 1 (Existing 1K) & 3 (Shape Puzzles): {len(sigs1 & sigs3)}")
print(f"Overlap 1 (Existing 1K) & 4 (Large Puzzles): {len(sigs1 & sigs4)}")
print(f"Overlap 2 (Unity Normal) & 3 (Shape Puzzles):{len(sigs2 & sigs3)}")
print(f"Overlap 2 (Unity Normal) & 4 (Large Puzzles):{len(sigs2 & sigs4)}")
print(f"Overlap 3 (Shape Puzzles) & 4 (Large Puzzles):{len(sigs3 & sigs4)}")

assert len(sigs1 & sigs2) == 0, "Duplicate between 1 and 2!"
assert len(sigs1 & sigs3) == 0, "Duplicate between 1 and 3!"
assert len(sigs1 & sigs4) == 0, "Duplicate between 1 and 4!"
assert len(sigs2 & sigs3) == 0, "Duplicate between 2 and 3!"
assert len(sigs2 & sigs4) == 0, "Duplicate between 2 and 4!"
assert len(sigs3 & sigs4) == 0, "Duplicate between 3 and 4!"

print("\nSUCCESS! ZERO DUPLICATES ACROSS ALL 4 CATEGORIES!")
print(f"  Category 1 (Existing 1K):    {len(m1)} levels ({m1[0]['ArrowCount']} to {m1[-1]['ArrowCount']} arr)")
shape_in_1k = sum(1 for x in m1 if x.get('ShapeName'))
print(f"  Category 1 shapes with names: {shape_in_1k} of {len(m1)} levels")
print(f"  Category 2 (Unity Normal):   {len(m2)} levels ({m2[0]['ArrowCount']} to {m2[-1]['ArrowCount']} arr)")
shape_in_normal = sum(1 for x in m2 if x.get('ShapeName'))
print(f"  Category 2 shapes with names: {shape_in_normal} of {len(m2)} levels")
print(f"  Category 3 (Shape Puzzles):  {len(m3)} shapes ({m3[0]['ArrowCount']} to {m3[-1]['ArrowCount']} arr)")
shape_in_shapes = sum(1 for x in m3 if x.get('ShapeName'))
print(f"  Category 3 shapes with names: {shape_in_shapes} of {len(m3)} shapes")
print(f"  Category 4 (Large Puzzles):  {len(m4)} mega grids ({m4[0]['ArrowCount']} to {m4[-1]['ArrowCount']} arr)")
shape_in_large = sum(1 for x in m4 if x.get('ShapeName'))
print(f"  Category 4 shapes with names: {shape_in_large} of {len(m4)} levels")
print(f"  TOTAL NAMED SHAPES ACROSS ALL CATEGORIES: {shape_in_1k + shape_in_normal + shape_in_shapes + shape_in_large}")
