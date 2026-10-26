import UnityPy
import re
import json

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'
configs_path = r'C:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelconfigs_d4dc0c9ff6c741d4efc9d9025ef15652.ab'
with open(configs_path, 'rb') as f:
    f.seek(32)
    env = UnityPy.load(f.read())

tables = {}
for obj in env.objects:
    if obj.type.name == 'TextAsset':
        d = obj.read()
        name = getattr(d, 'm_Name', '')
        b = d.m_Script.encode('utf-8', 'surrogateescape')
        hashes = [h.decode('ascii') for h in re.findall(rb'[a-f0-9]{16}', b)]
        if hashes:
            tables[name] = hashes

print(f"Total tables with hashes: {len(tables)}")

# Check how tables are grouped
prefixes = {}
for name in tables:
    prefix = re.sub(r'\d+$', '', name)
    prefixes[prefix] = prefixes.get(prefix, 0) + 1

print("\nTable prefixes:")
for p, c in sorted(prefixes.items(), key=lambda x: -x[1])[:20]:
    print(f"  {p:30s}: {c} tables")

# Load catalog to check which hashes exist in the levelpack
with open('scripts/unity_normal_levels_catalog.json', 'r') as fp:
    catalog = json.load(fp)
pack_hashes = set(x['hash'] for x in catalog)
print(f"\nTotal available pack hashes: {len(pack_hashes)}")

# Trace the progression sequence following Unity's official table ordering
ordered_hashes = []
seen = set()

# 1. GuideLevels1 (Tutorial Levels 1 to 40)
for h in tables.get('GuideLevels1', []):
    if h in pack_hashes and h not in seen:
        seen.add(h)
        ordered_hashes.append(('GuideLevels1', h))

print(f"After GuideLevels1: {len(ordered_hashes)} levels")

# 2. Priority Tables (NormalLevelsPriority, Priority1, Priority2, Priority3...)
for ptab in ['NormalLevelsPriority', 'NormalLevelsPriority1', 'NormalLevelsPriority2', 'NormalLevelsPriority3']:
    for h in tables.get(ptab, []):
        if h in pack_hashes and h not in seen:
            seen.add(h)
            ordered_hashes.append((ptab, h))

print(f"After NormalPriority 0-3: {len(ordered_hashes)} levels")

# 3. Numbered NormalLevels1 to NormalLevelsN
numbered_normal = [k for k in tables if re.match(r'^NormalLevels\d+$', k)]
numbered_normal.sort(key=lambda x: int(re.findall(r'\d+', x)[0]))
for t in numbered_normal:
    for h in tables.get(t, []):
        if h in pack_hashes and h not in seen:
            seen.add(h)
            ordered_hashes.append((t, h))

print(f"After Numbered NormalLevels ({len(numbered_normal)} tables): {len(ordered_hashes)} levels")

# 4. Shape and Milestone Normal tables
shape_normal = [k for k in tables if 'shape' in k.lower() and not any(m in k.lower() for m in ['daily', 'challenge', 'clash', 'cat'])]
for t in shape_normal:
    for h in tables.get(t, []):
        if h in pack_hashes and h not in seen:
            seen.add(h)
            ordered_hashes.append((t, h))

print(f"After Shape Normal tables ({len(shape_normal)} tables): {len(ordered_hashes)} levels")

# 5. Remaining Normal tables
for t, hlist in tables.items():
    if not any(m in t.lower() for m in ['daily', 'challenge', 'cat', 'clash', 'octa', 'oneline', 'radar', 'pinata', 'streak', 'tarot', 'chess', 'god', 'gallery', 'trophy']):
        for h in hlist:
            if h in pack_hashes and h not in seen:
                seen.add(h)
                ordered_hashes.append((t, h))

print(f"After all normal tables: {len(ordered_hashes)} levels")

# 6. Any remaining levels in pack
remaining_in_pack = pack_hashes - seen
print(f"Remaining in pack not in any normal table: {len(remaining_in_pack)}")
for h in remaining_in_pack:
    seen.add(h)
    ordered_hashes.append(('PackPool', h))

print(f"Total deduplicated sequence length: {len(ordered_hashes)} levels!")
