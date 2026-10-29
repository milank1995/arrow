import json
from test_accurate_matching import one_k, unity, shapes

cands_unity = [l for l in unity if len(l['Arrows']) == 70]
print(f"Total 70-arrow levels in unity: {len(cands_unity)}")
for c in cands_unity[:10]:
    print(f"Unity 70 arr: {c['XSize']}x{c['YSize']}, arrows={len(c['Arrows'])}, hash={c.get('Hash')}")

cands_1k = [l for l in one_k if len(l['Arrows']) == 70]
print(f"Total 70-arrow levels in 1K: {len(cands_1k)}")
for c in cands_1k[:10]:
    print(f"1K 70 arr: {c['XSize']}x{c['YSize']}, arrows={len(c['Arrows'])}, name={c.get('ShapeName')}")
