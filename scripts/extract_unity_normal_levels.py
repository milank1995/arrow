import os
import gzip
import json
import struct
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

    # 1. GuideLevels17 for introductory levels 1-20
    guide_hashes = []
    for obj in env.objects:
        if obj.type.name == 'TextAsset':
            data = obj.read()
            if getattr(data, 'm_Name', '') == 'GuideLevels17':
                b = data.m_Script.encode('utf-8', 'surrogateescape')
                guide_hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
                break

    print(f"GuideLevels17 has {len(guide_hashes)} levels.")

    # 2. LevelContentSequence2701 for difficulty progression
    lcs_rows = []
    for obj in env.objects:
        if obj.type.name == 'TextAsset':
            data = obj.read()
            if getattr(data, 'm_Name', '') == 'LevelContentSequence2701':
                b = data.m_Script.encode('utf-8', 'surrogateescape')
                s_len = struct.unpack('<I', b[:4])[0]
                count, size = struct.unpack('<II', b[4+s_len:4+s_len+8])
                rem = b[4+s_len+8:]
                rec_len = size // count
                for r in range(count):
                    rec = rem[r*rec_len:(r+1)*rec_len]
                    row_id = struct.unpack('<I', rec[0:4])[0]
                    diff = struct.unpack('<I', rec[4:8])[0]
                    has_pic = struct.unpack('<I', rec[8:12])[0]
                    lcs_rows.append({'id': row_id, 'diff': diff, 'has_pic': has_pic})
                break

    print(f"LevelContentSequence2701 has {len(lcs_rows)} rows.")

    # 3. LevelAttr12 pool entries
    level_attr = []
    for obj in env.objects:
        if obj.type.name == 'TextAsset':
            data = obj.read()
            if getattr(data, 'm_Name', '') == 'LevelAttr12':
                b = data.m_Script.encode('utf-8', 'surrogateescape')
                s_len = struct.unpack('<I', b[:4])[0]
                count, size = struct.unpack('<II', b[4+s_len:4+s_len+8])
                rem = b[4+s_len+8:]
                rec_len = size // count
                for r in range(count):
                    rec = rem[r*rec_len:(r+1)*rec_len]
                    row_id = struct.unpack('<I', rec[0:4])[0]
                    h = rec[8:24].decode('ascii')
                    diff = struct.unpack('<I', rec[24:28])[0]
                    arr_cnt = struct.unpack('<I', rec[28:32])[0]
                    pic = struct.unpack('<I', rec[44:48])[0]
                    level_attr.append({
                        'id': row_id,
                        'hash': h,
                        'diff': diff,
                        'arr_cnt': arr_cnt,
                        'pic': pic
                    })
                break

    print(f"LevelAttr12 has {len(level_attr)} entries.")

    # Load level pack env to retrieve hashes & raw level json
    print("Loading level pack...")
    with open(packs_path, 'rb') as f:
        f.seek(32)
        env_pack = UnityPy.load(f.read())

    raw_pack_objects = {}
    for obj in env_pack.objects:
        if obj.type.name == 'TextAsset':
            data = obj.read()
            name = getattr(data, 'm_Name', '')
            raw_pack_objects[name] = data

    pack_hashes = set(raw_pack_objects.keys())
    print(f"levelpacks_newuser contains {len(pack_hashes)} level assets.")

    # Build sequence
    sequence = []
    seen = set()

    # Levels 1 to 20: GuideLevels17
    for idx, h in enumerate(guide_hashes[:20], 1):
        if h in pack_hashes:
            seen.add(h)
            sequence.append({
                'index': idx,
                'hash': h,
                'difficulty': 1 if idx <= 2 else (3 if idx == 3 else (4 if idx == 5 else (5 if idx == 10 or idx == 20 else 2))),
                'category': 'Intro' if idx <= 4 else ('Milestone' if idx in [5, 10, 15, 20] else 'Normal'),
                'is_guide': True
            })

    # Pools grouped by difficulty and picture
    diff_pools = {d: [x for x in level_attr if x['diff'] == d and x['hash'] in pack_hashes] for d in range(1, 6)}
    pic_pools = {d: [x for x in diff_pools[d] if x['pic'] > 0] for d in range(1, 6)}

    ptrs = {d: 0 for d in range(1, 6)}
    pic_ptrs = {d: 0 for d in range(1, 6)}

    category_names = {
        1: 'Normal',
        2: 'Normal',
        3: 'Hard',
        4: 'SuperHard',
        5: 'Nightmare'
    }

    # Generate from Level 21 up to max_levels
    current_lvl = len(sequence) + 1
    while current_lvl <= max_levels:
        seq_idx = (current_lvl - 1) % len(lcs_rows)
        seq_row = lcs_rows[seq_idx]
        diff = seq_row['diff']
        has_pic = seq_row['has_pic']

        selected = None
        if has_pic and pic_pools[diff]:
            while pic_ptrs[diff] < len(pic_pools[diff]):
                candidate = pic_pools[diff][pic_ptrs[diff]]
                pic_ptrs[diff] += 1
                if candidate['hash'] not in seen:
                    selected = candidate
                    break

        if not selected:
            while ptrs[diff] < len(diff_pools[diff]):
                candidate = diff_pools[diff][ptrs[diff]]
                ptrs[diff] += 1
                if candidate['hash'] not in seen:
                    selected = candidate
                    break

        if not selected:
            for alt_d in sorted(range(1, 6), key=lambda x: abs(x - diff)):
                while ptrs[alt_d] < len(diff_pools[alt_d]):
                    candidate = diff_pools[alt_d][ptrs[alt_d]]
                    ptrs[alt_d] += 1
                    if candidate['hash'] not in seen:
                        selected = candidate
                        break
                if selected:
                    break

        if not selected:
            print(f"Warning: Pool exhausted at level {current_lvl}!")
            break

        seen.add(selected['hash'])
        cat = 'Milestone' if diff in [4, 5] else category_names.get(diff, 'Normal')
        sequence.append({
            'index': current_lvl,
            'hash': selected['hash'],
            'difficulty': diff,
            'category': cat,
            'is_guide': False,
            'pic': selected['pic'],
            'expected_arrows': selected['arr_cnt']
        })
        current_lvl += 1

    print(f"Sequencer created {len(sequence)} authentic levels.")

    # Convert and write each level
    manifest = []
    print("Writing level files to assets...")
    for item in sequence:
        idx = item['index']
        h = item['hash']
        cat = item['category']
        diff = item['difficulty']

        raw_obj = raw_pack_objects.get(h)
        if not raw_obj:
            print(f"Error: hash {h} not found in pack!")
            continue

        raw = raw_obj.m_Script.encode('utf-8', 'surrogateescape')
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

            # Expand points step-by-step
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

        out_level = {
            "LevelIndex": idx,
            "OriginalHash": h,
            "Category": cat,
            "Difficulty": diff,
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
            "category": cat,
            "difficulty": diff,
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
