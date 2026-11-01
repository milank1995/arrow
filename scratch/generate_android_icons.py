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

def extract_clean_foreground(src_img):
    w, h = src_img.size
    fg = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    for y in range(h):
        for x in range(w):
            r, g, b, a = src_img.getpixel((x, y))
            # If near white, make transparent
            if r > 248 and g > 248 and b > 248:
                fg.putpixel((x, y), (0, 0, 0, 0))
            else:
                # Anti-aliased edge smoothing for near-white pixels
                luminance = (r + g + b) / 3.0
                if luminance > 230:
                    alpha = int((1.0 - (luminance - 230.0) / 25.0) * 255.0)
                    fg.putpixel((x, y), (r, g, b, max(0, min(255, alpha))))
                else:
                    fg.putpixel((x, y), (r, g, b, 255))
    return fg

def main():
    src_path = os.path.join(ARTIFACT_DIR, 'user_logo_healed.png')
    src_img = Image.open(src_path).convert('RGBA')

    # 1. Extract clean transparent foreground
    fg = extract_clean_foreground(src_img)

    # 2. Find bounding box of artwork
    bbox = fg.getbbox() # (left, top, right, bottom)
    cropped_art = fg.crop(bbox)
    art_w, art_h = cropped_art.size

    # 3. Generate Play Store 512x512 Store Listing Icon
    play_store_icon = Image.new('RGBA', (512, 512), (255, 255, 255, 255))
    target_art_size = 360 # ~70% of 512
    scale = target_art_size / max(art_w, art_h)
    new_w = int(art_w * scale)
    new_h = int(art_h * scale)
    resized_art = cropped_art.resize((new_w, new_h), Image.Resampling.LANCZOS)
    pos_x = (512 - new_w) // 2
    pos_y = (512 - new_h) // 2
    play_store_icon.paste(resized_art, (pos_x, pos_y), resized_art)
    play_store_path = os.path.join(ARTIFACT_DIR, 'play_store_512.png')
    play_store_icon.save(play_store_path, 'PNG')
    print(f'Generated Google Play Store 512x512 icon: {play_store_path}')

    # 4. Generate Mipmap densities for Adaptive Foreground (ic_launcher_foreground.png)
    for folder, size in FOREGROUND_DENSITIES.items():
        out_dir = os.path.join(PROJECT_RES, folder)
        os.makedirs(out_dir, exist_ok=True)

        canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        # Center inside 65% safe-zone of the 108dp canvas
        target_size = int(size * 0.65)
        s = target_size / max(art_w, art_h)
        nw = int(art_w * s)
        nh = int(art_h * s)
        res_art = cropped_art.resize((nw, nh), Image.Resampling.LANCZOS)
        px = (size - nw) // 2
        py = (size - nh) // 2
        canvas.paste(res_art, (px, py), res_art)

        out_file = os.path.join(out_dir, 'ic_launcher_foreground.png')
        canvas.save(out_file, 'PNG')
        print(f'  Saved adaptive foreground ({size}x{size}): {out_file}')

    # 5. Generate Legacy Launcher Icons (ic_launcher.png & ic_launcher_round.png)
    for folder, size in ICON_DENSITIES.items():
        out_dir = os.path.join(PROJECT_RES, folder)
        os.makedirs(out_dir, exist_ok=True)

        # Standard icon (white squircle / square card)
        sq_icon = Image.new('RGBA', (size, size), (255, 255, 255, 255))
        target_size = int(size * 0.72)
        s = target_size / max(art_w, art_h)
        nw = int(art_w * s)
        nh = int(art_h * s)
        res_art = cropped_art.resize((nw, nh), Image.Resampling.LANCZOS)
        px = (size - nw) // 2
        py = (size - nh) // 2
        sq_icon.paste(res_art, (px, py), res_art)

        sq_file = os.path.join(out_dir, 'ic_launcher.png')
        sq_icon.save(sq_file, 'PNG')

        # Round icon (white circle on transparent background)
        round_icon = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        draw_r = ImageDraw.Draw(round_icon)
        draw_r.ellipse([0, 0, size, size], fill=(255, 255, 255, 255))
        round_icon.paste(res_art, (px, py), res_art)

        round_file = os.path.join(out_dir, 'ic_launcher_round.png')
        round_icon.save(round_file, 'PNG')
        print(f'  Saved legacy icons ({size}x{size}): {sq_file} & {round_file}')

    # 6. Update ic_launcher_background.xml to clean white
    bg_xml_path = os.path.join(PROJECT_RES, 'drawable', 'ic_launcher_background.xml')
    bg_xml_content = '''<?xml version="1.0" encoding="utf-8"?>
<color xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#FFFFFF" />
'''
    with open(bg_xml_path, 'w', encoding='utf-8') as f:
        f.write(bg_xml_content)
    print(f'Updated {bg_xml_path} to white color')

    # 7. Update mipmap-anydpi-v26/ic_launcher.xml and ic_launcher_round.xml
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
    print(f'Updated mipmap-anydpi-v26 adaptive icon XMLs to reference @mipmap/ic_launcher_foreground')

    # 8. Clean up obsolete drawable/ic_launcher_foreground.xml if present
    old_fg_xml = os.path.join(PROJECT_RES, 'drawable', 'ic_launcher_foreground.xml')
    if os.path.exists(old_fg_xml):
        os.remove(old_fg_xml)
        print(f'Removed obsolete {old_fg_xml}')

    print('All app icon assets successfully generated and configured!')

if __name__ == '__main__':
    main()
