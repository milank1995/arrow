import sys
sys.stdout.reconfigure(encoding='utf-8')
from test_windmill_isolated import shapes

for s in shapes:
    name = s.get('ShapeName', '')
    for kw in ['沙', '漏', '钟', '表', '杯', '瓶', '时']:
        if kw in name:
            print(f"'{name}': {len(s['Arrows'])} arrows")
