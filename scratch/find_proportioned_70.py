from test_accurate_matching import unity, one_k

print("Candidate 70-arrow levels with clean proportions:")
for idx, l in enumerate(unity):
    if len(l['Arrows']) == 70 and l['XSize'] <= 16 and l['YSize'] <= 22:
        print(f"Unity idx {idx}: {l['XSize']}x{l['YSize']}, {len(l['Arrows'])} arrows")

for idx, l in enumerate(one_k):
    if 65 <= len(l['Arrows']) <= 75:
        print(f"1K idx {idx}: {l['XSize']}x{l['YSize']}, {len(l['Arrows'])} arrows, name={l.get('ShapeName')}")
