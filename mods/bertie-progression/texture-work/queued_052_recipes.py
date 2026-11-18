"""Captured powered equipment and selected cybernetics acquisition changes."""
import copy
import json
from pathlib import Path

from compact_storage_recipes import load_native
from recipe_outputs import item_outputs

OFF = {'neoforge:conditions': [{'type': 'neoforge:false'}]}
RETIRED = {'rustic_engineer:rustic_glider_chestplate',
           'rustic_engineer:rustic_glider_speed_chestplate', 'rustic_engineer:speed_booster'}


def apply(module, native):
    module = Path(module)
    resources = module / 'src/main/resources'
    effective = copy.deepcopy(native)
    for path in (resources / 'data').glob('*/recipe/**/*.json'):
        rel = path.relative_to(resources / 'data')
        effective[rel.parts[0] + ':' + '/'.join(rel.parts[2:])[:-5]] = json.loads(path.read_text(encoding='utf-8'))
    changed = []

    def write(relative, value):
        path = resources / relative
        text = json.dumps(value, indent=2, ensure_ascii=False) + '\n'
        if not path.exists() or path.read_text(encoding='utf-8') != text:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8', newline='\n')
            changed.append(relative)

    def recipe(rid, value):
        ns, name = rid.split(':', 1)
        write(f'data/{ns}/recipe/{name}.json', value)
        effective[rid] = copy.deepcopy(value)

    def conditions(ids):
        return [{'type': 'neoforge:mod_loaded', 'modid': ns} for ns in
                sorted({i.split(':')[0] for i in ids} - {'minecraft', 'bertieprogression', 'c'})]

    layouts = json.loads((module / 'texture-work/queued_052_layouts.json').read_text(encoding='utf-8'))
    desired_ids = {row['recipe'] for row in layouts}
    replacements = {r['result']['id'] for r in layouts if r['result']['id'] != 'minecraft:elytra'}
    manual = {'minecraft:crafting_shaped', 'minecraft:crafting_shapeless',
              'mekanism:mek_data', 'avaritia:shaped_table', 'create:mechanical_crafting', 'oritech:assembler'}
    for rid, value in list(effective.items()):
        outputs = item_outputs(value, set())
        if outputs & RETIRED or (rid not in desired_ids and outputs & replacements and value.get('type') in manual):
            recipe(rid, OFF)
    for row in layouts:
        value = {k: copy.deepcopy(row[k]) for k in ('type', 'pattern', 'key', 'result')}
        if row['type'] == 'avaritia:shaped_table':
            value['tier'] = row['tier']
        else:
            value['accept_mirrored'] = False
        items = [row['result']['id']]
        for ingredient in row['key'].values():
            items.extend(v['item'] for v in (ingredient if isinstance(ingredient, list) else [ingredient]))
        items.append('create:mechanical_crafter' if row['type'].startswith('create:') else 'avaritia:table')
        value['neoforge:conditions'] = conditions(items)
        recipe(row['recipe'], value)

    for rid, old, new in [
        ('mekanism:quantum_entangloporter', 'mekanism:pellet_plutonium', 'bertieprogression:titanium_sheet'),
    ]:
        value = copy.deepcopy(effective[rid])
        for ingredient in value['key'].values():
            if ingredient.get('item') == old:
                ingredient['item'] = new
        recipe(rid, value)
    value = copy.deepcopy(effective['mekanism:chemical_oxidizer'])
    for symbol, ingredient in value['key'].items():
        if ingredient == {'item': 'mekanism:personal_barrel'}:
            value['key'][symbol] = [{'item': 'mekanism:personal_barrel'}, {'item': 'mekanism:personal_chest'}]
    recipe('mekanism:chemical_oxidizer', value)

    rid = 'mekanism_extras:reaction/molten_thermonuclear_gasification/molten_thermonuclear'
    value = copy.deepcopy(effective[rid])
    value['item_input'] = {'item': 'iceandfire:dragonsteel_fire_ingot', 'count': value['item_input']['count']}
    value['neoforge:conditions'] = conditions(['mekanism:reaction', 'mekanism_extras:molten_thermonuclear',
                                             'iceandfire:dragonsteel_fire_ingot'])
    recipe(rid, value)
    recipe('anvilcraft:jewel_crafting/elytra', OFF)
    recipe('cataclysm:mechanical_fusion_anvil', OFF)
    recipe('bertieprogression:block_compress/mechanical_fusion_anvil', {
        'type': 'anvilcraft:block_compress',
        'inputs': [{'blocks': 'twilightforest:carminite_block'}, {'blocks': 'minecraft:anvil'}],
        'result': {'block': 'cataclysm:mechanical_fusion_anvil'},
        'neoforge:conditions': conditions(['anvilcraft:anvil', 'twilightforest:carminite_block',
                                         'cataclysm:mechanical_fusion_anvil'])})

    swap = {'oritech:augment/flight': 'oritech:augment/hpboostultimate',
            'oritech:augment/hpboostultimate': 'oritech:augment/flight'}
    for rid, original in native.items():
        if original.get('type') != 'oritech:augment_data':
            continue
        value = copy.deepcopy(effective.get(rid, original))
        source = native.get(swap.get(rid, rid), original)
        value['requirements'] = [swap.get(parent, parent) for parent in source['requirements']]
        value['uiX'], value['uiY'] = source['uiX'], source['uiY']
        if rid in swap or value['requirements'] != original['requirements']:
            recipe(rid, value)
    lang_path = resources / 'assets/oritech/lang/en_us.json'
    lang = json.loads(lang_path.read_text(encoding='utf-8')) if lang_path.exists() else {}
    lang['oritech.text.augment.autofeeder'] = 'Adaptive Immune System'
    lang['oritech.text.augment.autofeeder.desc'] = 'Deploys nanobots to protect from dangers of space.'
    lang['oritech.text.augment.autofeeder.desc.1'] = 'Prevents negative effects of vacuum'
    lang['oritech.text.augment.autofeeder.desc.2'] = ''
    write('assets/oritech/lang/en_us.json', lang)

    removed = set(json.loads((resources / 'removed_items.json').read_text(encoding='utf-8')))
    write('removed_items.json', sorted(removed | RETIRED))
    for relative in ['data/c/tags/item/hidden_from_recipe_viewers.json',
                     'data/emi/tag/exclusions/hidden.json']:
        path = resources / relative
        if path.exists():
            value = json.loads(path.read_text(encoding='utf-8'))
            if isinstance(value.get('values'), list):
                for item in sorted(RETIRED):
                    if not any(v == item or isinstance(v, dict) and v.get('id') == item for v in value['values']):
                        value['values'].append(item)
                write(relative, value)
    from mechanical_adapter_assets import apply as assets
    assets(module)
    return changed


if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--module', type=Path, required=True)
    p.add_argument('--mods', type=Path, required=True)
    args = p.parse_args()
    print(json.dumps({'changed': apply(args.module, load_native(args.mods))}))
