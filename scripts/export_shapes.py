import subprocess
import json

p = 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_shape_puzzles/shape_levels.json'
out = subprocess.check_output(['git', 'show', p], encoding='utf-8')
shapes = json.loads(out)

with open('scratch/all_shape_names.txt', 'w', encoding='utf-8') as f:
    for s in shapes:
        f.write(f"Lvl {s['level']}: {s['name']} ({s['arrows']} arrows)\n")

print(f"Exported {len(shapes)} shapes to scratch/all_shape_names.txt")
