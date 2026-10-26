import UnityPy
import gzip
import json

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

print("Searching for Image 4 (Level 26)...")

candidates = []
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        data = obj.read()
        name = getattr(data, 'm_Name', '')
        if len(name) != 16:
            continue
        raw = data.m_Script.encode('utf-8', 'surrogateescape')
        try:
            lvl = json.loads(gzip.decompress(raw))
        except Exception:
            continue
        
        xs = lvl.get('XSize', 0)
        ys = lvl.get('YSize', 0)
        arrs = lvl.get('Arrows', [])
        
        # Check if rectangle (xs >= 12, ys >= 15)
        # In Image 4:
        # Far-left (0, 0): arrow head at x=0 pointing UP (dy = -1 or head at top edge)
        # Far-right (xs-1, 0): arrow head at x=xs-1 pointing UP
        has_top_left_up = False
        has_top_right_up = False
        has_bottom_left_arrow = False
        
        for a in arrs:
            pts = a.get('Points', [])
            if len(pts) >= 2:
                h = pts[0]
                hx, hy = h % xs, h // xs
                nx, ny = pts[1] % xs, pts[1] // xs
                if hx == 0 and hy == 0 and ny > hy: # head at (0,0) pointing UP
                    has_top_left_up = True
                if hx == xs - 1 and hy == 0 and ny > hy: # head at (xs-1, 0) pointing UP
                    has_top_right_up = True
                if hx <= 2 and hy == ys - 1 and nx > hx: # head near bottom left pointing LEFT
                    has_bottom_left_arrow = True
                    
        if has_top_left_up and has_top_right_up and has_bottom_left_arrow:
            candidates.append((name, xs, ys, len(arrs)))

print(f"Found {len(candidates)} exact candidates for Image 4:")
for c in candidates:
    print(" ", c)
