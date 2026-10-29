import subprocess
import json

with open('scratch/curated_150_plan_v3.json', encoding='utf-8') as f:
    plan = json.load(f)

p_shapes = json.loads(subprocess.check_output(['git', 'show', 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_shape_puzzles/shape_levels.json'], encoding='utf-8'))

plan_shapes = [p for p in plan if p['is_shape']]

for ps in plan_shapes:
    lvl = ps['level']
    pname = ps['name']
    arr = ps['arrows']
    
    # direct or partial matches
    matches = [s for s in p_shapes if pname.lower() in s['name'].lower() or s['name'].lower() in pname.lower()]
    if matches:
        matches.sort(key=lambda s: abs(s['arrows'] - arr))
        best = matches[0]
        print(f"L{lvl:3d}: Target '{pname}' ({arr} arr) -> Match '{best['name']}' (Lvl {best['level']}, {best['arrows']} arr)")
    else:
        # find shapes in p_shapes with exact or closest arrow count
        close = sorted(p_shapes, key=lambda s: abs(s['arrows'] - arr))[:5]
        close_str = ", ".join([f"'{s['name']}' ({s['arrows']})" for s in close])
        print(f"L{lvl:3d}: Target '{pname}' ({arr} arr) -> NO EXACT NAME. Closest arrow shapes: {close_str}")
