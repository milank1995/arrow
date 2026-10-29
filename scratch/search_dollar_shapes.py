from test_accurate_matching import shapes, large

for s in shapes + large:
    name = s.get('ShapeName', '').lower()
    for kw in ['dollar', 'money', 'coin', 'currency', 'gold', 'treasure', 'bank']:
        if kw in name:
            print(f"Match for {kw}: '{s.get('ShapeName')}', arrows={len(s['Arrows'])}")

# What shapes have between 42 and 48 arrows?
print("\nAll named shapes with 40-50 arrows:")
for s in shapes + large:
    name = s.get('ShapeName', '')
    if 40 <= len(s['Arrows']) <= 50 and not name.startswith('Shape #'):
        print(f"'{name}': {len(s['Arrows'])} arrows")
