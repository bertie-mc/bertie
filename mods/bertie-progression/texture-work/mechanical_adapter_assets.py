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
    for half in ('lower','upper'):
        for kind in ('body','shaft'):
            data=json.loads((source/f'mechanical_adapter_{kind}_{half}.json').read_text())
            data['textures']={key:'bertieprogression:block/mechanical_adapter' for key in data['textures']}
            data['textures']['particle']='bertieprogression:block/mechanical_adapter'
            name=f'mechanical_adapter_{half}' if kind=='body' else f'mechanical_adapter_shaft_{half}'
            write(Path(f'assets/bertieprogression/models/block/{name}.json'),data)
    write(Path('assets/bertieprogression/blockstates/mechanical_adapter.json'), {'variants': {
        f'facing={direction},half={half},machine_assembled={formed}': {'model':f'bertieprogression:block/mechanical_adapter_{half}', 'y':angle}
        for direction,angle in [('north',0),('east',90),('south',180),('west',270)]
        for half in ('lower','upper') for formed in ('true','false')}})
    write(Path('assets/bertieprogression/models/item/mechanical_adapter.json'),json.loads((source/'mechanical_adapter_item.json').read_text()))
    target = resources/'assets/bertieprogression/textures/block/mechanical_adapter.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source/'albedo.png',target)
    entries=[]
    for half,item in [('lower','bertieprogression:mechanical_adapter'),('upper','create:copper_casing')]:
        entries.append({'rolls':1,'entries':[{'type':'minecraft:item','name':item}],
            'conditions':[{'condition':'minecraft:survives_explosion'},
                {'condition':'minecraft:block_state_property','block':'bertieprogression:mechanical_adapter','properties':{'half':half}}]})
    write(Path('data/bertieprogression/loot_table/blocks/mechanical_adapter.json'), {
        'type':'minecraft:block', 'pools':[{'rolls':1,'entries':[
            {'type':'minecraft:item','name':'bertieprogression:mechanical_adapter','conditions':[{'condition':'minecraft:block_state_property','block':'bertieprogression:mechanical_adapter','properties':{'half':'lower'}}]},
            {'type':'minecraft:item','name':'create:copper_casing','conditions':[{'condition':'minecraft:block_state_property','block':'bertieprogression:mechanical_adapter','properties':{'half':'upper'}}]}],
            'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    write(Path('assets/bertieprogression/blockstates/assembled_copper_core.json'), {'variants':{'':{'model':'create:block/copper_casing'}}})
    write(Path('data/bertieprogression/loot_table/blocks/assembled_copper_core.json'), {
        'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'create:copper_casing'}],
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
