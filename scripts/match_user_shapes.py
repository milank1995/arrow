import subprocess
import json

p = 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_shape_puzzles/shape_levels.json'
out = subprocess.check_output(['git', 'show', p], encoding='utf-8')
shapes = json.loads(out)

p1k = 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_existing_1k/shape_levels.json'
out1k = subprocess.check_output(['git', 'show', p1k], encoding='utf-8')
shapes1k = json.loads(out1k)

user_shapes = ['goggles', 'oillamp', 'lamp', 'plant', 'man', 'puzzle', 'scholaraward', 'chakra', 'camera', 'dollar', 'flower', 'windmill', 'twoglass', 'snowflake', 'tyre', 'dog', 'owl', 'spider', 'cherry', 'halfstar', 'beach', 'truck', 'airplane']

print('Searching shapes in test_shape_puzzles (788 shapes):')
for us in user_shapes:
    matches = [s for s in shapes if us in s['name'].lower() or s['name'].lower() in us or any(w in s['name'].lower() for w in us.split())]
    if matches:
        m = matches[0]
        print(f"Match for {us}: {m['name']} (Lvl {m['level']}, {m['arrows']} arrows)")
    else:
        # check 1k
        m1k = [s for s in shapes1k if us in s['name'].lower()]
        if m1k:
            print(f"Match for {us} in 1K: {m1k[0]['name']} (Lvl {m1k[0]['level']}, {m1k[0]['arrows']} arrows)")
        else:
            print(f"NO match for {us}")
