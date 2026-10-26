import UnityPy
import json
import re

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
config_bundle_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'

with open('scripts/candidates.json', 'r') as f:
    cand = json.load(f)

img4_hashes = set(x[0] for x in cand['img4'])
img2_hashes = set(x[0] for x in cand['img2'])

with open(config_bundle_path, 'rb') as f:
    f.seek(32)
    env = UnityPy.load(f.read())

matches_img4 = []
matches_img2 = []

for obj in env.objects:
    if obj.type.name == 'TextAsset':
        data = obj.read()
        name = getattr(data, 'm_Name', '')
        b = data.m_Script.encode('utf-8', 'surrogateescape')
        hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
        
        # Check if index 25 (Level 26) is in img4_hashes
        if len(hashes) > 25 and hashes[25] in img4_hashes:
            matches_img4.append((name, 26, hashes[25]))
            
        # Check if index 24 (Level 25) is in img2_hashes
        if len(hashes) > 24 and hashes[24] in img2_hashes:
            matches_img2.append((name, 25, hashes[24]))

print("Configs where Level 26 matches Image 4 candidate:")
for m in matches_img4:
    print(" ", m)

print("\nConfigs where Level 25 matches Image 2 candidate:")
for m in matches_img2:
    print(" ", m)
