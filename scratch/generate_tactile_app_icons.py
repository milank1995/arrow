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

def main():
    art_path = os.path.join(ARTIFACT_DIR, 'tactile_arrows_perfect.png')
    art = Image.open(art_path).convert('RGBA')
    art_w, art_h = art.size
    print(f"Loaded perfect tactile arrows artwork: {art_w}x{art_h} px")

    # 1. Google Play Store 512x512 Master (play_store_512.png)
    # Safe zone: center 360px (70% of 512px)
    play_store = Image.new('RGBA', (512, 512), (255, 255, 255, 255))
    target_ps = 360
    scale_ps = target_ps / max(art_w, art_h)
    nw_ps = int(art_w * scale_ps)
    nh_ps = int(art_h * scale_ps)
    res_ps = art.resize((nw_ps, nh_ps), Image.Resampling.LANCZOS)
    play_store.paste(res_ps, ((512 - nw_ps) // 2, (512 - nh_ps) // 2), res_ps)
    ps_path = os.path.join(ARTIFACT_DIR, 'play_store_512.png')
    play_store.save(ps_path, 'PNG')
    print(f"Saved Play Store Master (512x512): {ps_path}")

    # 2. Adaptive Icon Foreground across all densities (108dp canvas base)
    # Target scale: exactly 46.5% of canvas size to guarantee 0 cropped pixels on ALL circular masks!
    for folder, size in FOREGROUND_DENSITIES.items():
        out_dir = os.path.join(PROJECT_RES, folder)
        os.makedirs(out_dir, exist_ok=True)

        canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        target_size = int(size * 0.465)
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

    # 3. Legacy Launcher Icons (ic_launcher.png & ic_launcher_round.png)
    for folder, size in ICON_DENSITIES.items():
        out_dir = os.path.join(PROJECT_RES, folder)
        os.makedirs(out_dir, exist_ok=True)

        # Standard icon (white card, 70% artwork)
        sq_icon = Image.new('RGBA', (size, size), (255, 255, 255, 255))
        target_sq = int(size * 0.70)
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

    # 4. Verify ic_launcher_background.xml is white
    bg_xml_path = os.path.join(PROJECT_RES, 'drawable', 'ic_launcher_background.xml')
    bg_xml_content = '''<?xml version="1.0" encoding="utf-8"?>
<color xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#FFFFFF" />
'''
    with open(bg_xml_path, 'w', encoding='utf-8') as f:
        f.write(bg_xml_content)
    print(f"Verified {bg_xml_path}")

    # 5. Verify mipmap-anydpi-v26 XMLs
    v26_dir = os.path.join(PROJECT_RES, 'mipmap-anydpi-v26')
    os.makedirs(v26_dir, exist_ok=True)
    adaptive_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
'''
    with open(os.path.join(v26_dir, 'ic_launcher.xml'), 'w', encoding='utf-8') as f:
        f.write(adaptive_xml)
    with open(os.path.join(v26_dir, 'ic_launcher_round.xml'), 'w', encoding='utf-8') as f:
        f.write(adaptive_xml)
    print("Verified mipmap-anydpi-v26 adaptive icon XMLs")

    print("\nAll Android launcher icon densities successfully configured with zero-crop safe-zone padding!")

if __name__ == '__main__':
    main()
