import UnityPy
import re
import json
import gzip
import os
import glob

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

# Dictionary mapping chinese names to English
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
    # Strip hex suffix like -830742e51e0b1e85 or _6fb864c9ab9e5270
    s = re.sub(r'[-_][0-9a-fA-F]{11,16}$', '', raw_name)
    s = re.sub(r'[-_][0-9a-fA-F]{8,}$', '', s)
    s = re.sub(r'_v\d+.*$', '', s)
    s = re.sub(r'v\d+.*$', '', s)
    s = re.sub(r'#\d+.*$', '', s)
    s = re.sub(r'pro$', '', s)
    s = re.sub(r'xg$', '', s)
    
    # Check chinese map
    for zh, en in ZH_EN_MAP.items():
        if zh in s:
            return en
            
    # Clean up camel/snake/case
    s = s.replace('_', ' ').replace('-', ' ').strip()
    # Add space before capital letters if CamelCase
    s = re.sub(r'([a-z])([A-Z])', r'\1 \2', s)
    return s.title()

print("Testing clean_shape_name:")
for t in ['TajMahal-9e0eeabf5067f313', 'Sphinx-10e52caf0ab7ebbd', 'CATclimber-9b1027ea9aab56cc', '泰姬陵d-12c477406c226fdd', '奔马-de1c4c4b3fb406e2', 'lazyfatcat-d3279c0f91b2bd8e']:
    print(f"  {t} -> {clean_shape_name(t)}")
