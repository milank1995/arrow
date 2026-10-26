import json, gzip, os, glob
from collections import defaultdict

# Let's inspect the transition points across milestones:
# Level 10 -> 11
# Level 20 -> 21
# Level 30 -> 31
# Level 40 -> 41
# Level 50 -> 51
# Level 100 -> 101
# Level 200 -> 201
# Level 500 -> 501
# Level 1000 -> 1001

def calculate_tier_floor(lvl):
    if lvl <= 4:
        return 4
    elif lvl <= 10:
        return 12
    elif lvl <= 20:
        return 18
    elif lvl <= 30:
        return 22
    elif lvl <= 50:
        return 30
    elif lvl <= 100:
        return 38
    elif lvl <= 250:
        return 45
    elif lvl <= 500:
        return 52
    elif lvl <= 1000:
        return 60
    elif lvl <= 1500:
        return 70
    else:
        return 80

print("=== Tier Floors Across the 2,000 Level Journey ===")
for test_lvl in [1, 5, 10, 11, 20, 21, 30, 31, 50, 51, 100, 101, 250, 251, 500, 501, 1000, 1001, 1500, 1501, 2000]:
    fl = calculate_tier_floor(test_lvl)
    print(f"Level {test_lvl:4d}: Hard Floor = {fl:2d} arrows (No level at or after L{test_lvl} can ever drop below {fl} arrows)")
