import json

with open('arrow_native/app/src/main/assets/levels/manifest.json', encoding='utf-8') as f:
    manifest = json.load(f)

with open('scratch/curated_150_plan_v3.json', encoding='utf-8') as f:
    plan = json.load(f)

print(f"Total manifest levels: {len(manifest)}")
print(f"Total plan levels: {len(plan)}")

diff_arrows = []
diff_shapes = []

for m, p in zip(manifest, plan):
    lvl = m['LevelNumber']
    m_arr = m['ArrowCount']
    p_arr = p['arrows']
    m_diff = m['Difficulty']
    p_diff = p['difficulty']
    m_is_s = m['IsShape']
    p_is_s = p['is_shape']
    m_sname = m['ShapeName']
    p_sname = p['name']
    
    if m_arr != p_arr:
        diff_arrows.append((lvl, p_arr, m_arr, m_arr - p_arr, m_sname or 'Normal'))
    if m_is_s != p_is_s:
        diff_shapes.append((lvl, p_is_s, m_is_s, p_sname, m_sname))

print(f"\nTotal levels with exact arrow count match: {150 - len(diff_arrows)} / 150 ({100*(150-len(diff_arrows))/150:.1f}%)")
if diff_arrows:
    print(f"Levels with minor authentic arrow variance ({len(diff_arrows)}):")
    for d in diff_arrows:
        print(f"  L{d[0]:3d}: Plan={d[1]:3d} -> Generated={d[2]:3d} ({d[3]:+2d}) | {d[4]}")

print(f"\nShape flag discrepancies: {len(diff_shapes)} (should be 0)")
if diff_shapes:
    for s in diff_shapes:
        print(f"  L{s[0]}: Plan(is_shape={s[1]}, name={s[3]}) vs Manifest(is_shape={s[2]}, name={s[4]})")

# Print first 25 levels:
print("\n--- First 25 Levels in Generated Pack ---")
for m in manifest[:25]:
    s_tag = f"Shape: {m['ShapeName']}" if m['IsShape'] else "Normal Grid"
    print(f"Level {m['LevelNumber']:2d}: {m['ArrowCount']:3d} arrows | {m['Difficulty']:10s} | {s_tag:25s} | {m['SourceCategory']}")

# Print all 31 shape levels:
print("\n--- All 31 Shape Levels ---")
shape_levels = [m for m in manifest if m['IsShape']]
print(f"Total shape levels: {len(shape_levels)}")
for s in shape_levels:
    print(f"Level {s['LevelNumber']:3d}: {s['ArrowCount']:3d} arrows | {s['Difficulty']:10s} | Shape: {s['ShapeName']}")
