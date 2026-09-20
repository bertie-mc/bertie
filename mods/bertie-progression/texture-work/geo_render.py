"""Isometric renderer for Bedrock/Geckolib .geo.json models."""
import json, math, io, zipfile
from PIL import Image, ImageDraw, ImageEnhance

def rotm(rx, ry, rz):
    rx, ry, rz = map(math.radians, (rx, ry, rz))
    def mul(A, B):
        return [[sum(A[i][k]*B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    Rx = [[1,0,0],[0,math.cos(rx),-math.sin(rx)],[0,math.sin(rx),math.cos(rx)]]
    Ry = [[math.cos(ry),0,math.sin(ry)],[0,1,0],[-math.sin(ry),0,math.cos(ry)]]
    Rz = [[math.cos(rz),-math.sin(rz),0],[math.sin(rz),math.cos(rz),0],[0,0,1]]
    return mul(Rz, mul(Ry, Rx))

def apply(M, v):
    return tuple(sum(M[i][k]*v[k] for k in range(3)) for i in range(3))

def add(a, b): return (a[0]+b[0], a[1]+b[1], a[2]+b[2])
def sub(a, b): return (a[0]-b[0], a[1]-b[1], a[2]-b[2])

FACES = {  # name: (corner indices ccw, normal)
 "east":  ((5,4,7,6), ( 1,0,0)), "west":  ((0,1,2,3), (-1,0,0)),
 "up":    ((3,2,6,7), (0, 1,0)), "down":  ((1,0,4,5), (0,-1,0)),
 "south": ((1,5,6,2), (0,0, 1)), "north": ((4,0,3,7), (0,0,-1)),
}
def corners(o, s):
    x,y,zz = o; w,h,d = s
    return [(x,y,zz),(x,y,zz+d),(x,y+h,zz+d),(x,y+h,zz),
            (x+w,y,zz),(x+w,y,zz+d),(x+w,y+h,zz+d),(x+w,y+h,zz)]

def box_uv(u, v, w, h, d):
    return {"up":(u+d, v, w, d), "down":(u+d+w, v, w, d),
            "east":(u, v+d, d, h), "north":(u+d, v+d, w, h),
            "west":(u+d+w, v+d, d, h), "south":(u+d+w+d, v+d, w, h)}

def affine(dst, src):
    (x0,y0),(x1,y1),(x2,y2) = dst
    (u0,v0),(u1,v1),(u2,v2) = src
    det = (x1-x0)*(y2-y0) - (x2-x0)*(y1-y0)
    if abs(det) < 1e-9: return None
    a = ((u1-u0)*(y2-y0) - (u2-u0)*(y1-y0)) / det
    b = ((x1-x0)*(u2-u0) - (x2-x0)*(u1-u0)) / det
    d = ((v1-v0)*(y2-y0) - (v2-v0)*(y1-y0)) / det
    e = ((x1-x0)*(v2-v0) - (x2-x0)*(v1-v0)) / det
    return (a, b, u0 - a*x0 - b*y0, d, e, v0 - d*x0 - e*y0)

def render(geo, atlas, size=420, yaw=-35, pitch=22, pad=1.15):
    g = geo["minecraft:geometry"][0]
    bones = {b["name"]: b for b in g["bones"]}
    cache = {}
    def bone_tf(name):
        if name in cache: return cache[name]
        b = bones[name]
        M, T = rotm(*(b.get("rotation") or (0,0,0))), b.get("pivot") or (0,0,0)
        if b.get("parent") in bones:
            PM, PT = bone_tf(b["parent"])
            def f(v, M=M, T=T, PM=PM, PT=PT):
                v = add(apply(M, sub(v, T)), T)
                return PT(v)
            cache[name] = (M, f)
        else:
            cache[name] = (M, lambda v, M=M, T=T: add(apply(M, sub(v, T)), T))
        return cache[name]
    V = rotm(pitch, yaw, 0)
    quads = []
    for b in g["bones"]:
        _, tf = bone_tf(b["name"])
        for c in b.get("cubes", []):
            o, s = list(c["origin"]), list(c["size"])
            inf = c.get("inflate", 0)
            o = [o[i]-inf for i in range(3)]; s = [s[i]+2*inf for i in range(3)]
            pts = [apply(V, tf(p)) for p in corners(o, s)]
            uvs = c.get("uv")
            if not isinstance(uvs, list): continue
            uvmap = box_uv(uvs[0], uvs[1], c["size"][0], c["size"][1], c["size"][2])
            for fname, (idx, n) in FACES.items():
                nv = apply(V, n)
                if nv[2] <= 0.001: continue
                quad = [pts[i] for i in idx]
                quads.append((sum(p[2] for p in quad)/4, quad, uvmap[fname], nv))
    xs = [p[0] for _, q, _, _ in quads for p in q]; ys = [-p[1] for _, q, _, _ in quads for p in q]
    if not xs: return None
    cx, cy = (min(xs)+max(xs))/2, (min(ys)+max(ys))/2
    span = max(max(xs)-min(xs), max(ys)-min(ys)) * pad or 1
    k = size / span
    def scr(p): return ((p[0]-cx)*k + size/2, (-p[1]-cy)*k + size/2)
    out = Image.new("RGBA", (size, size), (0,0,0,0))
    for _, quad, (u,v,w,h), nv in sorted(quads, key=lambda q: q[0]):
        P = [scr(p) for p in quad]
        co = affine((P[0], P[1], P[3]), ((u,v+h), (u+w,v+h), (u,v)))
        if not co: continue
        layer = atlas.transform((size,size), Image.AFFINE, co, resample=Image.NEAREST)
        shade = 0.55 + 0.45*max(0.0, nv[0]*0.4 + nv[1]*0.75 + nv[2]*0.5)
        layer = ImageEnhance.Brightness(layer).enhance(min(1.25, shade))
        mask = Image.new("L", (size,size), 0)
        ImageDraw.Draw(mask).polygon(P, fill=255)
        layer.putalpha(Image.composite(layer.getchannel("A"), Image.new("L",(size,size),0), mask))
        out.alpha_composite(layer)
    return out
