import UnityPy
import gzip
import json

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(packs_path, 'rb') as f:
    f.seek(32)
    env_pack = UnityPy.load(f.read())

print("Scanning pack objects...")

candidates_img2 = []
candidates_img4 = []

count = 0
for obj in env_pack.objects:
    if obj.type.name == 'TextAsset':
        count += 1
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
        num_arrs = len(arrs)
        
        # Image 4: rectangle, around 15-25 wide, 20-35 high, ~40-70 arrows
        # Bottom-most row has horizontal arrow pointing LEFT
        if 14 <= xs <= 26 and 18 <= ys <= 36 and 35 <= num_arrs <= 75:
            # check if there is an arrow pointing LEFT at the bottom
            has_bottom_left = False
            for a in arrs:
                pts = a.get('Points', [])
                if len(pts) >= 2:
                    h = pts[0]
                    hx, hy = h % xs, h // xs
                    nx, ny = pts[1] % xs, pts[1] // xs
                    # pointing left: hx < nx and hy == ny
                    # near bottom: hy >= ys - 3
                    if hx < nx and hy == ny and hy >= ys - 3 and (pts[-1] % xs) - hx > 5:
                        has_bottom_left = True
                        break
            if has_bottom_left:
                candidates_img4.append((name, xs, ys, num_arrs))
        
        # Image 2: Super Hard milestone level, around 20-35 wide, 35-55 high
        if 20 <= xs <= 35 and 35 <= ys <= 55 and 50 <= num_arrs <= 95:
            # Check bottom-most rows for concentric shapes or boxes
            candidates_img2.append((name, xs, ys, num_arrs))

print(f"Scanned {count} objects.")
print(f"Candidates for Image 4: {len(candidates_img4)}")
print(f"Candidates for Image 2: {len(candidates_img2)}")

# Save candidates to file
with open('scripts/candidates.json', 'w') as f_out:
    json.dump({'img4': candidates_img4, 'img2': candidates_img2}, f_out, indent=2)
print("Saved candidates.json")
