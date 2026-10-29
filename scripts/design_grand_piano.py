import sys

# Let's create an ASCII blueprint of an unmistakable Grand Piano (Isometric 3D / Perspective)
# Dimensions around 38 wide by 42 high or similar.

def print_grid(grid):
    for r in grid:
        print("".join(r))

# Let's test a 40x44 grid
W, H = 42, 46
canvas = [["." for _ in range(W)] for _ in range(H)]

# Let's sketch:
# 1. Open Lid (angled up from left rim to top right):
#    From (x=6, y=14) sloping up to (x=32, y=3)
# 2. Lid Prop Stick:
#    From piano body (x=24, y=18) up to lid (x=25, y=9)
# 3. Soundboard / Harps:
#    Inside the cavity between lid and body
# 4. Piano Body Rim:
#    Left side curve, back curve, straight right side
# 5. Music Stand & Sheet:
#    x=14..26, y=20..24
# 6. Keyboard:
#    x=8..34, y=27..32
# 7. Legs & Pedals:
#    Left leg: x=9..11, y=33..43
#    Right leg: x=31..33, y=33..43
#    Back leg: x=19..21, y=22..30
#    Pedals: x=20..22, y=37..43
# 8. Piano Bench:
#    x=15..27, y=38..44

print(f"Canvas size: {W}x{H}")
