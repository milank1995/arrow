import UnityPy
import gzip
import json

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

# Read the 125 candidates
candidates_hashes = [
  '025db7c5ec391792', '518d084900d09372', 'bc716b073af30dfa', 'e9a7860c30594457',
  '4a55668cc89a4054', 'bd6a06b8c911578f', '04e3daf7c0042fd3', 'ead06eb834350ac3',
  'c481d986cf6ad1c5', 'fb091e0fdfcae27e', '5011d2ca52c832bb', '7e2a66985cbb50c3',
  '5b3b634726c86501', '07942025abe24b70', '0a7b5a414f724963', '56061146658d8ea9',
  '6013c5e65480b66f', '5e252ceca42ac597', '651072a20e4819c6', '30c909823a8b85f5',
  '1be64a18d5c4361b', 'b020d511f47dc101', 'b72f32b3a690d0b8', '10613528854cfa2f',
  '1587ea6ce48c7fc0', 'bf0a073a27b472e8', '6af1385e93af2cd7', '7e93ea947c5d2d40',
  '3777577797ceec8b', '7ad6115199378e1c', 'a7851b5dd7476c4b', 'be2bf883559521ac',
  'cfa9083fc829319f', '443636648378dc16', '8e4b654f4a9f637c', 'aa7a5dc42002e345',
  '3f4c393d1ce1a12a', '9dfd96b59b280885', 'b75cc559a0c697d2', '25f16eb55888b386',
  'caacb012f43dcdd8', 'bf9d5fb59f70a45d', '9e09cebbdf85e230', '87ea57678b242f5c',
  'e5d62397c97fe8ff', '0bf7480caec1411d', 'dd760de6a0fc45b9', '7fdfc12d9e848cf6',
  'a45b18fe303ae698', '81c0854be722b4b7', 'e2652e6823fb43df', 'e8a444f367ba7597',
  '89491ab2747ff9c7', 'f42d7345cb2acacb', 'cf2cfb7d950362a4', '78afc5c774a705f7',
  '92725f80d56b8dde', 'ee232c3e2d511df8', '4c4497c57905229b', 'a06fb7b2078c812c',
  'f1dff855719ebee5', '07def0a63ebed402', '1787140efc7e92cf', 'fc72fe3f72ab348c',
  '6eda07b1074330d8', '69f732b5b0a72749', '944960d30a2afaf9', '6993d218e5409061',
  '37f2f4b410140134', 'b3e635ef014ab607', '89294bdd2dc585ab', '2e45b2ff3d034689',
  '30b5c5e7e5c85fdc', 'f88487e7db1bfd97', '4accab310a86088a', '88cd85b13df25a7b',
  '21395c435f53b104', '1f17e646038d69dc', 'a0ec2bf32ce4c540', '72a85373c6d64de5',
  'bb185234a872f15b', '7cffc8a8b34591f0', '3f2fcf95fe1b9fe7', '28534169d232862a',
  'f8087ce215c6b6da', '2c55003713ebfdb0', 'b48ae8867f5d672f', '05e9ad6bebedbae3',
  '1289cc889902f269', '4531b3d2012116f9', '3abeb2a1cb46f58f', 'cf1e1a30efc6b6c0',
  'c3a9a9d1fe3134f4', '02608f0e048c230f', '1b5dc170b0d58e5f', '3fd4f957e26c22c5',
  'ff9a1cb433d8fe29', '2a9719d1b6c077f4', '07e7b072a3e7fa37', 'ad770e31871f00e2',
  'e22ff7d733e1452f', '8cf540b2548bc64d', '3b6d4afe1a8f7485', '04b674549bf519f5',
  '2d631e7e7f50fad2', 'c712f66ee5be2bf2', '273a323c16534132', '5e3485f5fe426cfb',
  'bec7b796c76873eb', '5fcd57de39191bcc', '72d58df94cb1450f', '1773de17a32e69f7',
  'f43cd77c9bd402f9', '350a8345b332aeb5', '7fa5b146051b688a', '2339578df087fc67',
  '81e9024452930efd', '0bce5444d9387471', '8376fded3dcdd755', 'f841e59f3ab0a4d1',
  'aaf6758db6dd5c7b', '8d3f59e050235d9e', '32e5c0cb1f5d9526', '9a5bda07ceaa4e99',
  '9c18803717980709'
]

cand_set = set(candidates_hashes)
pack_objs = {}
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        data = obj.read()
        name = getattr(data, 'm_Name', '')
        if name in cand_set:
            raw = data.m_Script.encode('utf-8', 'surrogateescape')
            pack_objs[name] = json.loads(gzip.decompress(raw))

print(f"Loaded {len(pack_objs)} candidate json objects.")

# Let's filter by exact features of Image 4:
# In Image 4:
# Aspect ratio: Width is approximately 18-20, Height is approximately 25-28
# Arrow count: around 45 to 65
# Row 0:
# (0, 0): arrow head pointing UP
# In Row 0, near x=4..7: arrow pointing LEFT <-
# Near x=8..12: arrow pointing RIGHT ->
# Far right (xs-1, 0): arrow head pointing UP
filtered = []
for h, lvl in pack_objs.items():
    xs = lvl['XSize']
    ys = lvl['YSize']
    narrs = len(lvl['Arrows'])
    if not (16 <= xs <= 22 and 22 <= ys <= 30 and 45 <= narrs <= 70):
        continue
    
    # Check top row
    top_arrows = []
    for a in lvl['Arrows']:
        pts = a['Points']
        head = pts[0]
        hx, hy = head % xs, head // xs
        if hy == 0:
            top_arrows.append((hx, pts[1] % xs - hx, pts[1] // xs - hy))
            
    # Check bottom row
    bottom_arrows = []
    for a in lvl['Arrows']:
        pts = a['Points']
        head = pts[0]
        hx, hy = head % xs, head // xs
        if hy >= ys - 2:
            bottom_arrows.append((hx, hy, pts[1] % xs - hx, pts[1] // xs - hy))
            
    filtered.append((h, xs, ys, narrs, top_arrows, len(bottom_arrows)))

print(f"Filtered down to {len(filtered)} candidates:")
for f in filtered:
    print(f"  {f[0]}: {f[1]}x{f[2]}, {f[3]} arrs, top={f[4]}")
