from test_accurate_matching import shapes, large

keywords = ['hourglass', 'glass', 'sand', 'clock', 'time', '漏斗', '沙漏', '钟']
print("Searching for hourglass or clock shapes:")
for s in shapes + large:
    name = s.get('ShapeName', '')
    if any(k in name.lower() for k in keywords):
        print(f"Match: '{name}', arrows={len(s['Arrows'])}, size={s['XSize']}x{s['YSize']}")
