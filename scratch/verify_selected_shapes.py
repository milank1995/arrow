import sys
sys.stdout.reconfigure(encoding='utf-8')
from test_accurate_matching import shapes, large

selected_shapes = [
    (5, "Palm Tree", 23),
    (10, "Robot", 52),
    (15, "Panda", 81),
    (20, "Swan", 99),
    (25, "Strawberry", 130),
    (28, "Black Cat", 57),
    (33, "Rainbow", 80),
    (38, "Jellyfish", 44),
    (45, "Squirrel", 62),
    (49, "Cactus", 130),
    (53, "Flower Vase", 46), # Hourglass shape!
    (58, "Magnolia", 63),
    (63, "Goldfish", 70),
    (68, "Trophy", 77),
    (73, "Windmill", 60),
    (78, "Coral", 72),      # Shape for 76-80!
    (83, "Sparrow", 80),
    (88, "Dog", 86),
    (93, "Seal Balancing Ball", 80),
    (97, "Rabbit1", 91),
    (100, "Mega Grid", 253),
    (104, "Castle", 102),
    (109, "Winterbird2", 110),
    (113, "Tropical Waterfall Elephant", 115),
    (118, "Honeybee", 82),
    (123, "Greetstar", 60),
    (128, "Sunflower", 67),
    (133, "Spin", 56),
    (138, "Whale", 136),
    (143, "Catstarv7", 126),
    (147, "Lion", 141),
    (150, "Sofa", 261)
]

print("Verifying selected shapes:")
for lvl, name, target_arr in selected_shapes:
    matches = [s for s in shapes + large if s.get('ShapeName', '').lower() == name.lower() or name.lower() in s.get('ShapeName', '').lower()]
    if matches:
        matches.sort(key=lambda s: abs(len(s['Arrows']) - target_arr))
        best = matches[0]
        print(f"L{lvl:3d}: '{name}' -> Found '{best.get('ShapeName')}' ({len(best['Arrows'])} arrows, {best['XSize']}x{best['YSize']})")
    else:
        print(f"L{lvl:3d}: '{name}' -> NOT FOUND!")
