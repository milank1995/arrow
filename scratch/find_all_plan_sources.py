import subprocess
import json

p_shapes = json.loads(subprocess.check_output(['git', 'show', 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_shape_puzzles/shape_levels.json'], encoding='utf-8'))
p_1k = json.loads(subprocess.check_output(['git', 'show', 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_existing_1k/shape_levels.json'], encoding='utf-8'))
p_large = json.loads(subprocess.check_output(['git', 'show', 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_large_puzzles/shape_levels.json'], encoding='utf-8'))
p_unity = json.loads(subprocess.check_output(['git', 'show', 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_unity_normal/shape_levels.json'], encoding='utf-8'))

print(f"Shape counts: shapes={len(p_shapes)}, 1k={len(p_1k)}, large={len(p_large)}, unity={len(p_unity)}")

with open('scratch/curated_150_plan_v3.json', encoding='utf-8') as f:
    plan = json.load(f)

plan_shapes = [p for p in plan if p['is_shape']]
print(f"Plan shapes: {len(plan_shapes)}")

all_shapes = []
for s in p_shapes:
    all_shapes.append(('test_shape_puzzles', s))
for s in p_1k:
    all_shapes.append(('test_existing_1k', s))
for s in p_large:
    all_shapes.append(('test_large_puzzles', s))
for s in p_unity:
    all_shapes.append(('test_unity_normal', s))

for ps in plan_shapes:
    lvl = ps['level']
    pname = ps['name']
    arr = ps['arrows']
    matches = []
    for src, s in all_shapes:
        sname = s['name']
        if pname.lower() == sname.lower() or pname.lower() in sname.lower() or sname.lower() in pname.lower():
            matches.append((src, s))
    if matches:
        # sort by arrow closeness
        matches.sort(key=lambda m: abs(m[1]['arrows'] - arr))
        best = matches[0]
        print(f"L{lvl} '{pname}' ({arr} arr) -> {best[0]}: \"{best[1]['name']}\" (Lvl {best[1]['level']}, {best[1]['arrows']} arr)")
    else:
        print(f"L{lvl} '{pname}' ({arr} arr) -> NO MATCH!")
