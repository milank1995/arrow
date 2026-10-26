import os
import gzip
import json
import UnityPy

UnityPy.config.FALLBACK_UNITY_VERSION = '2022.3.20f1'

bundles = [
    r'c:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\levelpacks_newuser_2c8302a1cbb1e9f8abf1795f681728e4.ab',
    r'c:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\arrowclashlevels_7e1041dd308db9b4ef96b6258926e88b.ab',
    r'c:\Projects\Personal\Games\arrow decode\original arrow decode\app\src\main\assets\AssetBundle\boardmap_arrow3d_fc9dbf2a398ce9a01442c204b792bede.ab'
]

for b_path in bundles:
    if not os.path.exists(b_path):
        continue
    bname = os.path.basename(b_path).split('_')[0]
    with open(b_path, 'rb') as f:
        f.seek(32)
        env = UnityPy.load(f.read())
    found = 0
    for obj in env.objects:
        if obj.type.name == 'TextAsset':
            d = obj.read()
            raw = d.m_Script.encode('utf-8', 'surrogateescape')
            try:
                data = json.loads(gzip.decompress(raw))
                if data.get('XSize') == 9 and data.get('YSize') == 10 and len(data.get('Arrows', [])) == 3:
                    name = getattr(d, 'm_Name', '')
                    pts = [a.get('Points') for a in data.get('Arrows', [])]
                    print(f"Match found in {bname}: {name} pts={pts}")
                    found += 1
            except Exception:
                pass
    print(f"Bundle {bname}: found {found} matches for (9,10,3)")
