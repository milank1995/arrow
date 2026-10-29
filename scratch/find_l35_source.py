import json
from check_unity_indices import unity

with open('arrow_native/app/src/main/assets/levels/Level_35.json') as f:
    l35 = json.load(f)

sig35 = (l35['XSize'], l35['YSize'], len(l35['Arrows']))
print("L35 sig:", sig35)

matches = []
for idx, l in enumerate(unity):
    if len(l['Arrows']) == 70 and l['XSize'] == 20 and l['YSize'] == 28:
        matches.append(idx)

print("Found matching indices in unity:", matches)
if matches:
    raw = unity[matches[0]]
    print("Hash:", raw.get('Hash'))
    print("Points in arrow 0:", len(raw['Arrows'][0].get('Indices', [])))
