import json
from PIL import Image, ImageDraw
from collections import defaultdict

def render_arrows_to_image(arrows, xs, ys, out_filename, title=""):
    CELL_SIZE = 20
    PADDING = 40
    img_w = xs * CELL_SIZE + 2 * PADDING
    img_h = ys * CELL_SIZE + 2 * PADDING
    img = Image.new("RGBA", (img_w, img_h), "#FAF6EE")
    draw = ImageDraw.Draw(img)
    
    COLOR_BODY = "#5C4033"
    COLOR_HEAD = "#3E2723"
    
    for a in arrows:
        coords = a['canvas_coords']
        cdx, cdy = a['cdx'], a['cdy']
        pts = [(PADDING + cx * CELL_SIZE + CELL_SIZE // 2, PADDING + cy * CELL_SIZE + CELL_SIZE // 2) for cx, cy in coords]
        if len(pts) >= 2:
            draw.line(pts, fill=COLOR_BODY, width=5)
        elif len(pts) == 1:
            draw.ellipse([pts[0][0]-3, pts[0][1]-3, pts[0][0]+3, pts[0][1]+3], fill=COLOR_BODY)
            
        hx, hy = pts[0]
        hl = 7
        if cdx == 1:
            draw.polygon([(hx + hl, hy), (hx - 2, hy - 4), (hx - 2, hy + 4)], fill=COLOR_HEAD)
        elif cdx == -1:
            draw.polygon([(hx - hl, hy), (hx + 2, hy - 4), (hx + 2, hy + 4)], fill=COLOR_HEAD)
        elif cdy == 1:
            draw.polygon([(hx, hy + hl), (hx - 4, hy - 2), (hx + 4, hy - 2)], fill=COLOR_HEAD)
        elif cdy == -1:
            draw.polygon([(hx, hy - hl), (hx - 4, hy + 2), (hx + 4, hy + 2)], fill=COLOR_HEAD)
            
    img.save(out_filename)
    print(f"Saved {out_filename}")

print("Renderer ready.")
