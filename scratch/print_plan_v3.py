import json

with open('scratch/curated_150_plan_v3.json', encoding='utf-8') as f:
    plan = json.load(f)

for i, p in enumerate(plan):
    lvl = p['level']
    arr = p['arrows']
    diff = p['difficulty']
    is_s = p['is_shape']
    name = p['name']
    src = p['source_collection']
    sid = p.get('source_id', '')
    print(f"L{lvl:3d}: {arr:3d} arrows | Diff: {diff:10s} | Shape: {str(is_s):5s} | Name: {name:30s} | Src: {src} ({sid})")
