import json

# Define the 30 5-level brackets with target shapes and smooth arrow progression
# Early levels 1-10:
# L1: 4 (Unity)
# L2: 6 (1K)
# L3: 8 (1K)
# L4: 12 (1K)
# L5: 20 (Palm Tree Shape)
# L6: 26 (1K Breather)
# L7: 32 (Unity Normal)
# L8: 38 (Unity Normal Hard)
# L9: 44 (1K Breather)
# L10: 54 (Winecat / Robot Shape)

with open('scratch/curated_150_plan_v2.json', encoding='utf-8') as f:
    old_plan = json.load(f)

# Build improved plan
plan_v3 = []

# Exact shape assignments ensuring EVERY 5-level bracket has a shape:
# (bracket_index, level, shape_name, arrows, difficulty)
bracket_shapes = {
    1: (5, "Palm Tree", 22, "Medium"),      # Bracket 1-5 (Smooth 22 arrows!)
    2: (10, "Robot", 52, "Hard"),           # Bracket 6-10 (Fresh 52 arrows!)
    3: (15, "Panda", 81, "Hard"),           # Bracket 11-15
    4: (20, "Swan", 99, "Super Hard"),      # Bracket 16-20
    5: (25, "Strawberry", 130, "Super Hard"),# Bracket 21-25
    6: (28, "Black Cat", 57, "Medium"),     # Bracket 26-30
    7: (33, "Rainbow", 60, "Medium"),       # Bracket 31-35 (Added shape for 31-35!)
    8: (36, "Jellyfish", 40, "Medium"),     # Bracket 36-40
    9: (45, "Squirrel", 62, "Medium"),      # Bracket 41-45
    10: (49, "Cactus", 130, "Super Hard"),  # Bracket 46-50
    11: (53, "Hourglass", 45, "Medium"),    # Bracket 51-55
    12: (58, "Magnolia", 63, "Hard"),       # Bracket 56-60 (Added shape for 56-60!)
    13: (62, "Flower Vase", 70, "Medium"),  # Bracket 61-65
    14: (70, "Trophy", 77, "Hard"),         # Bracket 66-70 (Added shape for 66-70!)
    15: (71, "Windmill", 90, "Medium"),     # Bracket 71-75
    16: (79, "Snowflake", 68, "Medium"),    # Bracket 76-80
    17: (83, "Sparrow", 80, "Medium"),      # Bracket 81-85 (Added shape for 81-85!)
    18: (88, "Dog", 80, "Medium"),          # Bracket 86-90
    19: (93, "Seal", 80, "Medium"),         # Bracket 91-95 (Added shape for 91-95!)
    20: (96, "Rabbit", 64, "Medium"),       # Bracket 96-100
    21: (105, "Owl", 103, "Medium"),        # Bracket 101-105
    22: (109, "Spider", 105, "Super Hard"), # Bracket 106-110
    23: (113, "Cherry", 65, "Medium"),      # Bracket 111-115
    24: (119, "Honeybee", 82, "Medium"),    # Bracket 116-120 (Added shape for 116-120!)
    25: (122, "Star", 54, "Medium"),        # Bracket 121-125
    26: (128, "Sunflower", 67, "Super Hard"),# Bracket 126-130 (Added shape for 126-130!)
    27: (131, "Beach Umbrella", 56, "Medium"),# Bracket 131-135
    28: (140, "Truck", 122, "Super Hard"),  # Bracket 136-140
    29: (144, "Catstar", 126, "Medium"),    # Bracket 141-145 (Added shape for 141-145!)
    30: (148, "Airplane", 80, "Medium"),    # Bracket 146-150
}

# Invert bracket_shapes by level:
shape_by_lvl = {v[0]: v for v in bracket_shapes.values()}

# Early levels smooth ramp:
early_overrides = {
    1: ("New Unity Normal Levels", "UnityNormal_L1", "Introductory Grid", 4, "Medium"),
    2: ("Existing 1K Levels", "L2", "Tutorial Corner Clearance", 6, "Medium"),
    3: ("Existing 1K Levels", "L3", "Tutorial Cross Unblocking", 8, "Medium"),
    4: ("Existing 1K Levels", "L4", "Tutorial Corner Turn", 12, "Medium"),
    6: ("Existing 1K Levels", "L6", "Shape #1000 Mini Breather", 26, "Medium"),
    7: ("New Unity Normal Levels", "UnityNormal_L2", "Step Grid", 32, "Medium"),
    8: ("New Unity Normal Levels", "UnityNormal_L3", "Interlocking Corner", 38, "Hard"),
    9: ("Existing 1K Levels", "L9", "Classic Mini Maze", 44, "Medium"),
}

large_climaxes = {
    100: ("Large Puzzles", "Mega Grid Century Climax", 250, "Super Hard", False),
    150: ("Large Puzzles", "Sofa Grand Finale", 261, "Super Hard", True),
}

unity_counter = 4
for lvl in range(1, 151):
    if lvl in early_overrides:
        src, sid, name, arr, diff = early_overrides[lvl]
        plan_v3.append({
            "level": lvl, "source_collection": src, "source_id": sid,
            "name": name, "is_shape": False, "difficulty": diff, "arrows": arr
        })
    elif lvl in large_climaxes:
        src, name, arr, diff, is_s = large_climaxes[lvl]
        plan_v3.append({
            "level": lvl, "source_collection": src, "source_id": f"Large_L{lvl}",
            "name": name, "is_shape": is_s, "difficulty": diff, "arrows": arr
        })
    elif lvl in shape_by_lvl:
        _, name, arr, diff = shape_by_lvl[lvl]
        plan_v3.append({
            "level": lvl, "source_collection": "Shape Puzzle Levels", "source_id": f"Shape_L{lvl}",
            "name": name, "is_shape": True, "difficulty": diff, "arrows": arr
        })
    else:
        # Check old plan item for normal progression:
        old_item = old_plan[lvl - 1]
        src = old_item['source_collection']
        if src == "Shape Puzzle Levels":
            src = "New Unity Normal Levels"
        
        diff = old_item['difficulty']
        arr = old_item['arrows']
        name = "Geometric Abstract Grid"
        sid = f"UnityNormal_L{unity_counter}" if src == "New Unity Normal Levels" else old_item['source_id']
        if src == "New Unity Normal Levels":
            unity_counter += 1
        
        plan_v3.append({
            "level": lvl, "source_collection": src, "source_id": sid,
            "name": name, "is_shape": False, "difficulty": diff, "arrows": arr
        })

# Verify every 5-level bracket has at least 1 shape:
brackets_ok = True
for b in range(1, 31):
    start = (b - 1) * 5 + 1
    end = b * 5
    shapes_in_b = [p for p in plan_v3 if start <= p['level'] <= end and p['is_shape']]
    if not shapes_in_b:
        print(f"ERROR: Bracket {start}-{end} has NO shapes!")
        brackets_ok = False

if brackets_ok:
    print("ALL 30 5-level brackets contain at least 1 Shape Puzzle! SUCCESS!")

# Output formatted JSON:
user_formatted_v3 = {}
for p in plan_v3:
    lvl = p['level']
    src_short = {'Existing 1K Levels': 'Existing1K', 'Shape Puzzle Levels': 'ShapePuzzles', 'New Unity Normal Levels': 'UnityNormal', 'Large Puzzles': 'LargePuzzles'}[p['source_collection']]
    shape_str = p['name'].lower().replace(' ', '').replace('#', '')
    diff_str = p['difficulty'].lower().replace(' ', '_')
    key = f"{lvl}.jpg_{src_short}_{shape_str}_{diff_str}"
    user_formatted_v3[key] = p['arrows']

with open('scratch/curated_150_plan_v3.json', 'w', encoding='utf-8') as f:
    json.dump(plan_v3, f, indent=2)

with open('scratch/user_formatted_150_v3.json', 'w', encoding='utf-8') as f:
    json.dump(user_formatted_v3, f, indent=2)

print("Saved curated_150_plan_v3.json and user_formatted_150_v3.json!")
print(f"Total shapes in 150 levels: {sum(1 for p in plan_v3 if p['is_shape'])}")
