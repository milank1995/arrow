import os
import json

def analyze_shapes_in_set(dir_path, prefix, max_count):
    special_shapes = []
    
    for i in range(1, max_count + 1):
        fname = f"{prefix}_{i}.json"
        p = os.path.join(dir_path, fname)
        if not os.path.exists(p):
            continue
        with open(p, 'r', encoding='utf-8') as f:
            d = json.load(f)
        xs = d.get('XSize') or d.get('xSize')
        ys = d.get('YSize') or d.get('ySize')
        arrows = d.get('Arrows') or d.get('arrows', [])
        if xs < 10 or ys < 10:
            continue
        
        # Calculate grid occupancy
        occupied = set()
        for a in arrows:
            for idx in a.get('Indices', []):
                occupied.add(idx)
                
        total_cells = xs * ys
        occupancy_ratio = len(occupied) / total_cells
        
        # Check boundary cells: if corners and outer rows/cols are mostly empty, it's a custom silhouette shape!
        empty_corners = 0
        corners = [0, xs - 1, (ys - 1) * xs, ys * xs - 1]
        for c in corners:
            if c not in occupied:
                empty_corners += 1
                
        # If occupancy is under 70% and corners are empty, or highly asymmetric
        if occupancy_ratio < 0.65 and empty_corners >= 2:
            special_shapes.append({
                'level': i,
                'xs': xs,
                'ys': ys,
                'arrows': len(arrows),
                'occupancy': occupancy_ratio,
                'empty_corners': empty_corners,
                'arrow_list': arrows
            })
            
    return special_shapes

old_dir = 'arrow_native/app/src/main/assets/levels'
new_dir = 'arrow_native/app/src/main/assets/new_levels'

print("Scanning Old Levels for special silhouette shapes...")
old_shapes = analyze_shapes_in_set(old_dir, 'Level', 1001)
print(f"Old Levels with custom silhouette shapes: {len(old_shapes)}")
for s in old_shapes[:25]:
    print(f"  Level {s['level']}: {s['xs']}x{s['ys']}, {s['arrows']} arrows, occupancy: {s['occupancy']:.1%}")

print("\nScanning New Levels for special silhouette shapes...")
new_shapes = analyze_shapes_in_set(new_dir, 'NewLevel', 1533)
print(f"New Levels with custom silhouette shapes: {len(new_shapes)}")
for s in new_shapes[:25]:
    print(f"  Level {s['level']}: {s['xs']}x{s['ys']}, {s['arrows']} arrows, occupancy: {s['occupancy']:.1%}")
