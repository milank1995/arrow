import json, gzip, os, glob
from collections import defaultdict

# Let's inspect the shapes from our solvable unique pool
with open('scripts/unity_normal_levels_catalog.json', 'r') as f:
    catalog = json.load(f)

# Also check our scratch extracted levels
print(f"Total catalog entries: {len(catalog)}")
shape_entries = [x for x in catalog if x.get('isShape')]
print(f"Catalog shape entries: {len(shape_entries)}")

# Group shapes by arrow counts
brackets = defaultdict(list)
for s in shape_entries:
    narr = s['arrowCount']
    b = (narr // 10) * 10
    brackets[b].append(s)

print("\nShape distribution by arrow brackets:")
for b in sorted(brackets):
    print(f"  {b:3d} - {b+9:3d} arrows: {len(brackets[b]):4d} shapes")
