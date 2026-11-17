"""Selected Arcane Anvil, chemical-machine and Entangloporter acquisition changes."""
import copy
import json
from pathlib import Path

from compact_storage_recipes import load_native
from recipe_outputs import item_outputs

OFF = {'neoforge:conditions': [{'type': 'neoforge:false'}]}
LARGE_MINI = {f'mekmm:large_{name}' for name in (
    'heat_generator', 'gas_burning_generator', 'pigment_mixer',
    'chemical_infuser', 'electrolytic_separator')}


def apply(module, native):
    module = Path(module)
    resources = module/'src/main/resources'
    effective = copy.deepcopy(native)
    for path in (resources/'data').glob('*/recipe/**/*.json'):
        rel = path.relative_to(resources/'data')
        effective[rel.parts[0]+':'+ '/'.join(rel.parts[2:])[:-5]] = json.loads(path.read_text(encoding='utf-8'))
    changed = []

    def write(rid, value):
        ns, name = rid.split(':', 1)
        path = resources/f'data/{ns}/recipe/{name}.json'
        text = json.dumps(value, indent=2)+'\n'
        if not path.exists() or path.read_text(encoding='utf-8') != text:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8', newline='\n')
            changed.append(rid)
        effective[rid] = copy.deepcopy(value)

    def conditions(items):
        return [{'type':'neoforge:mod_loaded','modid':ns} for ns in
                sorted({item.split(':')[0] for item in items}-{'minecraft','bertieprogression','c'})]

    for row in json.loads((module/'texture-work/queued_051_layouts.json').read_text(encoding='utf-8')):
        items = [v['item'] for v in row['key'].values()]+[row['result']['id'],'avaritia:table']
        write(row['recipe'], {'type':'avaritia:shaped_table','tier':row['tier'],
              'pattern':row['pattern'],'key':row['key'],'result':row['result'],
              'neoforge:conditions':conditions(items)})

    write('irons_spellbooks:arcane_anvil', {
        'type':'malum:spirit_infusion',
        'input':{'item':'minecraft:anvil','count':1},
        'extraInputs':[{'item':item,'count':count} for item,count in (
            ('minecraft:amethyst_block',16),('berlordscarving:diamond_big_slate',4),
            ('minecraft:polished_deepslate',4))],
        'spirits':[{'type':'malum:'+kind,'count':count} for kind,count in (('arcane',16),('earthen',15),('aqueous',8))],
        'result':{'id':'irons_spellbooks:arcane_anvil','count':1},
        'neoforge:conditions':conditions(['malum:spirit_altar','irons_spellbooks:arcane_anvil','berlordscarving:diamond_big_slate'])})

    for rid, value in list(effective.items()):
        if value.get('type') == 'compactcrafting:miniaturization' or value == OFF:
            continue
        outputs = item_outputs(value, set())
        if outputs & LARGE_MINI:
            assert value.get('type') in {'minecraft:crafting_shaped','minecraft:crafting_shapeless',
                                        'mekanism:mek_data','avaritia:shaped_table'}, (rid,value.get('type'))
            write(rid, OFF)
        elif 'irons_spellbooks:arcane_anvil' in outputs and rid != 'irons_spellbooks:arcane_anvil':
            if value.get('type') in {'minecraft:crafting_shaped','minecraft:crafting_shapeless','avaritia:shaped_table'}:
                write(rid, OFF)

    for name in ('pigment_extractor','pigment_mixer'):
        rid = 'mekanism:'+name
        value = copy.deepcopy(effective[rid])
        def replace(obj):
            if isinstance(obj, dict):
                for key, child in obj.items():
                    if child == 'mekanism_extras:alloy_thermonuclear':obj[key]='mekanism:alloy_infused'
                    else:replace(child)
            elif isinstance(obj,list):
                for child in obj:replace(child)
        replace(value)
        # Conditions may still cover other Extras ingredients; preserve them.
        write(rid, value)

    rid = 'mekanism_extras:reaction/molten_thermonuclear_gasification/molten_thermonuclear'
    value = copy.deepcopy(effective[rid])
    old = value['item_input']
    assert old.get('tag') == 'c:ingots/netherite' or old.get('item') in {
        'l2complements:sculkium_ingot', 'iceandfire:dragonsteel_fire_ingot'}
    value['item_input'] = {'item':'l2complements:sculkium_ingot','count':old['count']}
    value['neoforge:conditions'] = conditions(['mekanism:reaction','mekanism_extras:molten_thermonuclear','l2complements:sculkium_ingot'])
    write(rid, value)
    return changed


if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--module', type=Path, required=True)
    p.add_argument('--mods', type=Path, required=True)
    args = p.parse_args()
    print(json.dumps({'changed':apply(args.module,load_native(args.mods))}))
