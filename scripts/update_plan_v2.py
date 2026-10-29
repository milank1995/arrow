import json

# User's reference dictionary (exact arrow counts and difficulties):
user_ref = {
  "1.jpg_rectangle_simple": 4, "2.jpg_rectangle_simple": 4, "3.jpg_rectangle_simple": 4, "4.jpg_rectangle_simple": 7,
  "5.jpg_goggles_hard": 54, "6.jpg_rectangle_simple": 28, "7.jpg_rectangle_simple": 54, "8.jpg_rectangle_hard": 78,
  "9.jpg_rectangle_simple": 36, "10.jpg_oillamp_hard": 99, "11.jpg_rectangle_simple": 45, "12.jpg_rectangle_simple": 62,
  "13.jpg_rectangle_hard": 80, "14.jpg_rectangle_simple": 46, "15.jpg_lamp_hard": 81, "16.jpg_rectangle_simple": 55,
  "17.jpg_rectangle_simple": 59, "18.jpg_rectangle_hard": 112, "19.jpg_rectangle_simple": 68, "20.jpg_plant_super_hard": 130,
  "21.jpg_rectangle_simple": 75, "22.jpg_rectangle_hard": 161, "23.jpg_rectangle_simple": 83, "24.jpg_rectangle_simple": 41,
  "25.jpg_rectangle_super_hard": 177, "26.jpg_rectangle_simple": 77, "27.jpg_rectangle_hard": 158, "28.jpg_man_simple": 57,
  "29.jpg_rectangle_simple": 55, "30.jpg_rectangle_super_hard": 158, "31.jpg_rectangle_simple": 107, "32.jpg_rectangle_super_hard": 167,
  "33.jpg_rectangle_simple": 54, "34.jpg_rectangle_hard": 164, "35.jpg_rectangle_simple": 70, "36.jpg_puzzle_simple": 40,
  "37.jpg_rectangle_super_hard": 188, "38.jpg_rectangle_simple": 102, "39.jpg_rectangle_hard": 165, "40.jpg_rectangle_simple": 135,
  "41.jpg_rectangle_simple": 45, "42.jpg_rectangle_super_hard": 221, "43.jpg_rectangle_simple": 60, "44.jpg_rectangle_super_hard": 192,
  "45.jpg_scholaraward_simple": 62, "46.jpg_chakra_hard": 131, "47.jpg_rectangle_simple": 54, "48.jpg_rectangle_simple": 94,
  "49.jpg_camera_super_hard": 130, "50.jpg_rectangle_simple": 53, "51.jpg_rectangle_hard": 180, "52.jpg_rectangle_simple": 85,
  "53.jpg_dollar_simple": 45, "54.jpg_rectangle_super_hard": 200, "55.jpg_rectangle_simple": 124, "56.jpg_rectangle_super_hard": 167,
  "57.jpg_rectangle_simple": 114, "58.jpg_rectangle_hard": 168, "59.jpg_rectangle_simple": 42, "60.jpg_rectangle_simple": 136,
  "61.jpg_rectangle_super_hard": 177, "62.jpg_flower_simple": 70, "63.jpg_rectangle_hard": 171, "64.jpg_rectangle_simple": 88,
  "65.jpg_rectangle_simple": 50, "66.jpg_rectangle_super_hard": 203, "67.jpg_rectangle_simple": 72, "68.jpg_rectangle_super_hard": 194,
  "69.jpg_rectangle_simple": 60, "70.jpg_rectangle_hard": 173, "71.jpg_windmill_simple": 90, "72.jpg_rectangle_simple": 88,
  "73.jpg_rectangle_super_hard": 169, "74.jpg_rectangle_simple": 28, "75.jpg_twoglass_hard": 90, "76.jpg_rectangle_simple": 40,
  "77.jpg_rectangle_simple": 55, "78.jpg_rectangle_super_hard": 194, "79.jpg_snowflake_simple": 68, "80.jpg_tyre_super_hard": 167,
  "81.jpg_rectangle_simple": 60, "82.jpg_rectangle_hard": 138, "83.jpg_rectangle_simple": 56, "84.jpg_rectangle_simple": 55,
  "85.jpg_rectangle_super_hard": 166, "86.jpg_rectangle_simple": 63, "87.jpg_rectangle_hard": 177, "88.jpg_dog_simple": 80,
  "89.jpg_rectangle_simple": 50, "90.jpg_rectangle_super_hard": 167, "91.jpg_rectangle_simple": 53, "92.jpg_rectangle_super_hard": 180,
  "93.jpg_rectangle_simple": 65, "94.jpg_rectangle_hard": 170, "95.jpg_rectangle_simple": 42, "96.jpg_<unknown_shape>_simple": 64,
  "97.jpg_rectangle_super_hard": 191, "98.jpg_rectangle_simple": 55, "99.jpg_rectangle_hard": 154, "100.jpg_rectangle_simple": 34,
  "101.jpg_rectangle_simple": 52, "102.jpg_rectangle_super_hard": 227, "103.jpg_rectangle_simple": 132, "104.jpg_rectangle_super_hard": 145,
  "105.jpg_owl_simple": 103, "106.jpg_oillamp_hard": 121, "107.jpg_rectangle_simple": 76, "108.jpg_rectangle_simple": 75,
  "109.jpg_spider_super_hard": 105, "110.jpg_rectangle_simple": 88, "111.jpg_rectangle_hard": 171, "112.jpg_rectangle_simple": 93,
  "113.jpg_cherry_simple": 65, "114.jpg_rectangle_super_hard": 120, "115.jpg_rectangle_simple": 60, "116.jpg_rectangle_super_hard": 214,
  "117.jpg_rectangle_simple": 43, "118.jpg_rectangle_hard": 172, "119.jpg_rectangle_simple": 107, "120.jpg_rectangle_simple": 54,
  "121.jpg_rectangle_super_hard": 202, "122.jpg_halfstar_simple": 54, "123.jpg_rectangle_hard": 169, "124.jpg_rectangle_simple": 140,
  "125.jpg_rectangle_simple": 148, "126.jpg_rectangle_super_hard": 120, "127.jpg_rectangle_simple": 78, "128.jpg_rectangle_super_hard": 188,
  "129.jpg_rectangle_simple": 60, "130.jpg_rectangle_hard": 182, "131.jpg_beach_simple": 56, "132.jpg_rectangle_simple": 59,
  "133.jpg_rectangle_super_hard": 182, "134.jpg_rectangle_simple": 151, "135.jpg_<unknown_shape>_hard": 142, "136.jpg_rectangle_simple": 147,
  "137.jpg_rectangle_simple": 59, "138.jpg_rectangle_super_hard": 179, "139.jpg_<unknown_shape>_simple": 85, "140.jpg_truck_super_hard": 122,
  "141.jpg_rectangle_simple": 158, "142.jpg_rectangle_hard": 151, "143.jpg_rectangle_simple": 75, "144.jpg_rectangle_simple": 75,
  "145.jpg_rectangle_super_hard": 216, "146.jpg_rectangle_simple": 69, "147.jpg_rectangle_hard": 187, "148.jpg_airplane_simple": 80,
  "149.jpg_rectangle_simple": 82, "150.jpg_rectangle_super_hard": 112
}

# 1. Level 1 from Unity collection (4 arrows)
# 2. Levels 2-4 from Existing 1K collection (4, 4, 7 arrows)
early_levels = {
    1: ("New Unity Normal Levels", "UnityNormal_L1", "Introductory Grid", 4, "Medium"),
    2: ("Existing 1K Levels", "L2", "Tutorial Corner Clearance", 4, "Medium"),
    3: ("Existing 1K Levels", "L3", "Tutorial Cross Unblocking", 4, "Medium"),
    4: ("Existing 1K Levels", "L4", "Tutorial Corner Turn", 7, "Medium"),
}

# 3. Shape Puzzle assignments (Fresh, unique, engaging shapes, NO Goggles or Lamp in first 50 levels! Exact arrow counts preserved!):
shape_mapping = {
    5: ("Shape Puzzle Levels", "Winecat", 54, "Hard"),        # Fresh 54-arrow shape (NO goggles!)
    10: ("Shape Puzzle Levels", "Swan", 99, "Hard"),           # Fresh 99-arrow shape (NO oillamp / table lamp!)
    15: ("Shape Puzzle Levels", "Panda", 81, "Hard"),          # Fresh 81-arrow shape (NO lamp!)
    20: ("Shape Puzzle Levels", "Strawberry", 130, "Super Hard"), # Fresh 130-arrow shape (NO plant!)
    28: ("Shape Puzzle Levels", "Black Cat", 57, "Medium"),    # Fresh 57-arrow animal shape
    36: ("Shape Puzzle Levels", "Jellyfish", 40, "Medium"),    # Fresh 40-arrow marine shape
    45: ("Shape Puzzle Levels", "Squirrel", 62, "Medium"),     # Fresh 62-arrow wildlife shape
    46: ("Shape Puzzle Levels", "Cat", 131, "Hard"),           # Fresh 131-arrow animal shape
    49: ("Shape Puzzle Levels", "Cactus", 130, "Super Hard"),  # Fresh 130-arrow nature shape
    53: ("Shape Puzzle Levels", "Hourglass", 45, "Medium"),
    62: ("Shape Puzzle Levels", "Flower Vase", 70, "Medium"),
    71: ("Shape Puzzle Levels", "Windmill", 90, "Medium"),
    75: ("Shape Puzzle Levels", "Teapot", 90, "Hard"),
    79: ("Shape Puzzle Levels", "Snowflake", 68, "Medium"),
    80: ("Shape Puzzle Levels", "Ferris Wheel", 167, "Super Hard"),
    88: ("Shape Puzzle Levels", "Dog", 80, "Medium"),
    96: ("Shape Puzzle Levels", "Rabbit", 64, "Medium"),
    105: ("Shape Puzzle Levels", "Owl", 103, "Medium"),
    106: ("Shape Puzzle Levels", "Treasurepot", 121, "Hard"),
    109: ("Shape Puzzle Levels", "Spider", 105, "Super Hard"),
    113: ("Shape Puzzle Levels", "Cherry", 65, "Medium"),
    122: ("Shape Puzzle Levels", "Star", 54, "Medium"),
    131: ("Shape Puzzle Levels", "Beach Umbrella", 56, "Medium"),
    135: ("Shape Puzzle Levels", "Hangeoman", 142, "Hard"),
    139: ("Shape Puzzle Levels", "Bar Cart", 85, "Medium"),
    140: ("Shape Puzzle Levels", "Truck", 122, "Super Hard"),
    148: ("Shape Puzzle Levels", "Airplane", 80, "Medium"),
}

# Special Large Puzzle boss climaxes:
large_puzzle_mapping = {
    100: ("Large Puzzles", "Mega Grid Climax", 250, "Super Hard"),
    150: ("Large Puzzles", "Sofa Grand Finale", 261, "Super Hard")
}

# Selected Existing 1K compact breathers:
existing_1k_breathers = {
    6: ("Existing 1K Levels", "L6", "Shape #1000 Mini Breather", 28, "Medium"),
    7: ("Existing 1K Levels", "L7", "Classic Grid Breather", 54, "Medium"),
    9: ("Existing 1K Levels", "L9", "Classic Mini Maze", 36, "Medium"),
    11: ("Existing 1K Levels", "L11", "Classic Compact Grid", 45, "Medium"),
    12: ("Existing 1K Levels", "L12", "Classic Stepped Grid", 62, "Medium"),
    14: ("Existing 1K Levels", "L14", "Classic Double Corner", 46, "Medium"),
    16: ("Existing 1K Levels", "L16", "Shape #125 Milestone Breather", 55, "Medium"),
    24: ("Existing 1K Levels", "L24", "Classic Pocket Breather", 41, "Medium"),
    50: ("Existing 1K Levels", "L50", "Classic Golden Breather", 53, "Medium"),
    74: ("Existing 1K Levels", "L74", "Classic Relief Breather", 28, "Medium"),
}

plan = []
user_formatted = {}
unity_normal_counter = 2

for k, v in user_ref.items():
    parts = k.replace('.jpg', '').split('_')
    lvl = int(parts[0])
    raw_diff = 'Super Hard' if parts[-2:] == ['super', 'hard'] else ('Hard' if parts[-1] == 'hard' else 'Medium')
    
    if lvl in early_levels:
        src, sid, name, arr, diff = early_levels[lvl]
        plan.append({
            "level": lvl, "source_collection": src, "source_id": sid,
            "name": name, "is_shape": False, "difficulty": diff, "arrows": arr
        })
    elif lvl in large_puzzle_mapping:
        src, name, arr, diff = large_puzzle_mapping[lvl]
        plan.append({
            "level": lvl, "source_collection": src, "source_id": f"Large_L{lvl}",
            "name": name, "is_shape": ("Sofa" in name), "difficulty": diff, "arrows": arr
        })
    elif lvl in shape_mapping:
        src, name, arr, diff = shape_mapping[lvl]
        plan.append({
            "level": lvl, "source_collection": src, "source_id": f"Shape_L{lvl}",
            "name": name, "is_shape": True, "difficulty": diff, "arrows": arr
        })
    elif lvl in existing_1k_breathers:
        src, sid, name, arr, diff = existing_1k_breathers[lvl]
        plan.append({
            "level": lvl, "source_collection": src, "source_id": sid,
            "name": name, "is_shape": ("Shape #" in name), "difficulty": diff, "arrows": arr
        })
    else:
        src = "New Unity Normal Levels"
        sid = f"UnityNormal_L{unity_normal_counter}"
        unity_normal_counter += 1
        plan.append({
            "level": lvl, "source_collection": src, "source_id": sid,
            "name": "Geometric Abstract Grid", "is_shape": False, "difficulty": raw_diff, "arrows": v
        })

for p in plan:
    lvl = p['level']
    src_short = {'Existing 1K Levels': 'Existing1K', 'Shape Puzzle Levels': 'ShapePuzzles', 'New Unity Normal Levels': 'UnityNormal', 'Large Puzzles': 'LargePuzzles'}[p['source_collection']]
    shape_str = p['name'].lower().replace(' ', '')
    diff_str = p['difficulty'].lower().replace(' ', '_')
    key = f"{lvl}.jpg_{src_short}_{shape_str}_{diff_str}"
    user_formatted[key] = p['arrows']

with open('scratch/curated_150_plan_v2.json', 'w', encoding='utf-8') as f:
    json.dump(plan, f, indent=2)

with open('scratch/user_formatted_150_v2.json', 'w', encoding='utf-8') as f:
    json.dump(user_formatted, f, indent=2)

print("Generated curated_150_plan_v2.json and user_formatted_150_v2.json!")
print("Total levels:", len(plan))
# Verify first 50 levels shapes:
first_50_shapes = [p for p in plan if p['level'] <= 50 and p['is_shape']]
print(f"First 50 levels shapes ({len(first_50_shapes)}):")
for s in first_50_shapes:
    print(f"  Level {s['level']}: {s['name']} ({s['arrows']} arrows, {s['difficulty']})")

# Check arrows match reference in first 50:
mismatches = []
ref_items = list(user_ref.items())
for i in range(50):
    ref_k, ref_v = ref_items[i]
    cur = plan[i]
    if cur['arrows'] != ref_v:
        mismatches.append((cur['level'], cur['arrows'], ref_v))
print("First 50 arrows mismatches with user reference:", mismatches)
