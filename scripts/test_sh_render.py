import UnityPy
import gzip
import json

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

test_hashes = ['64c4a9e4c65840c1', '8c1d89c05851d1f9', 'c8297f81c66e44d3', 'cfe301ba8b847dc9', 'a788d30f5ba28408', '158b50b8866ba2e8']

for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        data = obj.read()
        name = getattr(data, 'm_Name', '')
        if name in test_hashes:
            raw = data.m_Script.encode('utf-8', 'surrogateescape')
            lvl = json.loads(gzip.decompress(raw))
            xs = lvl['XSize']
            ys = lvl['YSize']
            # print bottom 8 rows in ASCII
            grid = [[' ' for _ in range(xs)] for _ in range(ys)]
            for a in lvl['Arrows']:
                pts = a['Points']
                for k in range(len(pts) - 1):
                    p1, p2 = pts[k], pts[k+1]
                    x1, y1 = p1 % xs, p1 // xs
                    x2, y2 = p2 % xs, p2 // xs
                    dx = 1 if x2 > x1 else (-1 if x2 < x1 else 0)
                    dy = 1 if y2 > y1 else (-1 if y2 < y1 else 0)
                    cx, cy = x1, y1
                    while (cx != x2) or (cy != y2):
                        grid[cy][cx] = '#'
                        cx += dx
                        cy += dy
                tail = pts[-1]
                grid[tail // xs][tail % xs] = '#'
                head = pts[0]
                grid[head // xs][head % xs] = '^'
            print(f"=== {name} ({xs}x{ys}) Bottom 8 rows ===")
            for r in range(ys - 8, ys):
                print(f"{r:2d}: " + ''.join(grid[r]))
