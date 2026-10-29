import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('scratch/all_shape_names.txt', encoding='utf-8') as f:
    shapes = [l.strip() for l in f if l.strip()]

real_shapes = [s for s in shapes if 'Shape #' not in s]
print(f"Total real named shapes: {len(real_shapes)}")

# Sort by arrow count
def get_arrows(line):
    # e.g. "Lvl 3: Palm Tree (23 arrows)"
    try:
        return int(line.split('(')[-1].split()[0])
    except:
        return 0

real_shapes.sort(key=get_arrows)

# Group by arrow ranges:
# 20-30, 30-40, 40-50, 50-60, 60-70, 70-80, 80-90, 90-100, 100-130, 130+
ranges = [(20, 30), (30, 40), (40, 50), (50, 60), (60, 70), (70, 80), (80, 90), (90, 100), (100, 130), (130, 200)]

for r_min, r_max in ranges:
    in_range = [s for s in real_shapes if r_min <= get_arrows(s) < r_max]
    print(f"\n--- Range {r_min}-{r_max} arrows ({len(in_range)} shapes) ---")
    for s in in_range[:15]:
        print("  " + s)
