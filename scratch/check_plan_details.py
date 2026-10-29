import json

with open('scratch/curated_150_plan_v3.json', encoding='utf-8') as f:
    plan = json.load(f)

print(f"Total levels in plan: {len(plan)}")
shapes = [p for p in plan if p['is_shape']]
print(f"Total shape levels: {len(shapes)}")
for s in shapes:
    print(f"L{s['level']}: {s['name']} (arrows={s['arrows']}, diff={s['difficulty']}, src={s['source_collection']})")
