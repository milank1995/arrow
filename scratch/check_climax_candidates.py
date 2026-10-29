import sys
sys.stdout.reconfigure(encoding='utf-8')
sys.path.append('scratch')
from test_accurate_matching import large, shapes, unity, one_k

print("Large puzzle arrow counts:")
large_counts = sorted((len(l['Arrows']), l.get('ShapeName', '')) for l in large)
for a, n in large_counts:
    print(f"  {a} arrows: '{n}'")

print("\nShapes with 170-220 arrows:")
for s in shapes:
    a = len(s['Arrows'])
    n = s.get('ShapeName', '')
    if 170 <= a <= 220 and not n.startswith('Shape #'):
        print(f"  {a} arrows: '{n}' ({s['XSize']}x{s['YSize']})")

print("\nUnity normal levels with 170-220 arrows:")
unity_climaxes = [l for l in unity if 175 <= len(l['Arrows']) <= 215]
print(f"Total unity climax levels: {len(unity_climaxes)}")
for l in unity_climaxes[:5]:
    print(f"  {len(l['Arrows'])} arrows: {l['XSize']}x{l['YSize']}")
