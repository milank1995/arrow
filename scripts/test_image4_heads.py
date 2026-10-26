import UnityPy
import gzip
import json

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

# Check candidates around 20x28 or 18x25
test_hashes = [
    'ad770e31871f00e2', '4accab310a86088a', 'a45b18fe303ae698', 'e8a444f367ba7597',
    '7e2a66985cbb50c3', '0a7b5a414f724963', 'caacb012f43dcdd8', 'bf9d5fb59f70a45d',
    '1b5dc170b0d58e5f', '5e3485f5fe426cfb', '350a8345b332aeb5', '2339578df087fc67',
    '0bce5444d9387471', '07942025abe24b70', '6013c5e65480b66f', 'b72f32b3a690d0b8',
    '1587ea6ce48c7fc0', '443636648378dc16', '9dfd96b59b280885', 'b75cc559a0c697d2',
    'e5d62397c97fe8ff', 'a06fb7b2078c812c', 'cf1e1a30efc6b6c0', '3fd4f957e26c22c5',
    '07e7b072a3e7fa37', 'aaf6758db6dd5c7b', '9c18803717980709', '3b6d4afe1a8f7485'
]

levels = {}
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        data = obj.read()
        name = getattr(data, 'm_Name', '')
        if name in test_hashes:
            raw = data.m_Script.encode('utf-8', 'surrogateescape')
            levels[name] = json.loads(gzip.decompress(raw))

print(f"Loaded {len(levels)} candidate levels.")

# In Image 4:
# Row 0:
# Left column 0: arrow pointing UP (head at (0,0))
# Let's see arrow heads in row 0 and row 1:
for h, lvl in levels.items():
    xs = lvl['XSize']
    ys = lvl['YSize']
    heads_row0 = []
    for a in lvl['Arrows']:
        pts = a['Points']
        head = pts[0]
        hx, hy = head % xs, head // xs
        if hy == 0:
            heads_row0.append((hx, pts[1] % xs - hx, pts[1] // xs - hy)) # (x, dx, dy)
    # In Image 4:
    # head at (0,0) with dy=1 (pointing UP)
    # head around middle pointing RIGHT (dx=-1)
    # head at far right (xs-1, 0) with dy=1 (pointing UP)
    # Let's print row 0 heads:
    print(f"Hash {h} ({xs}x{ys}, {len(lvl['Arrows'])} arrs): Row 0 heads: {heads_row0}")
