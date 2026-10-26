import os
import json

def is_solvable(level_data):
    xs = level_data.get('XSize') or level_data.get('xSize')
    ys = level_data.get('YSize') or level_data.get('ySize')
    raw_arrows = level_data.get('Arrows') or level_data.get('arrows', [])
    if not raw_arrows:
        return False, "Empty arrows"
    
    # Parse arrows
    arrows = []
    for i, a in enumerate(raw_arrows):
        hx = a.get('X') if 'X' in a else a.get('x')
        hy = a.get('Y') if 'Y' in a else a.get('y')
        dx = a.get('Dx') if 'Dx' in a else a.get('dx')
        dy = a.get('Dy') if 'Dy' in a else a.get('dy')
        indices = set(a.get('Indices') or a.get('indices', []))
        arrows.append({
            'id': i,
            'hx': hx,
            'hy': hy,
            'dx': dx,
            'dy': dy,
            'indices': indices
        })
    
    # Board occupancy: cell_index -> arrow_id
    board = {}
    for a in arrows:
        for idx in a['indices']:
            board[idx] = a['id']
            
    active_arrows = set(range(len(arrows)))
    
    # Greedy simulation loop
    while active_arrows:
        cleared_any = False
        for aid in list(active_arrows):
            a = arrows[aid]
            # Check ray from head outwards in direction (dx, dy)
            cx = a['hx'] + a['dx']
            cy = a['hy'] + a['dy']
            blocked = False
            while 0 <= cx < xs and 0 <= cy < ys:
                idx = cy * xs + cx
                # Blocked if another active arrow occupies this cell
                other_id = board.get(idx)
                if other_id is not None and other_id != aid and other_id in active_arrows:
                    blocked = True
                    break
                cx += a['dx']
                cy += a['dy']
            
            if not blocked:
                # Arrow can escape!
                active_arrows.remove(aid)
                cleared_any = True
                break # Re-evaluate remaining arrows
                
        if not cleared_any:
            # Deadlock: cannot clear remaining arrows
            return False, f"Deadlock with {len(active_arrows)} remaining"
            
    return True, "Solvable"

# Test on 100 sample levels from old and new
old_dir = 'arrow_native/app/src/main/assets/levels'
new_dir = 'arrow_native/app/src/main/assets/new_levels'

solvable_count = 0
unsolvable = []
total_tested = 0

for dpath in [new_dir, old_dir]:
    for f in os.listdir(dpath):
        if f.startswith('NewLevel_') or f.startswith('Level_'):
            total_tested += 1
            with open(os.path.join(dpath, f)) as fp:
                lvl = json.load(fp)
                ok, reason = is_solvable(lvl)
                if ok:
                    solvable_count += 1
                else:
                    unsolvable.append((f, reason))
            if total_tested >= 500:
                break
    if total_tested >= 500:
        break

print(f"Tested {total_tested} levels: {solvable_count} are 100% solvable! Unsolvable: {len(unsolvable)}")
if unsolvable:
    print("Unsolvable samples:", unsolvable[:5])
