import sys
import UnityPy
import re
import json
import gzip
import os
import glob

sys.stdout.reconfigure(encoding='utf-8')
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

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
    # Check chinese map first
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

def solve_level(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    arrows = lvl['Arrows']
    if not arrows: return False
    active = list(range(len(arrows)))
    occupied = {}
    heads, dirs, cells_list = [], [], []
    for i, a in enumerate(arrows):
        idxs = a['Indices']
        hx, hy = idxs[0] % xs, (ys - 1) - (idxs[0] // xs)
        heads.append((hx, hy))
        dirs.append((a['Dx'], -a['Dy']))
        cset = set((idx % xs, (ys - 1) - (idx // xs)) for idx in idxs)
        for c in cset: occupied[c] = i
        cells_list.append(cset)
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

def get_signature(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    items = []
    for a in lvl['Arrows']:
        idxs = a['Indices']
        items.append((idxs[0], idxs[-1], a['Dx'], a['Dy'], a.get('BendCount', 0)))
    return (xs, ys, tuple(sorted(items)))

# 1. Ingest all Named Unity Shapes
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'
with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

unity_candidates = []
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
                unity_candidates.append({
                    'XSize': xs,
                    'YSize': ys,
                    'Arrows': arrows_out,
                    'Hash': name,
                    'ShapeName': clean_shape_name(name),
                    'Source': 'Unity'
                })
        except:
            pass

print(f"Ingested {len(unity_candidates)} named Unity shape candidates.")

# 2. Ingest Existing 1K Shape Levels (every 5th level: 5, 10, 15... 1000)
existing_1k_shapes = []
old_files = glob.glob('scratch/old_levels/arrow_native/app/src/main/assets/levels/Level_*.json')
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

print(f"Ingested {len(existing_1k_shapes)} Existing 1K shape levels (every 5th level).")

# 3. Deduplicate & Validate Solvability
seen_signatures = set()
curated_shapes = []

all_candidates = unity_candidates + existing_1k_shapes
for c in all_candidates:
    cropped = crop_level(c)
    sig = get_signature(cropped)
    if sig not in seen_signatures:
        seen_signatures.add(sig)
        if solve_level(cropped):
            cropped['ArrowCount'] = len(cropped['Arrows'])
            curated_shapes.append(cropped)

print(f"Total curated, 100% solvable, unique shape levels: {len(curated_shapes)}")
curated_shapes.sort(key=lambda x: (x['ArrowCount'], x['XSize'] * x['YSize']))
print(f"Arrow range: {curated_shapes[0]['ArrowCount']} to {curated_shapes[-1]['ArrowCount']}")

print("\nSample Smallest Shapes:")
for s in curated_shapes[:10]:
    print(f"  {s['ArrowCount']:3d} arr | {s['XSize']:2d}x{s['YSize']:2d} | {s['ShapeName']} ({s['Source']})")

print("\nSample Middle Shapes:")
mid = len(curated_shapes) // 2
for s in curated_shapes[mid-5:mid+5]:
    print(f"  {s['ArrowCount']:3d} arr | {s['XSize']:2d}x{s['YSize']:2d} | {s['ShapeName']} ({s['Source']})")

print("\nSample Largest Shapes:")
for s in curated_shapes[-10:]:
    print(f"  {s['ArrowCount']:3d} arr | {s['XSize']:2d}x{s['YSize']:2d} | {s['ShapeName']} ({s['Source']})")
