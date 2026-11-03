import os
import math
from PIL import Image, ImageDraw

ARTIFACT_DIR = r"C:\Users\ADMIN\.gemini\antigravity\brain\3dd67974-6676-434e-8080-dbae7662d6dc"
PROJECT_RES = r"C:\Projects\Personal\Games\arrow decode\arrow decode\arrow_native\app\src\main\res"
SOURCE_IMAGE = os.path.join(ARTIFACT_DIR, ".user_uploaded", "media_1793707277394_91df794e.png")

# Densities for legacy launcher icons (ic_launcher.png, ic_launcher_round.png)
ICON_DENSITIES = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192
}

# Densities for adaptive icon foreground (ic_launcher_foreground.png, 108dp base)
FOREGROUND_DENSITIES = {
    'mipmap-mdpi': 108,
    'mipmap-hdpi': 162,
    'mipmap-xhdpi': 216,
    'mipmap-xxhdpi': 324,
    'mipmap-xxxhdpi': 432
}

def extract_clean_arrows():
    """Extract the tactile arrows with smooth alpha shadows and absolutely zero corner black triangles."""
    orig = Image.open(SOURCE_IMAGE).convert('RGBA')
    w, h = orig.size
    clean = Image.new('RGBA', (w, h), (0, 0, 0, 0))

    for y in range(h):
        for x in range(w):
            r, g, b, a = orig.getpixel((x, y))
            # If outside card squircle, strictly transparent
            if a < 50:
                continue
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            if lum >= 248:
                # White background card
                continue
            elif lum >= 215:
                # Smooth shadow gradient tapering to transparent
                alpha = int(255 * (248 - lum) / (248 - 215))
                clean.putpixel((x, y), (r, g, b, alpha))
            else:
                # Solid arrow body and core tactile bevel
                clean.putpixel((x, y), (r, g, b, 255))

    bbox = clean.getbbox()
    cropped = clean.crop(bbox)
    clean_path = os.path.join(ARTIFACT_DIR, "tactile_arrows_v4_clean.png")
    cropped.save(clean_path, "PNG")
    print(f"Extracted clean arrows: {cropped.size} px saved to {clean_path}")
    return cropped

def generate_icons(cropped_arrows):
    c_w, c_h = cropped_arrows.size

    # 1. Google Play Store 512x512 Master Icon (and clean downloadable master)
    # Increased target size: 395px (77.1% of 512px) for a bold, prominent look on Play Store
    play_master = Image.new('RGBA', (512, 512), (255, 255, 255, 255))
    target_ps = 395
    s_ps = target_ps / max(c_w, c_h)
    nw_ps = int(c_w * s_ps)
    nh_ps = int(c_h * s_ps)
    res_ps = cropped_arrows.resize((nw_ps, nh_ps), Image.Resampling.LANCZOS)
    px_ps = (512 - nw_ps) // 2
    py_ps = (512 - nh_ps) // 2
    play_master.paste(res_ps, (px_ps, py_ps), res_ps)
    
    ps_path = os.path.join(ARTIFACT_DIR, "play_store_512.png")
    play_master.save(ps_path, "PNG")
    master_path = os.path.join(ARTIFACT_DIR, "clean_tactile_master_512.png")
    play_master.save(master_path, "PNG")
    print(f"Saved Play Store 512x512 Master: {ps_path}")

    # 2. Adaptive Icon Foregrounds (ic_launcher_foreground.png)
    # Scale: 50.0% of canvas base (e.g. 216px on 432px base), 0 cropped pixels guaranteed on all circular masks!
    for folder, size in FOREGROUND_DENSITIES.items():
        out_dir = os.path.join(PROJECT_RES, folder)
        os.makedirs(out_dir, exist_ok=True)

        fg_canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        target_size = int(size * 0.500)
        s = target_size / max(c_w, c_h)
        nw = int(c_w * s)
        nh = int(c_h * s)
        res_fg = cropped_arrows.resize((nw, nh), Image.Resampling.LANCZOS)
        px = (size - nw) // 2
        py = (size - nh) // 2
        fg_canvas.paste(res_fg, (px, py), res_fg)

        fg_file = os.path.join(out_dir, "ic_launcher_foreground.png")
        fg_canvas.save(fg_file, "PNG")
        print(f"Updated {folder}/ic_launcher_foreground.png ({size}x{size}, artwork {nw}x{nh})")

    # 3. Legacy icons (ic_launcher.png with rounded corners, ic_launcher_round.png circular)
    for folder, size in ICON_DENSITIES.items():
        out_dir = os.path.join(PROJECT_RES, folder)
        os.makedirs(out_dir, exist_ok=True)

        # Pre-composed square icon with soft rounded corners (20% squircle)
        sq_icon = Image.new('RGBA', (size, size), (255, 255, 255, 255))
        target_legacy = int(size * 0.77)
        s_leg = target_legacy / max(c_w, c_h)
        nw_leg = int(c_w * s_leg)
        nh_leg = int(c_h * s_leg)
        res_leg = cropped_arrows.resize((nw_leg, nh_leg), Image.Resampling.LANCZOS)
        px_leg = (size - nw_leg) // 2
        py_leg = (size - nh_leg) // 2
        sq_icon.paste(res_leg, (px_leg, py_leg), res_leg)

        # Apply natural rounded corners to square icon
        mask_sq = Image.new('L', (size, size), 0)
        draw_sq = ImageDraw.Draw(mask_sq)
        corner_r = int(size * 0.20)
        draw_sq.rounded_rectangle([(0, 0), (size, size)], radius=corner_r, fill=255)
        
        final_sq = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        final_sq.paste(sq_icon, (0, 0), mask_sq)
        sq_file = os.path.join(out_dir, "ic_launcher.png")
        final_sq.save(sq_file, "PNG")

        # Round icon (ic_launcher_round.png)
        # Scaled to fit comfortably inside the circle (70% scale)
        rd_icon = Image.new('RGBA', (size, size), (255, 255, 255, 255))
        target_rd = int(size * 0.70)
        s_rd = target_rd / max(c_w, c_h)
        nw_rd = int(c_w * s_rd)
        nh_rd = int(c_h * s_rd)
        res_rd = cropped_arrows.resize((nw_rd, nh_rd), Image.Resampling.LANCZOS)
        px_rd = (size - nw_rd) // 2
        py_rd = (size - nh_rd) // 2
        rd_icon.paste(res_rd, (px_rd, py_rd), res_rd)

        mask_rd = Image.new('L', (size, size), 0)
        draw_rd = ImageDraw.Draw(mask_rd)
        draw_rd.ellipse([(0, 0), (size, size)], fill=255)

        final_rd = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        final_rd.paste(rd_icon, (0, 0), mask_rd)
        rd_file = os.path.join(out_dir, "ic_launcher_round.png")
        final_rd.save(rd_file, "PNG")

        print(f"Updated {folder}/ic_launcher.png & ic_launcher_round.png ({size}x{size})")

def verify_and_generate_previews(cropped_arrows):
    """Generate high-resolution device simulation previews (Circle, Squircle) and verify zero cropping."""
    c_w, c_h = cropped_arrows.size
    canvas_size = 432
    center = 216.0
    circle_radius = 144.0

    target_size = int(canvas_size * 0.500)
    s = target_size / max(c_w, c_h)
    nw = int(c_w * s)
    nh = int(c_h * s)
    res_fg = cropped_arrows.resize((nw, nh), Image.Resampling.LANCZOS)
    px = (canvas_size - nw) // 2
    py = (canvas_size - nh) // 2

    # Verify zero crop on circle
    cropped_px = 0
    max_dist = 0.0
    for y in range(nh):
        for x in range(nw):
            if res_fg.getpixel((x, y))[3] > 20:
                dist = math.hypot(px + x - center, py + y - center)
                if dist > max_dist:
                    max_dist = dist
                if dist >= circle_radius:
                    cropped_px += 1

    margin = circle_radius - max_dist
    print(f"\n--- VERIFICATION RESULTS ---")
    print(f"Canvas: {canvas_size}x{canvas_size} px (108dp base)")
    print(f"Arrows dimension: {nw}x{nh} px (50% scale, enlarged from 46.5%)")
    print(f"Circular mask radius: {circle_radius} px (72dp diameter)")
    print(f"Furthest arrow pixel from center: {max_dist:.1f} px")
    print(f"Breathing margin to mask edge: {margin:.1f} px ({margin/4.0:.1f} dp)")
    print(f"Cropped pixels under circular mask: {cropped_px} px -> {'100% ZERO CROP' if cropped_px == 0 else 'WARNING'}")

    # Render Circle Preview (512x512)
    circle_preview = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    # Full composite: pure white background + enlarged foreground
    comp = Image.new('RGBA', (canvas_size, canvas_size), (255, 255, 255, 255))
    comp.paste(res_fg, (px, py), res_fg)

    # Crop to circle mask of 288x288 px
    mask_c = Image.new('L', (canvas_size, canvas_size), 0)
    draw_c = ImageDraw.Draw(mask_c)
    draw_c.ellipse([(center - circle_radius, center - circle_radius), 
                    (center + circle_radius, center + circle_radius)], fill=255)
    
    masked_c = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
    masked_c.paste(comp, (0, 0), mask_c)
    cropped_c = masked_c.crop((int(center - circle_radius), int(center - circle_radius),
                               int(center + circle_radius), int(center + circle_radius)))
    res_c_512 = cropped_c.resize((480, 480), Image.Resampling.LANCZOS)
    circle_preview.paste(res_c_512, (16, 16), res_c_512)
    c_preview_path = os.path.join(ARTIFACT_DIR, "tactile_preview_circle_v4.png")
    circle_preview.save(c_preview_path, "PNG")

    # Render Squircle Preview (512x512)
    squircle_preview = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    mask_sq = Image.new('L', (canvas_size, canvas_size), 0)
    draw_sq = ImageDraw.Draw(mask_sq)
    draw_sq.rounded_rectangle([(center - circle_radius, center - circle_radius), 
                               (center + circle_radius, center + circle_radius)], 
                              radius=int(circle_radius * 0.56), fill=255)
    masked_sq = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
    masked_sq.paste(comp, (0, 0), mask_sq)
    cropped_sq = masked_sq.crop((int(center - circle_radius), int(center - circle_radius),
                                 int(center + circle_radius), int(center + circle_radius)))
    res_sq_512 = cropped_sq.resize((480, 480), Image.Resampling.LANCZOS)
    squircle_preview.paste(res_sq_512, (16, 16), res_sq_512)
    sq_preview_path = os.path.join(ARTIFACT_DIR, "tactile_preview_squircle_v4.png")
    squircle_preview.save(sq_preview_path, "PNG")
    print(f"Generated device mask previews: {c_preview_path} & {sq_preview_path}")

if __name__ == '__main__':
    arrows = extract_clean_arrows()
    generate_icons(arrows)
    verify_and_generate_previews(arrows)
