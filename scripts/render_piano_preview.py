import json
from PIL import Image, ImageDraw
from design_perfect_piano import build_grand_piano, XS, YS

arrows = build_grand_piano()

# Cell size in pixels
CELL_SIZE = 24
PADDING = 40
img_w = XS * CELL_SIZE + 2 * PADDING
img_h = YS * CELL_SIZE + 2 * PADDING

# Cream background like the game
img = Image.new("RGBA", (img_w, img_h), "#FAF6EE")
draw = ImageDraw.Draw(img)

# Arrow theme color: warm wooden brown / walnut piano color
COLOR_PIANO = "#5C4033" # dark walnut brown
COLOR_HEAD = "#3E2723"
COLOR_ACCENT = "#8D6E63"

# Draw grid dots
for cy in range(YS):
    for cx in range(XS):
        px = PADDING + cx * CELL_SIZE + CELL_SIZE // 2
        py = PADDING + cy * CELL_SIZE + CELL_SIZE // 2
        # draw.ellipse([px-1, py-1, px+1, py+1], fill="#E6DFD3")

for a in arrows:
    coords = a['canvas_coords']
    cdx, cdy = a['cdx'], a['cdy']
    
    # Draw arrow body
    pts = []
    for cx, cy in coords:
        px = PADDING + cx * CELL_SIZE + CELL_SIZE // 2
        py = PADDING + cy * CELL_SIZE + CELL_SIZE // 2
        pts.append((px, py))
        
    if len(pts) >= 2:
        draw.line(pts, fill=COLOR_PIANO, width=6)
    elif len(pts) == 1:
        draw.ellipse([pts[0][0]-3, pts[0][1]-3, pts[0][0]+3, pts[0][1]+3], fill=COLOR_PIANO)
        
    # Draw arrow head at pts[0]
    hx, hy = pts[0]
    head_len = 8
    if cdx == 1: # Right
        draw.polygon([(hx + head_len, hy), (hx - 2, hy - 5), (hx - 2, hy + 5)], fill=COLOR_HEAD)
    elif cdx == -1: # Left
        draw.polygon([(hx - head_len, hy), (hx + 2, hy - 5), (hx + 2, hy + 5)], fill=COLOR_HEAD)
    elif cdy == 1: # Down
        draw.polygon([(hx, hy + head_len), (hx - 5, hy - 2), (hx + 5, hy - 2)], fill=COLOR_HEAD)
    elif cdy == -1: # Up
        draw.polygon([(hx, hy - head_len), (hx - 5, hy + 2), (hx + 5, hy + 2)], fill=COLOR_HEAD)

img.save("rendered_grand_piano.png")
print("Saved rendered_grand_piano.png successfully!")
