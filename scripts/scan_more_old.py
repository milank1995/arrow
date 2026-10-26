import os
import sys
sys.path.append('.')
import json
from scripts.render_shapes import render_ascii

old_dir = 'arrow_native/app/src/main/assets/levels'
# Let's check a few more low-occupancy levels in Old Levels:
for lvl in [3, 4, 5, 7, 15, 17, 28, 30, 33, 37, 45, 61, 64, 80, 100]:
    p = os.path.join(old_dir, f'Level_{lvl}.json')
    if os.path.exists(p):
        with open(p) as f:
            d = json.load(f)
            print(f"\n================ Old Level_{lvl} ({d['XSize']}x{d['YSize']}, {len(d['Arrows'])} arrows) ================")
            print(render_ascii(d['XSize'], d['YSize'], d['Arrows']))
