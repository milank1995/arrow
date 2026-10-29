import os
import sys
import json
import gzip
import re
import UnityPy

sys.stdout.reconfigure(encoding='utf-8')
UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

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
                    lvl_obj = {
                        'XSize': xs,
                        'YSize': ys,
                        'Arrows': arrows_out,
                        'Hash': name
                    }
                    raw_large.append(lvl_obj)
        except:
            pass

print(f"Total raw large (250-450 arr): {len(raw_large)}")

# Deduplicate
def get_sig(lvl):
    return (lvl['XSize'], lvl['YSize'], tuple(sorted(a['Indices'][0] for a in lvl['Arrows'])))

seen = set()
unique_large = []
for lvl in raw_large:
    sig = get_sig(lvl)
    if sig not in seen:
        seen.add(sig)
        unique_large.append(lvl)

print(f"Unique large puzzles: {len(unique_large)}")

solvable_large = []
for lvl in unique_large:
    if solve_level(lvl):
        solvable_large.append(lvl)

print(f"100% Solvable unique large puzzles: {len(solvable_large)}")

solvable_large.sort(key=lambda x: len(x['Arrows']))
print("\nSolvability verified arrow counts for all large puzzles:")
for p in solvable_large:
    print(f"  {len(p['Arrows']):3d} arrows | Grid {p['XSize']}x{p['YSize']} | Hash: {p['Hash']}")
