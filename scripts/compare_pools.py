import os
import json
import gzip
import UnityPy

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

levels_dir = 'arrow_native/app/src/main/assets/levels'
old_files = [f for f in os.listdir(levels_dir) if f.startswith('Level_')]

# Check how old Level_1 is defined
with open(os.path.join(levels_dir, 'Level_1.json')) as f:
    l1 = json.load(f)

print("Old Level_1 metadata:")
print(" XSize:", l1.get('XSize'), "YSize:", l1.get('YSize'))
print(" Arrows:", len(l1.get('Arrows', [])))
for i, a in enumerate(l1.get('Arrows', [])[:3]):
    print(f"  arrow {i}: x={a.get('X')}, y={a.get('Y')}, dx={a.get('Dx')}, dy={a.get('Dy')}, indices={a.get('Indices')}")

# Check new_levels/NewLevel_1.json
with open('arrow_native/app/src/main/assets/new_levels/NewLevel_1.json') as f:
    nl1 = json.load(f)
print("\nNewLevel_1 metadata:")
print(" XSize:", nl1.get('XSize'), "YSize:", nl1.get('YSize'))
print(" Arrows:", len(nl1.get('Arrows', [])))
for i, a in enumerate(nl1.get('Arrows', [])[:3]):
    print(f"  arrow {i}: x={a.get('X')}, y={a.get('Y')}, dx={a.get('Dx')}, dy={a.get('Dy')}, indices={a.get('Indices')}")
