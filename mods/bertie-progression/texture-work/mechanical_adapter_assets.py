"""Promote the approved two-block Mechanical Adapter without changing its palette."""
import copy
import json
import shutil
from pathlib import Path


def apply(module):
    module = Path(module)
    source = module/'texture-work/approved/mechanical_adapter'
    resources = module/'src/main/resources'

    def write(path, value):
        path = resources/path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value, indent=2)+'\n', encoding='utf-8', newline='\n')

    model = json.loads((source/'model.json').read_text(encoding='utf-8'))
    model['textures'] = {key: 'bertieprogression:block/mechanical_adapter' for key in model['textures']}
    model['textures']['particle'] = 'bertieprogression:block/mechanical_adapter'
    write(Path('assets/bertieprogression/models/block/mechanical_adapter_full.json'), model)
    for name, low, high in [('lower',0,16), ('upper',16,32)]:
        part = copy.deepcopy(model)
        part['elements'] = []
        for original in model['elements']:
            if original['to'][1] <= low or original['from'][1] >= high:
                continue
            element = copy.deepcopy(original)
            bottom, top = max(low, original['from'][1]), min(high, original['to'][1])
            height = original['to'][1] - original['from'][1]
            if height > 0:
                for face in ('north','south','east','west'):
                    if face in element.get('faces',{}) and 'uv' in element['faces'][face]:
                        uv = element['faces'][face]['uv']
                        rate = (uv[3]-uv[1])/height
                        uv[1] += (original['to'][1]-top)*rate
                        uv[3] -= (bottom-original['from'][1])*rate
            if top < original['to'][1]:
                element['faces'].pop('up',None)
            if bottom > original['from'][1]:
                element['faces'].pop('down',None)
            element['from'][1], element['to'][1] = bottom-low, top-low
            if 'rotation' in element:
                element['rotation']['origin'][1] -= low
            part['elements'].append(element)
        write(Path(f'assets/bertieprogression/models/block/mechanical_adapter_{name}.json'), part)
    write(Path('assets/bertieprogression/blockstates/mechanical_adapter.json'), {'variants': {
        f'facing={direction},half={half}': {'model':f'bertieprogression:block/mechanical_adapter_{half}', 'y':angle}
        for direction,angle in [('north',0),('east',90),('south',180),('west',270)] for half in ('lower','upper')}})
    write(Path('assets/bertieprogression/models/item/mechanical_adapter.json'), {
        'parent':'bertieprogression:block/mechanical_adapter_full',
        'display': {'gui': {'rotation':[30,225,0], 'translation':[0,-4,0], 'scale':[0.42,0.42,0.42]}}})
    target = resources/'assets/bertieprogression/textures/block/mechanical_adapter.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source/'albedo.png',target)
    write(Path('data/bertieprogression/loot_table/blocks/mechanical_adapter.json'), {
        'type':'minecraft:block', 'pools':[{'rolls':1,'entries':[{
            'type':'minecraft:item','name':'create:copper_casing',
            'functions':[{'function':'minecraft:set_count','count':2}]}],
            'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    lang_path = resources/'assets/bertieprogression/lang/en_us.json'
    lang = json.loads(lang_path.read_text(encoding='utf-8'))
    lang['block.bertieprogression.mechanical_adapter'] = 'Mechanical Adapter'
    lang['item.bertieprogression.mechanical_adapter'] = 'Mechanical Adapter'
    write(Path('assets/bertieprogression/lang/en_us.json'),lang)
    for tag in ['minecraft:mineable/pickaxe', 'minecraft:needs_stone_tool', 'create:non_movable']:
        ns,name = tag.split(':',1)
        relative = Path(f'data/{ns}/tags/block/{name}.json')
        path = resources/relative
        value = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace':False,'values':[]}
        if 'bertieprogression:mechanical_adapter' not in value['values']:
            value['values'].append('bertieprogression:mechanical_adapter')
        write(relative,value)


if __name__ == '__main__':
    import sys
    apply(Path(sys.argv[1]))
