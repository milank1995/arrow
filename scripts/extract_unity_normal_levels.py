import os
import gzip
import json
import re
import UnityPy

def extract_normal_levels(output_dir, max_levels=1533):
    os.makedirs(output_dir, exist_ok=True)
    
    UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
    config_bundle_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
    packs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab'

    print("Loading level configs bundle...")
    with open(config_bundle_path, 'rb') as f:
        f.seek(32)
        env = UnityPy.load(f.read())

    def get_hashes(config_name):
        for obj in env.objects:
            if obj.type.name == 'TextAsset':
                data = obj.read()
                if getattr(data, 'm_Name', '') == config_name:
                    b = data.m_Script.encode('utf-8', 'surrogateescape')
                    return [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
        return []

    # 1. GuideLevels1: Handcrafted progression for levels 1 to 40 (Level 25 is authentic SuperHard 35ad40df6826a14f)
    h_guide1 = get_hashes('GuideLevels1')
    print(f"GuideLevels1 has {len(h_guide1)} levels.")

    # 2. Sequential Normal Priority Pools for levels 41 to 1533+
    h_prio0 = get_hashes('NormalLevelsPriority')
    h_prio1 = get_hashes('NormalLevelsPriority1')
    h_prio2 = get_hashes('NormalLevelsPriority2')
    h_prio3 = get_hashes('NormalLevelsPriority3')

    print(f"NormalLevelsPriority: {len(h_prio0)}, Prio1: {len(h_prio1)}, Prio2: {len(h_prio2)}, Prio3: {len(h_prio3)}")

    all_sequence = []
    seen = set()
    pools = [
        ('Guide', h_guide1),
        ('Normal', h_prio0),
        ('Normal', h_prio1),
        ('Normal', h_prio2),
        ('Normal', h_prio3),
    ]

    for cat, hashes in pools:
        for h in hashes:
            if h not in seen:
                seen.add(h)
                all_sequence.append((cat, h))
                if len(all_sequence) >= max_levels:
                    break
        if len(all_sequence) >= max_levels:
            break

    print(f"Targeting {len(all_sequence)} sequential normal levels.")
    target_hashes = set(h for _, h in all_sequence)

    print("Loading level assets pack...")
    with open(packs_path, 'rb') as f:
        f.seek(32)
        env_pack = UnityPy.load(f.read())

    raw_levels = {}
    for obj in env_pack.objects:
        if obj.type.name == 'TextAsset':
            data = obj.read()
            name = getattr(data, 'm_Name', '')
            if name in target_hashes:
                raw_levels[name] = data.m_Script.encode('utf-8', 'surrogateescape')

    manifest = []
    print("Converting and writing level JSONs...")
    for idx, (cat, h) in enumerate(all_sequence, 1):
        raw = raw_levels.get(h)
        if not raw:
            print(f"Warning: Hash {h} not found in pack!")
            continue

        raw_level = json.loads(gzip.decompress(raw))
        xs = raw_level['XSize']
        ys = raw_level['YSize']
        arrows_out = []

        for a in raw_level.get('Arrows', []):
            pts = a.get('Points', [])
            if not pts or len(pts) < 2:
                continue

            head_idx = pts[0]
            next_idx = pts[1]
            hx, hy = head_idx % xs, head_idx // xs
            nx, ny = next_idx % xs, next_idx // xs

            dx = 1 if hx > nx else (-1 if hx < nx else 0)
            dy = 1 if hy > ny else (-1 if hy < ny else 0)

            # Expand indices step by step
            indices = []
            for k in range(len(pts) - 1):
                p1 = pts[k]
                p2 = pts[k + 1]
                x1, y1 = p1 % xs, p1 // xs
                x2, y2 = p2 % xs, p2 // xs
                step_x = 1 if x2 > x1 else (-1 if x2 < x1 else 0)
                step_y = 1 if y2 > y1 else (-1 if y2 < y1 else 0)

                cx, cy = x1, y1
                while (cx != x2) or (cy != y2):
                    indices.append(cy * xs + cx)
                    cx += step_x
                    cy += step_y

            tail = pts[-1]
            indices.append((tail // xs) * xs + (tail % xs))

            bend_count = max(0, len(pts) - 2)
            arrow_dict = {
                "Dx": dx,
                "Dy": dy,
                "X": hx,
                "Y": hy,
                "Indices": indices,
                "BendCount": bend_count
            }

            if "ColorIndex" in a:
                arrow_dict["ColorIndex"] = a["ColorIndex"]
            if "ColorHex" in a:
                arrow_dict["ColorHex"] = a["ColorHex"]

            arrows_out.append(arrow_dict)

        # Category and Milestone tag
        is_milestone = (idx % 5 == 0)
        level_category = 'Milestone' if is_milestone else ('Intro' if idx <= 4 else 'Normal')

        out_level = {
            "LevelIndex": idx,
            "OriginalHash": h,
            "Category": level_category,
            "XSize": xs,
            "YSize": ys,
            "MaxHearts": 3,
            "Arrows": arrows_out
        }

        file_name = f"NewLevel_{idx}.json"
        file_path = os.path.join(output_dir, file_name)
        with open(file_path, 'w', encoding='utf-8') as f_out:
            json.dump(out_level, f_out, separators=(',', ':'))

        manifest.append({
            "index": idx,
            "category": level_category,
            "xSize": xs,
            "ySize": ys,
            "arrowCount": len(arrows_out),
            "originalHash": h
        })

    manifest_path = os.path.join(output_dir, "manifest.json")
    with open(manifest_path, 'w', encoding='utf-8') as f_man:
        json.dump(manifest, f_man, indent=2)

    print(f"Extraction complete! {len(manifest)} levels exported to {output_dir}")

if __name__ == "__main__":
    out = r"arrow_native\app\src\main\assets\new_levels"
    extract_normal_levels(out, max_levels=1533)
