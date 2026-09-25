"""Build boot geometry with individually mapped faces from the unchanged atlases."""
from pathlib import Path
import json,math
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets'
MATERIALS={
 'bishop_of_deceit':{'base':['cbbccc','e0d6e1','ffffff'],'edge':['774019','b06412','cd7d00','eb9c31','f7b913','ffed62'],'sole':['774019','874a08','b06412'],'bone':['e0d6e1','ffffff']},
 'bone_reptile':{'base':['1f1e38','252445','2d2c52','3d3c61'],'edge':['945b31','b98a3c','dbb86a','ffe69a'],'bone':['5f473f','7e675e','9a8c7c','b7b196','dbcea7'],'sole':['2c1816','5f473f','7e675e']},
 'pyromancer_brute':{'base':['101010','242424','2e2e2d','383837','656564'],'edge':['60230a','de8a1c','e9af15','f5da2a','ffe5b5'],'sole':['101010','242424'],'bone':['60230a','762b0d']},
 'nameless_one':{'base':['141118','151421','1e1a2c','2c243f','4d4061'],'edge':['005503','008802','009d05'],'bone':['88929b','b1bbc4','cdd3d9'],'sole':['0a090d','141118']},
 'necromancer':{'base':['181116','1f1421','2c1a2c','3f243d','61405b'],'edge':['174954','247889','0b9cbb'],'bone':['99896c','b2a284','c5b698','ddd1b6'],'sole':['181116','1f1421']},
}

def integral(values):
    out=[[0.0]*129 for _ in range(129)]
    for y in range(128):
        row=0.0
        for x in range(128):
            row+=values[y][x];out[y+1][x+1]=out[y][x+1]+row
    return out

def total(a,x,y,w,h):return a[y+h][x+w]-a[y][x+w]-a[y+h][x]+a[y][x]

class Atlas:
    def __init__(self,path,palettes):
        image=Image.open(path).convert('RGBA');self.pixels=list(image.get_flattened_data());self.cache={}
        self.alpha=integral([[int(self.pixels[y*128+x][3]!=255) for x in range(128)]for y in range(128)])
        self.metrics={}
        for key,hexes in palettes.items():
            colors=[tuple(bytes.fromhex(h))for h in hexes]
            distances=[];brightness=[];squares=[]
            for y in range(128):
                row=[];light=[];sq=[]
                for x in range(128):
                    p=self.pixels[y*128+x];row.append(min(sum(abs(p[i]-c[i])for i in range(3))for c in colors));v=sum(p[:3])/3;light.append(v);sq.append(v*v)
                distances.append(row);brightness.append(light);squares.append(sq)
            self.metrics[key]=(integral(distances),integral(brightness),integral(squares))

    def uv(self,material,w,h):
        w=max(1,math.ceil(w));h=max(1,math.ceil(h));key=(material,w,h)
        if key not in self.cache:
            distance,light,squares=self.metrics[material];best=None
            for y in range(129-h):
                for x in range(129-w):
                    if total(self.alpha,x,y,w,h):continue
                    mean=total(light,x,y,w,h)/(w*h)
                    variance=max(0,total(squares,x,y,w,h)/(w*h)-mean*mean)
                    score=total(distance,x,y,w,h)/(w*h)-math.sqrt(variance)*.055
                    candidate=(score,y,x)
                    if best is None or candidate<best:best=candidate
            if best is None:raise ValueError(f'No opaque {w}x{h} atlas region for {material}')
            _,y,x=best;self.cache[key]={'uv':[x,y],'uv_size':[w,h]}
        return self.cache[key].copy()

def make_boots(family,atlas):
    wizard=family in ('nameless_one','necromancer');result=[]
    for side,sign in [('Left',1),('Right',-1)]:
        center=(2.25 if wizard else 2)*sign
        pieces=[]
        def box(name,x,y,z,w,h,d,material):
            w,h,d=(max(1,v) for v in (w,h,d))
            faces={n:atlas.uv(material,a,b)for n,a,b in [('north',w,h),('south',w,h),('east',d,h),('west',d,h),('up',w,d),('down',w,d)]}
            pieces.append({'name':name,'origin':[round(center+x,4),y,z],'size':[w,h,d],'uv':faces})
        depth=6.2 if wizard else 5.0
        box('shaft',-2.5,.4,-depth/2,5,5.4,depth,'base')
        box('closed_toe',-2.65,-.2,-3.7,5.3,2.1,3.2,'base' if family!='bone_reptile' else 'edge')
        box('sole',-2.75,-.35,-3.75,5.5,1,6.7,'sole')
        box('heel',-2.2,0,1.15,4.4,1.4,1.9,'sole')
        box('cuff',-2.75,5.7,-(depth+.5)/2,5.5,1,depth+.5,'base' if wizard else 'edge')
        box('ankle_wrap',-2.68,1.9,-2.65,5.36,.9,5.3,'edge')
        box('instep',-1.9,1.45,-3.4,3.8,1,2.2,'base')
        outer=1.8 if sign>0 else -2.8
        if family=='bishop_of_deceit':
            box('gold_galloon',outer,2.8,-2.7,1,2.8,.75,'edge')
            box('toe_trim',-2.55,.35,-3.95,5.1,.8,.8,'edge')
            box('cuff_clasp',-.8,4.75,-2.85,1.6,1.1,.8,'edge')
        elif family=='bone_reptile':
            box('shin_frame',-1.95,2.7,-2.85,3.9,3.2,.8,'edge')
            box('fossil_shin',-1.35,2.95,-3.1,2.7,2.7,.85,'bone')
            box('fossil_toe',-1.85,.8,-3.95,3.7,1.05,1.4,'bone')
            box('side_brace',outer,2.75,-1.6,1,2.8,3,'edge')
            box('heel_bone',-1.5,1.15,2.25,3,1.2,1,'bone')
        elif family=='pyromancer_brute':
            box('gold_toe_plate',-2.25,.65,-3.95,4.5,1.1,2,'edge')
            box('charcoal_shin',-1.6,2.8,-2.9,3.2,2.8,.8,'base')
            box('outer_cuff_plate',outer,4.6,-1.75,1,2.1,3.5,'edge')
            box('shin_fastener',-.75,3.3,-3.15,1.5,1.2,.6,'edge')
        else:
            box('side_piping',outer,2.7,-3.25,1,2.9,.9,'edge')
            box('toe_piping',-2.5,.3,-3.95,5,.85,.8,'edge')
            box('cuff_binding',-2.6,5.1,-3.4,5.2,.8,.65,'edge')
            box('clasp',1.0 if sign>0 else -2.0,4.6,-3.55,1,1.2,.85,'bone')
        result.append({'name':'armor'+side+'Boot','parent':'biped'+side+'Leg','pivot':[2*sign,12,0],'cubes':pieces})
    return result

def main():
    for family,palettes in MATERIALS.items():
        namespace='cataclysm'if family=='bone_reptile'else'hazennstuff'
        atlas=Atlas(ASSETS/namespace/f'textures/armor/{family}_completed.png',palettes)
        boots=make_boots(family,atlas)
        if family=='bone_reptile':
            path=ASSETS/'armorcompletions/geometry/bone_reptile_boots.json';path.parent.mkdir(parents=True,exist_ok=True)
            data={'texture_width':128,'texture_height':128,'bones':boots}
        else:
            path=ASSETS/namespace/f'geo/armor/{family}_completed.geo.json';data=json.loads(path.read_text())
            bones={b['name']:b for b in data['minecraft:geometry'][0]['bones']}
            for boot in boots:bones[boot['name']]['cubes']=[{k:v for k,v in c.items()if k!='name'}for c in boot['cubes']]
        path.write_text(json.dumps(data,indent=2)+'\n')
        print(family, len(boots[0]['cubes']), 'parts per boot; all faces >= 1 texel per model unit')

if __name__=='__main__':main()
