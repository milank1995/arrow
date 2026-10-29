import json

with open('arrow_native/app/src/main/assets/levels/manifest.json') as f:
    m = json.load(f)

for lvl in [33, 100, 150]:
    item = m[lvl - 1]
    print(f"Level {item['LevelNumber']}: {item['ArrowCount']} arrows, diff={item['Difficulty']}, shape='{item['ShapeName']}', isShape={item['IsShape']}, size={item['XSize']}x{item['YSize']}")
