import UnityPy
import gzip
import json

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

target_h = '2339578df087fc67'
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        data = obj.read()
        if getattr(data, 'm_Name', '') == target_h:
            raw = data.m_Script.encode('utf-8', 'surrogateescape')
            lvl = json.loads(gzip.decompress(raw))
            break

xs = lvl['XSize']
ys = lvl['YSize']
grid = [[' ' for _ in range(xs)] for _ in range(ys)]

for arr_idx, a in enumerate(lvl['Arrows']):
    pts = a['Points']
    head = pts[0]
    hx, hy = head % xs, head // xs
    grid[hy][hx] = '^'
    for k in range(len(pts) - 1):
        p1, p2 = pts[k], pts[k+1]
        x1, y1 = p1 % xs, p1 // xs
        x2, y2 = p2 % xs, p2 // xs
        dx = 1 if x2 > x1 else (-1 if x2 < x1 else 0)
        dy = 1 if y2 > y1 else (-1 if y2 < y1 else 0)
        cx, cy = x1, y1
        while (cx != x2) or (cy != y2):
            if grid[cy][cx] == ' ':
                grid[cy][cx] = '#'
            cx += dx
            cy += dy
    tail = pts[-1]
    tx, ty = tail % xs, tail // xs
    if grid[ty][tx] == ' ':
        grid[ty][tx] = '#'

print(f"Grid for {target_h} ({xs}x{ys}):")
for r in range(ys):
    print(f"{r:2d}: " + ''.join(grid[r]))
