import random

def rotate_90(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    new_xs = ys
    new_ys = xs
    new_arrows = []
    for a in lvl['Arrows']:
        new_idxs = []
        for idx in a['Indices']:
            ux = idx % xs
            uy = idx // xs
            new_ux = ys - 1 - uy
            new_uy = ux
            new_idxs.append(new_uy * new_xs + new_ux)
        dx = a['Dx']
        dy = a['Dy']
        new_dx = -dy
        new_dy = dx
        new_a = dict(a)
        new_a['Indices'] = new_idxs
        new_a['Dx'] = new_dx
        new_a['Dy'] = new_dy
        new_arrows.append(new_a)
    res = dict(lvl)
    res['XSize'] = new_xs
    res['YSize'] = new_ys
    res['Arrows'] = new_arrows
    return res

def flip_h(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    new_arrows = []
    for a in lvl['Arrows']:
        new_idxs = []
        for idx in a['Indices']:
            ux = idx % xs
            uy = idx // xs
            new_ux = xs - 1 - ux
            new_uy = uy
            new_idxs.append(new_uy * xs + new_ux)
        dx = a['Dx']
        dy = a['Dy']
        new_dx = -dx
        new_dy = dy
        new_a = dict(a)
        new_a['Indices'] = new_idxs
        new_a['Dx'] = new_dx
        new_a['Dy'] = new_dy
        new_arrows.append(new_a)
    res = dict(lvl)
    res['Arrows'] = new_arrows
    return res

def flip_v(lvl):
    xs = lvl['XSize']
    ys = lvl['YSize']
    new_arrows = []
    for a in lvl['Arrows']:
        new_idxs = []
        for idx in a['Indices']:
            ux = idx % xs
            uy = idx // xs
            new_ux = ux
            new_uy = ys - 1 - uy
            new_idxs.append(new_uy * xs + new_ux)
        dx = a['Dx']
        dy = a['Dy']
        new_dx = dx
        new_dy = -dy
        new_a = dict(a)
        new_a['Indices'] = new_idxs
        new_a['Dx'] = new_dx
        new_a['Dy'] = new_dy
        new_arrows.append(new_a)
    res = dict(lvl)
    res['Arrows'] = new_arrows
    return res

def transform_level(lvl, seed=None):
    if seed is not None:
        random.seed(seed)
    ops = [
        lambda l: l,
        rotate_90,
        lambda l: rotate_90(rotate_90(l)),
        lambda l: rotate_90(rotate_90(rotate_90(l))),
        flip_h,
        flip_v,
        lambda l: flip_h(rotate_90(l)),
        lambda l: flip_v(rotate_90(l)),
    ]
    chosen = random.choice(ops[1:]) # always apply at least one transformation!
    transformed = chosen(lvl)
    
    # Shuffle arrow array order so indices array order is completely randomized
    arrows = list(transformed['Arrows'])
    random.shuffle(arrows)
    transformed['Arrows'] = arrows
    return transformed
