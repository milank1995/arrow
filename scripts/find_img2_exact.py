import UnityPy
import gzip
import json

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

with open(packs_path, 'rb') as f:
    f.seek(32)
    env = UnityPy.load(f.read())

print("Searching for exact match to Image 2...")

# Look at Image 2:
# Top border:
# Leftmost: 3 vertical lanes going down and up:
# (0, 0) arrow goes down?
# At bottom left:
# concentric boxes in bottom rows!
# Let's inspect the candidate list from candidates_img2 in scripts/candidates.json:
with open('scripts/candidates.json', 'r') as f:
    cand = json.load(f)

img2_list = cand['img2']
print(f"Total img2 candidates: {len(img2_list)}")

# Let's check each candidate for the specific concentric U-turn structure in bottom rows
matched = []
for h, xs, ys, narrs in img2_list:
    # Look for narrow width ~20-30, height ~35-50, narrs ~50-80
    if 20 <= xs <= 30 and 35 <= ys <= 50 and 55 <= narrs <= 85:
        matched.append((h, xs, ys, narrs))

print(f"Filtered candidates: {len(matched)}")
for m in matched[:20]:
    print(" ", m)
