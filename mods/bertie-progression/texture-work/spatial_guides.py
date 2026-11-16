"""Deterministic Ponder structures for the owned spatial recipes."""
import gzip
import io
import json
from pathlib import Path
import nbtlib as nbt


def generate(module):
    module = Path(module)
    res = module / 'src/main/resources'
    rows = json.loads((res / 'compact_mini_recipes.json').read_text())
    captures = {r['output']['id']: r for r in json.loads((module / 'texture-work/large_machine_mini_layouts.json').read_text())['recipes']}
    guides = []
    for row in rows:
        n = row['size']
        if row.get('machine_layout'):
            cells = captures[row['output']['id']]['physical_cells']
            blocks = [{'pos': [b['relative'][0] + 1, b['relative'][1], b['relative'][2] + 1], 'state': b['state']}
                      for b in cells if b['state']['Name'] != 'minecraft:air']
        else:
            blocks = []
            for y in range(n):
                for z in range(n):
                    for x in range(n):
                        block = row['wall'] if any(c in (0, n-1) for c in (x,y,z)) else row['core'] if x == y == z == n//2 else None
                        if block: blocks.append({'pos':[x,y,z], 'state':{'Name':block}})
        guides.append({'recipe': row['recipe'], 'size': n, 'kind':'mini', 'blocks': blocks,
                       'catalyst': row['catalyst'], 'output': row['output']})
    for name, source in [('overcharged_crystal','multiblock/overcharged_crystal'),('compact_machine_wall','multiblock/compact_machine_wall')]:
        value = json.loads((res / f'data/bertieprogression/recipe/{source}.json').read_text())
        pattern = value['pattern']; n = len(pattern['layers']); blocks=[]
        for y,layer in enumerate(pattern['layers']):
            for z,line in enumerate(layer):
                for x,symbol in enumerate(line):
                    if symbol != ' ': blocks.append({'pos':[x,y,z], 'state':{'Name':pattern['symbols'][symbol]['block']}})
        guides.append({'recipe':'bertieprogression:giant_anvil/'+name,'size':n,'kind':'giant','blocks':blocks,
                       'catalyst':'anvilcraft:giant_anvil','output':value['result']})
        if name=='compact_machine_wall':guides[-1]['block_output']='compactmachines:wall'
    for guide in guides:
        n=guide['size']; guide['scene']='bertieprogression:guide/'+guide['recipe'].split(':')[1]
        palette=[]; by_state={}; blocks=[]
        def add(pos,state,tag=None):
            key=json.dumps(state,sort_keys=True)
            if key not in by_state:
                by_state[key]=len(palette)
                value={'Name':nbt.String(state['Name'])}
                if state.get('Properties'):value['Properties']=nbt.Compound({k:nbt.String(v) for k,v in state['Properties'].items()})
                palette.append(nbt.Compound(value))
            value={'pos':nbt.List[nbt.Int](pos),'state':nbt.Int(by_state[key])}
            if tag:value['nbt']=nbt.Compound(tag)
            blocks.append(nbt.Compound(value))
        for z in range(n+2):
            for x in range(n+2):add([x,0,z],{'Name':'minecraft:gray_concrete'})
        for b in guide['blocks']:
            x,y,z=b['pos'];add([x+1,y+1,z+1],b['state'])
        if guide['kind']=='mini':
            for x,z,facing in [(0,0,'south'),(n+1,0,'west'),(n+1,n+1,'north'),(0,n+1,'east')]:
                add([x,1,z],{'Name':'compactcrafting:field_projector','Properties':{'facing':facing}})
        else:
            for x in range(n):
                for z in range(n):add([x+1,n+1,z+1],{'Name':'minecraft:crafting_table'})
        structure=nbt.File({'DataVersion':nbt.Int(3955),'size':nbt.List[nbt.Int]([n+2,n+8,n+2]),
                            'palette':nbt.List[nbt.Compound](palette),'blocks':nbt.List[nbt.Compound](blocks),
                            'entities':nbt.List[nbt.Compound]([])})
        raw=io.BytesIO();structure.write(raw)
        path=res/'assets/bertieprogression/ponder'/('guide/'+guide['recipe'].split(':')[1]+'.nbt')
        path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(gzip.compress(raw.getvalue(),mtime=0))
    (res/'spatial_guides.json').write_text(json.dumps(guides,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    return guides
