import subprocess
import json

p = 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_shape_puzzles/shape_levels.json'
out = subprocess.check_output(['git', 'show', p], encoding='utf-8')
shapes = json.loads(out)
named = [s for s in shapes if not s['name'].startswith('Shape #')]

print('Smallest named shapes:')
for s in sorted(named, key=lambda x: x['arrows'])[:25]:
    print(f"  {s['name']}: {s['arrows']} arrows (Lvl {s['level']})")
