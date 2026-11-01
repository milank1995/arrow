import os
from PIL import Image, ImageDraw

PROJECT_RES = r"C:\Projects\Personal\Games\arrow decode\arrow decode\arrow_native\app\src\main\res"
ARTIFACT_DIR = r"C:\Users\ADMIN\.gemini\antigravity\brain\3dd67974-6676-434e-8080-dbae7662d6dc"

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

def extract_clean_artwork(src_img):
    w, h = src_img.size
    fg = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    min_x, max_x, min_y, max_y = w, 0, h, 0

    for y in range(h):
        for x in range(w):
            r, g, b, a = src_img.getpixel((x, y))
            lum = (r + g + b) / 3.0
            if lum < 244:
                fg.putpixel((x, y), (r, g, b, 255))
                if x < min_x: min_x = x
                if x > max_x: max_x = x
                if y < min_y: min_y = y
                if y > max_y: max_y = y
            elif lum < 252:
                alpha = int((1.0 - (lum - 244.0) / 8.0) * 255.0)
                fg.putpixel((x, y), (r, g, b, max(0, min(255, alpha))))

    cropped = fg.crop((min_x, min_y, max_x + 1, max_y + 1))
    return cropped

def main():
    src_path = os.path.join(ARTIFACT_DIR, 'user_puzzle_logo_v2.png')
    src_img = Image.open(src_path).convert('RGBA')

    # 1. Extract pure artwork bounding box
    art = extract_clean_artwork(src_img)
    art_w, art_h = art.size
    print(f"Isolated clean artwork: {art_w}x{art_h} px")

    # 2. Google Play Store Master (512x512)
    # Safe zone: center 365px (71% of 512), safely inside Play Store squircle
    play_store = Image.new('RGBA', (512, 512), (255, 255, 255, 255))
    target_ps = 365
    scale_ps = target_ps / max(art_w, art_h)
    nw_ps = int(art_w * scale_ps)
    nh_ps = int(art_h * scale_ps)
    res_ps = art.resize((nw_ps, nh_ps), Image.Resampling.LANCZOS)
    play_store.paste(res_ps, ((512 - nw_ps) // 2, (512 - nh_ps) // 2), res_ps)
    ps_path = os.path.join(ARTIFACT_DIR, 'play_store_512.png')
    play_store.save(ps_path, 'PNG')
    print(f"Saved Play Store Master (512x512): {ps_path}")

    # 3. Adaptive Icon Foreground across all densities (108dp canvas base)
    # Safe zone: exactly 52% of canvas size to guarantee ZERO cropping on circular masks!
    for folder, size in FOREGROUND_DENSITIES.items():
        out_dir = os.path.join(PROJECT_RES, folder)
        os.makedirs(out_dir, exist_ok=True)

        canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        target_size = int(size * 0.52)
        s = target_size / max(art_w, art_h)
        nw = int(art_w * s)
        nh = int(art_h * s)
        res_art = art.resize((nw, nh), Image.Resampling.LANCZOS)
        px = (size - nw) // 2
        py = (size - nh) // 2
        canvas.paste(res_art, (px, py), res_art)

        out_file = os.path.join(out_dir, 'ic_launcher_foreground.png')
        canvas.save(out_file, 'PNG')
        print(f"  Adaptive Foreground ({size}x{size}): {out_file}")

    # 4. Legacy Launcher Icons (ic_launcher.png & ic_launcher_round.png)
    for folder, size in ICON_DENSITIES.items():
        out_dir = os.path.join(PROJECT_RES, folder)
        os.makedirs(out_dir, exist_ok=True)

        # Standard icon (white card, 72% artwork)
        sq_icon = Image.new('RGBA', (size, size), (255, 255, 255, 255))
        target_sq = int(size * 0.72)
        s_sq = target_sq / max(art_w, art_h)
        nw_sq = int(art_w * s_sq)
        nh_sq = int(art_h * s_sq)
        res_sq = art.resize((nw_sq, nh_sq), Image.Resampling.LANCZOS)
        sq_icon.paste(res_sq, ((size - nw_sq) // 2, (size - nh_sq) // 2), res_sq)
        sq_file = os.path.join(out_dir, 'ic_launcher.png')
        sq_icon.save(sq_file, 'PNG')

        # Round icon (white circle on transparent, 56% artwork so it NEVER touches circular edge)
        round_icon = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        d_r = ImageDraw.Draw(round_icon)
        d_r.ellipse([0, 0, size, size], fill=(255, 255, 255, 255))

        target_rd = int(size * 0.56)
        s_rd = target_rd / max(art_w, art_h)
        nw_rd = int(art_w * s_rd)
        nh_rd = int(art_h * s_rd)
        res_rd = art.resize((nw_rd, nh_rd), Image.Resampling.LANCZOS)
        round_icon.paste(res_rd, ((size - nw_rd) // 2, (size - nh_rd) // 2), res_rd)
        rd_file = os.path.join(out_dir, 'ic_launcher_round.png')
        round_icon.save(rd_file, 'PNG')

        print(f"  Legacy Icons ({size}x{size}): {sq_file} & {rd_file}")

    print("\nAll Android launcher icon densities successfully updated with zero-crop padding!")

if __name__ == '__main__':
    main()
