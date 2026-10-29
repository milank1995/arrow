import json
from test_accurate_matching import one_k, unity, shapes, large

with open('arrow_native/app/src/main/assets/levels/Level_35.json') as f:
    l35 = json.load(f)

for idx, l in enumerate(one_k):
    if len(l['Arrows']) == 70 and l['XSize'] == 20 and l['YSize'] == 28:
        print(f"Found in one_k at idx {idx}! ShapeName: '{l.get('ShapeName')}'")

for idx, l in enumerate(unity):
    if len(l['Arrows']) == 70 and l['XSize'] == 20 and l['YSize'] == 28:
        print(f"Found in unity at idx {idx}! ShapeName: '{l.get('ShapeName')}'")

for idx, l in enumerate(shapes):
    if len(l['Arrows']) == 70 and l['XSize'] == 20 and l['YSize'] == 28:
        print(f"Found in shapes at idx {idx}! ShapeName: '{l.get('ShapeName')}'")
