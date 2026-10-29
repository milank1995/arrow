import json

with open('scratch/curated_150_plan.json', encoding='utf-8') as f:
    plan = json.load(f)

user_formatted = {}
for p in plan:
    lvl = p['level']
    src_short = {'Existing 1K Levels': 'Existing1K', 'Shape Puzzle Levels': 'ShapePuzzles', 'New Unity Normal Levels': 'UnityNormal', 'Large Puzzles': 'LargePuzzles'}[p['source_collection']]
    shape_str = p['name'].lower().replace(' ', '')
    diff_str = p['difficulty'].lower().replace(' ', '_')
    key = f"{lvl}.jpg_{src_short}_{shape_str}_{diff_str}"
    user_formatted[key] = p['arrows']

with open('scratch/user_formatted_150.json', 'w', encoding='utf-8') as f:
    json.dump(user_formatted, f, indent=2)

print("Saved scratch/user_formatted_150.json successfully!")
