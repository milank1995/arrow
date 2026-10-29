import sys
sys.stdout.reconfigure(encoding='utf-8')
sys.path.append('scratch')
import json
from test_windmill_isolated import shapes

with open('arrow_native/app/src/main/assets/levels/Level_33.json') as f:
    l33 = json.load(f)

print("L33 in assets:", l33['XSize'], l33['YSize'], len(l33['Arrows']), l33['ShapeName'])
for idx, s in enumerate(shapes):
    if len(s['Arrows']) == 80 and s['XSize'] == l33['XSize'] and s['YSize'] == l33['YSize']:
        print(f"Match in shapes at idx {idx}: Name='{s.get('ShapeName')}', hash='{s.get('Hash')}'")

# Also check other levels named Rainbow in shapes
print("\nAll shapes containing Rainbow:")
for s in shapes:
    if 'rainbow' in s.get('ShapeName', '').lower():
        print(f"Rainbow shape: '{s.get('ShapeName')}', {len(s['Arrows'])} arrows, {s['XSize']}x{s['YSize']}")
