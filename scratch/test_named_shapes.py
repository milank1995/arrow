import UnityPy
import re
import json
import gzip
import os

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'
with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

named_levels = []
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        if re.match(r'^[0-9a-fA-F]{11,16}$', name): continue
        if re.match(r'^(reason|rule|hole|pipes|level|test|sample)[-_0-9a-zA-Z]+$', name): continue
        if re.match(r'^[0-9]{4,}[-_]', name): continue
        if re.match(r'^(H|N|SH)-[0-9a-fA-F]+$', name): continue
        if re.match(r'^(QuadraPass|PipelineGuide)', name): continue
        
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        try:
            raw = json.loads(gzip.decompress(b))
            named_levels.append((name, raw))
        except:
            pass

print(f"Total candidate named levels: {len(named_levels)}")

def solve_level(xs, ys, arrows):
    if not arrows: return False
    occupied = {}
    heads, dirs, cells_list = [], [], []
    for a in arrows:
        pts = a.get('Points', [])
        if not pts or len(pts) < 2:
            continue
        i = len(heads)
        hx, hy = pts[0] % xs, pts[0] // xs
        nx, ny = pts[1] % xs, pts[1] // xs
        dx = 1 if hx > nx else (-1 if hx < nx else 0)
        dy = -(1 if hy > ny else (-1 if hy < ny else 0)) # canvas dy
        
        cset = set()
        for p in pts:
            ux, uy = p % xs, p // xs
            cx, cy = ux, (ys - 1) - uy
            cset.add((cx, cy))
            occupied[(cx, cy)] = i
        
        heads.append((hx, (ys - 1) - hy))
        dirs.append((dx, dy))
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
        if not prog:
            return False
    return True

solvable_named = []
for name, raw in named_levels:
    xs = raw.get('XSize', 0)
    ys = raw.get('YSize', 0)
    arrs = raw.get('Arrows', [])
    if solve_level(xs, ys, arrs):
        solvable_named.append((name, len(arrs), xs, ys, raw))

print(f"100% Solvable named shape levels: {len(solvable_named)}")
solvable_named.sort(key=lambda x: x[1])

with open('scratch/solvable_named_shapes.txt', 'w', encoding='utf-8') as f:
    for name, cnt, xs, ys, _ in solvable_named:
        f.write(f"{cnt:3d} arr | {xs:2d}x{ys:2d} | {name}\n")

print("Wrote list to scratch/solvable_named_shapes.txt")
