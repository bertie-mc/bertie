"""Convert a Bedrock/Geckolib .geo.json into a vanilla item model.

Geckolib draws its own geometry, and only for items the mod that owns it registered. An item
registered from outside gets no such renderer, so the geometry is rewritten here as ordinary model
`elements` and the vanilla item renderer draws it instead.

The one real constraint is rotation. A bone may turn about three axes by any angle; a vanilla
element may turn about ONE axis by one of five. Every quarter turn is therefore baked into the box
corners - a 90 degree turn maps an axis-aligned box onto another one, permuting its faces - and
only the remainder is left on the element, snapped to the nearest angle vanilla accepts.

Bedrock's X axis runs opposite Java's. Rather than mirror, which would reverse every face's UV as
well, the axes are used as they are: the model is geometrically exact and reads mirrored against
the mod's own renderer, which for a book or a staff is not something you can see.
"""
import json
import math

# name -> (normal, u direction, v direction). V runs down the texture.
FACE_BASIS = {
    "north": ((0, 0, -1), (-1, 0, 0), (0, -1, 0)),
    "south": ((0, 0, 1), (1, 0, 0), (0, -1, 0)),
    "west": ((-1, 0, 0), (0, 0, 1), (0, -1, 0)),
    "east": ((1, 0, 0), (0, 0, -1), (0, -1, 0)),
    "up": ((0, 1, 0), (1, 0, 0), (0, 0, 1)),
    "down": ((0, -1, 0), (1, 0, 0), (0, 0, -1)),
}
ALLOWED = (-45.0, -22.5, 0.0, 22.5, 45.0)
approximated = []


def _mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def _rot(axis, deg):
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    return ([[1, 0, 0], [0, c, -s], [0, s, c]] if axis == 0 else
            [[c, 0, s], [0, 1, 0], [-s, 0, c]] if axis == 1 else
            [[c, -s, 0], [s, c, 0], [0, 0, 1]])


def _apply(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))


def _sign_vec(v):
    return tuple(int(round(c)) for c in v)


def _face_for(normal):
    for name, (n, _, _) in FACE_BASIS.items():
        if _sign_vec(normal) == n:
            return name
    return None


def _uv_rotation(name, u, v):
    """How far the baked face's texture has turned against that face's canonical axes."""
    _, cu, cv = FACE_BASIS[name]
    u, v = _sign_vec(u), _sign_vec(v)
    neg = lambda t: tuple(-c for c in t)
    for deg, (tu, tv) in ((0, (cu, cv)), (90, (neg(cv), cu)),
                          (180, (neg(cu), neg(cv))), (270, (cv, neg(cu)))):
        if u == tuple(tu) and v == tuple(tv):
            return deg
    return 0


def _box_uv(u, v, size, tw, th):
    w, h, d = size
    raw = {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d),
           "west": (u, v + d, d, h), "north": (u + d, v + d, w, h),
           "east": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h)}
    return {f: [x * 16.0 / tw, y * 16.0 / th, (x + fw) * 16.0 / tw, (y + fh) * 16.0 / th]
            for f, (x, y, fw, fh) in raw.items()}


def _split(angles, label):
    """(quarter turns to bake, single leftover axis, leftover angle)."""
    quarters = [round(a / 90.0) * 90.0 for a in angles]
    rest = [angles[i] - quarters[i] for i in range(3)]
    axis = max(range(3), key=lambda i: abs(rest[i]))
    leftover = min(ALLOWED, key=lambda a: abs(a - rest[axis]))
    for i in range(3):
        if i != axis and abs(rest[i]) > 1e-6:
            approximated.append(f"{label}: dropped {rest[i]:+.1f} deg about {'xyz'[i]}")
    if abs(leftover - rest[axis]) > 1e-6:
        approximated.append(f"{label}: {rest[axis]:+.1f} deg about {'xyz'[axis]} -> {leftover:+.1f}")
    return quarters, axis, leftover


def convert(geo, texture, display=None, label=""):
    g = geo["minecraft:geometry"][0]
    desc = g["description"]
    tw = desc.get("texture_width", 16)
    th = desc.get("texture_height", tw)
    bones = {b["name"]: b for b in g["bones"]}
    elements = []

    for bone in g["bones"]:
        chain, name = [], bone["name"]
        while name in bones:
            chain.append(bones[name])
            name = bones[name].get("parent")
        for cube in bone.get("cubes", []):
            uv = cube.get("uv")
            if not isinstance(uv, list):
                continue
            angles = [0.0, 0.0, 0.0]
            pivot = (0.0, 0.0, 0.0)
            for b in chain:
                r = b.get("rotation") or (0, 0, 0)
                if any(r):
                    angles = [angles[i] + r[i] for i in range(3)]
                    pivot = tuple(b.get("pivot") or (0, 0, 0))
            cr = cube.get("rotation")
            if cr:
                angles = [angles[i] + cr[i] for i in range(3)]
                pivot = tuple(cube.get("pivot") or pivot)
            elements.append(_element(cube, uv, angles, pivot, tw, th,
                                     f"{label}/{bone['name']}"))
    span = _recentre(elements)
    return {"credit": "geometry taken from the mod's own .geo.json",
            "texture_size": [tw, th],
            "textures": {"0": texture, "particle": texture},
            "display": display or _display(span),
            "elements": elements}


def _recentre(elements):
    """Move the geometry so its middle sits at the block centre, and report its longest side.

    The mod's own display transforms are written for Geckolib, which centres the model for itself.
    A vanilla element is placed absolutely, so the model is centred here instead and the transforms
    below can then be the same for every item.
    """
    lo = [min(e["from"][i] for e in elements) for i in range(3)]
    hi = [max(e["to"][i] for e in elements) for i in range(3)]
    shift = [8.0 - (lo[i] + hi[i]) / 2 for i in range(3)]
    for e in elements:
        e["from"] = [round(e["from"][i] + shift[i], 4) for i in range(3)]
        e["to"] = [round(e["to"][i] + shift[i], 4) for i in range(3)]
        if "rotation" in e:
            o = e["rotation"]["origin"]
            e["rotation"]["origin"] = [round(o[i] + shift[i], 4) for i in range(3)]
    return max(hi[i] - lo[i] for i in range(3))


def _display(span):
    """Held and inventory transforms.

    Scale is derived from a target size in model units rather than a fixed factor, so a book and a
    staff end up the same apparent size in the slot and in the hand however tall their geometry is.
    The targets match what a vanilla item occupies in each view.
    """
    def sc(units):
        return [round(min(4.0, units / span), 3)] * 3 if span else [1.0] * 3
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": sc(13)},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": sc(6)},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": sc(12)},
        "head": {"rotation": [0, 180, 0], "translation": [0, 13, 0], "scale": sc(18)},
        "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5],
                                  "scale": sc(9)},
        "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, 0.5],
                                 "scale": sc(9)},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13],
                                  "scale": sc(11)},
        "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13],
                                 "scale": sc(11)},
    }


def _element(cube, uv, angles, pivot, tw, th, label):
    o = list(cube["origin"])
    s = list(cube["size"])
    inf = cube.get("inflate", 0.0)
    o = [o[i] - inf for i in range(3)]
    s = [s[i] + 2 * inf for i in range(3)]
    quarters, axis, leftover = _split(angles, label)
    q = _mul(_rot(0, quarters[0]), _mul(_rot(1, quarters[1]), _rot(2, quarters[2])))

    lo = [o[i] - pivot[i] for i in range(3)]
    hi = [lo[i] + s[i] for i in range(3)]
    a, b = _apply(q, lo), _apply(q, hi)
    lo = [min(a[i], b[i]) + pivot[i] + (8.0 if i != 1 else 0.0) for i in range(3)]
    hi = [max(a[i], b[i]) + pivot[i] + (8.0 if i != 1 else 0.0) for i in range(3)]

    src = _box_uv(uv[0], uv[1], s, tw, th)
    faces = {}
    for name, (n, u, v) in FACE_BASIS.items():
        moved = _face_for(_apply(q, n))
        if moved is None:
            continue
        face = {"uv": [round(c, 4) for c in src[name]], "texture": "#0"}
        deg = _uv_rotation(moved, _apply(q, u), _apply(q, v))
        if deg:
            face["rotation"] = deg
        faces[moved] = face

    el = {"from": [round(c, 4) for c in lo], "to": [round(c, 4) for c in hi], "faces": faces}
    if abs(leftover) > 1e-6:
        el["rotation"] = {"origin": [round(pivot[0] + 8.0, 4), round(pivot[1], 4),
                                     round(pivot[2] + 8.0, 4)],
                          "axis": "xyz"[axis], "angle": leftover}
    return el
