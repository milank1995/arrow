import subprocess
import json

p = 'backup/feature-new-levels-test-v2:arrow_native/app/src/main/assets/levels/test_shape_puzzles/shape_levels.json'
out = subprocess.check_output(['git', 'show', p], encoding='utf-8')
shapes = json.loads(out)

named_shapes = [s for s in shapes if not s['name'].startswith('Shape #')]

target_ranges = [
    (5, 54, 'around 54 arrows (for L5)'),
    (10, 99, 'around 99 arrows (for L10)'),
    (15, 81, 'around 81 arrows (for L15)'),
    (20, 130, 'around 130 arrows (for L20)'),
    (28, 57, 'around 57 arrows (for L28)'),
    (36, 40, 'around 40 arrows (for L36)'),
    (45, 62, 'around 62 arrows (for L45)'),
    (46, 131, 'around 131 arrows (for L46)'),
    (49, 130, 'around 130 arrows (for L49)'),
]

with open('scratch/shape_candidates.txt', 'w', encoding='utf-8') as f:
    for lvl, target, desc in target_ranges:
        f.write(f"=== Level {lvl}: Target {target} arrows ({desc}) ===\n")
        candidates = [s for s in named_shapes if abs(s['arrows'] - target) <= 8 and 'goggle' not in s['name'].lower() and 'lamp' not in s['name'].lower()]
        candidates.sort(key=lambda s: abs(s['arrows'] - target))
        for c in candidates[:12]:
            f.write(f"  Shape: {c['name']} (Lvl {c['level']}, {c['arrows']} arrows)\n")

print("Wrote candidates to scratch/shape_candidates.txt")
