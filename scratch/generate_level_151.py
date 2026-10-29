import json

with open('scratch/lock_user_input.json', 'r', encoding='utf-8') as f:
    input_data = json.load(f)

# Format consistent with Level_1.json .. Level_150.json
level_151 = {
    "LevelNumber": 151,
    "XSize": input_data["XSize"],
    "YSize": input_data["YSize"],
    "MaxHearts": input_data.get("MaxHearts", 3),
    "ShapeName": input_data.get("ShapeName", "Lock"),
    "Difficulty": "Hard",
    "ArrowCount": len(input_data["Arrows"]),
    "Arrows": input_data["Arrows"]
}

out_path_151 = 'arrow_native/app/src/main/assets/levels/Level_151.json'
with open(out_path_151, 'w', encoding='utf-8') as f:
    json.dump(level_151, f, separators=(',', ':'))

out_path_lock = 'arrow_native/app/src/main/assets/levels/Level_Lock.json'
with open(out_path_lock, 'w', encoding='utf-8') as f:
    json.dump(level_151, f, indent=2)

# Update manifest.json
manifest_path = 'arrow_native/app/src/main/assets/levels/manifest.json'
with open(manifest_path, 'r', encoding='utf-8') as f:
    manifest = json.load(f)

# Check if 151 is already in manifest
manifest = [m for m in manifest if m.get('level') != 151]
manifest.append({
    "level": 151,
    "arrows": len(input_data["Arrows"]),
    "difficulty": "Hard",
    "shapeName": "Lock",
    "isShape": True
})

with open(manifest_path, 'w', encoding='utf-8') as f:
    json.dump(manifest, f, indent=2)

print("Generated Level_151.json, Level_Lock.json, and updated manifest.json successfully!")
