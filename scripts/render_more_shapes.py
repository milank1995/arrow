import os
import sys
sys.path.append('.')
import json
from scripts.render_shapes import render_ascii

# Check NewLevel_20
with open('arrow_native/app/src/main/assets/new_levels/NewLevel_20.json') as f:
    d = json.load(f)
    print("\n================ NewLevel_20 ================")
    print(render_ascii(d['XSize'], d['YSize'], d['Arrows']))

# Check some interesting old levels
old_dir = 'arrow_native/app/src/main/assets/levels'
for lvl in [6, 13, 25, 29, 41, 53]:
    p = os.path.join(old_dir, f'Level_{lvl}.json')
    if os.path.exists(p):
        with open(p) as f:
            d = json.load(f)
            print(f"\n================ Old Level_{lvl} ({d['XSize']}x{d['YSize']}, {len(d['Arrows'])} arrows) ================")
            print(render_ascii(d['XSize'], d['YSize'], d['Arrows']))
