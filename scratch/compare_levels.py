import json
import glob

levels = glob.glob('arrow_native/app/src/main/assets/levels/Level_*.json')
keys_found = set()
difficulties = set()
shapes = []
stats = []

for p in levels:
    with open(p, 'r', encoding='utf-8') as f:
        d = json.load(f)
        keys_found.update(d.keys())
        difficulties.add(d.get('Difficulty'))
        stats.append({
            'num': d.get('LevelNumber'),
            'xs': d.get('XSize'),
            'ys': d.get('YSize'),
            'arrows': d.get('ArrowCount', len(d.get('Arrows', []))),
            'shape': d.get('ShapeName'),
            'diff': d.get('Difficulty')
        })
        if d.get('ShapeName'):
            shapes.append(stats[-1])

print(f"Total levels analyzed: {len(levels)}")
print('Keys across all levels:', keys_found)
print('Difficulties in dataset:', difficulties)
print(f'Total levels with shapes: {len(shapes)}')

shapes.sort(key=lambda x: x['num'])
for s in shapes[:20]:
    print(s)

# Difficulty distribution by arrow count
for diff in ['Easy', 'Medium', 'Hard', 'Super Hard']:
    subset = [s['arrows'] for s in stats if s['diff'] == diff]
    if subset:
        print(f"Diff {diff}: count={len(subset)}, min={min(subset)}, max={max(subset)}, avg={sum(subset)/len(subset):.1f}")

# Look at 70 arrow levels specifically:
around_70 = [s for s in stats if 60 <= s['arrows'] <= 80]
print(f"\nLevels with 60-80 arrows (user has 70 arrows):")
for s in around_70[:10]:
    print(s)
