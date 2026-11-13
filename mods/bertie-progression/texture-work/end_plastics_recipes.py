"""End progression recipes, plastic form order, and retired End materials."""

import copy
import json
from pathlib import Path
from zipfile import ZipFile

from tech_solar_recipes import DISABLED, replace

RETIRED = {
    'betterend:ender_ore', 'betterend:ender_shard', 'endersdelight:ender_shard',
    *('betterend:' + name + '_forged_plate' for name in ['thallasium', 'terminite', 'aeternium']),
}
SHARDS = {'betterend:ender_shard', 'endersdelight:ender_shard'}


def apply(module, native, mods):
    resources = Path(module) / 'src/main/resources'
    recipes = {(key if not key.startswith('data/') else key.split('/')[1] + ':' + '/'.join(key.split('/')[3:])[:-5]): value
               for key, value in native.items() if not key.startswith('data/') or '/recipe/' in key}
    changed = []

    def write(path, value):
        target = resources / path
        text = json.dumps(value, indent=2, ensure_ascii=False) + '\n'
        if not target.exists() or target.read_text(encoding='utf-8') != text:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text, encoding='utf-8', newline='\n')
            changed.append(path)

    def recipe(identifier, value):
        namespace, name = identifier.split(':', 1)
        write(f'data/{namespace}/recipe/{name}.json', value)

    def shaped(identifier, output, pattern, key, tier=None):
        value = {'type': 'minecraft:crafting_shaped' if tier is None else 'avaritia:shaped_table',
                 'pattern': pattern, 'key': {k: {'item': v} for k, v in key.items()},
                 'result': {'id': output, 'count': 1}}
        if tier is not None:
            value['tier'] = tier
        recipe(identifier, value)

    def effective():
        result = copy.deepcopy(recipes)
        for path in (resources / 'data').glob('*/recipe/**/*.json'):
            rel = path.relative_to(resources / 'data')
            result[rel.parts[0] + ':' + Path(*rel.parts[2:]).with_suffix('').as_posix()] = json.loads(path.read_text(encoding='utf-8'))
        return result

    shaped('mekanism:hdpe_stick', 'mekanism:hdpe_stick', ['P', 'P'], {'P': 'mekanism:hdpe_pellet'})
    shaped('mekanism:hdpe_rod', 'mekanism:hdpe_rod', ['S', 'S', 'S'], {'S': 'mekanism:hdpe_stick'})
    lathe = effective()['bertieprogression:lathe/plastick']
    lathe['input'] = {'count': 1, 'item': 'mekanism:hdpe_stick'}
    lathe['output'] = {'id': 'mekanism:hdpe_rod', 'count': 1}
    recipe('bertieprogression:lathe/plastick', lathe)
    shaped('oritech:crafting/droneport', 'oritech:drone_port_block',
           ['PBBBP', 'BTUTB', 'BSKSB', 'BMCMB', 'PBBBP'], {
               'P': 'create:propeller', 'B': 'bertieprogression:bronze_sheet', 'T': 'oritech:basic_battery',
               'U': 'oritech:unholy_intelligence', 'S': 'oritech:superconductor', 'K': 'mekanism:steel_casing',
               'M': 'oritech:motor', 'C': 'mekanism:ultimate_control_circuit'}, 2)
    # Disable any other native Drone Port crafting route independently of its recipe ID.
    for key, value in effective().items():
        result = value.get('result')
        if key != 'oritech:crafting/droneport' and isinstance(result, dict) and result.get('id') == 'oritech:drone_port_block':
            recipe(key, DISABLED)
    shaped('mekanismgenerators:generator/gas_burning', 'mekanismgenerators:gas_burning_generator',
           [' OOO ', 'RAAAR', 'REKER', 'RMMMR', 'DDDDD'], {
               'O': 'bertieprogression:osmium_sheet', 'A': 'mekanism:alloy_infused', 'R': 'mekanism:hdpe_rod',
               'E': 'mekanism:electrolytic_core', 'K': 'mekanism:steel_casing',
               'M': 'oritech:magnetic_coil', 'D': 'bertieprogression:deep_alloy_sheet'}, 2)
    recipe('oritech:foundry/alloy/netherite', DISABLED)
    shaped('bertieprogression:sculk/bedrock', 'minecraft:bedrock', ['DDD', 'DDD', 'DDD'],
           {'D': 'anvilcraft:bedrock_dust_block'}, 1)
    recipe('bertieprogression:infusion/raven_enchanting_table', {
        'type': 'apothic_enchanting:infusion', 'input': {'item': 'apothic_enchanting:apothic_enchanting_table'},
        'requirements': {'eterna': 99, 'quanta': 99.5, 'arcana': 1.25},
        'max_requirements': {'eterna': 99, 'quanta': 99.5, 'arcana': 1.25},
        'result': {'id': 'apothic_enchanting:raven_enchanting_table', 'count': 1}})
    recipe('anvilcraft:recovery_pearl', DISABLED)
    write('data/bertieprogression/forbidden_arcanus/hephaestus_forge/ritual/recovery_pearl.json', {
        'essences': {'blood': 10000, 'souls': 1},
        'inputs': [{'amount': 4, 'ingredient': {'item': item}}
                   for item in ['l2complements:totemic_gold_ingot', 'minecraft:echo_shard']],
        'magic_circle': 'forbidden_arcanus:create_item', 'main_ingredient': {'item': 'minecraft:ender_pearl'},
        'result': {'type': 'forbidden_arcanus:create_item',
                   'result_item': {'id': 'anvilcraft:recovery_pearl', 'count': 1}}, 'forge_tier': 1})

    for key, value in effective().items():
        if not SHARDS.intersection(json.dumps(value).split('"')) or 'neoforge:false' in json.dumps(value):
            continue
        if value.get('type') == 'betterend:infusion':
            recipe(key, replace(value, {item: 'minecraft:ender_pearl' for item in SHARDS}))
        elif value.get('type') == 'farmersdelight:cooking':
            def cooking(child):
                if isinstance(child, dict) and child.get('item') in SHARDS:
                    return [{'item': 'minecraft:ender_pearl'}, {'item': 'anvilcraft:ender_pearl_fragment'}]
                if isinstance(child, dict):
                    return {k: cooking(v) for k, v in child.items()}
                if isinstance(child, list):
                    return [cooking(v) for v in child]
                return child
            recipe(key, cooking(value))
    for key, value in effective().items():
        if RETIRED.intersection(json.dumps(value).split('"')):
            recipe(key, DISABLED)

    def loot(value):
        if isinstance(value, dict):
            if value.get('type') == 'minecraft:item' and value.get('name') in RETIRED:
                return None
            result = {key: cleaned for key, child in value.items() if (cleaned := loot(child)) is not None}
            if 'entries' in result and not result['entries']:
                return None
            if result.get('type') in {'minecraft:alternatives', 'minecraft:sequence', 'minecraft:group'} and not result.get('children'):
                return None
            return result
        if isinstance(value, list):
            return [cleaned for child in value if (cleaned := loot(child)) is not None]
        return value

    sources = {}
    for jar in Path(mods).glob('*.jar'):
        if not jar.name.lower().startswith(('betterend', 'endersdelight', 'ends_delight')):
            continue
        with ZipFile(jar) as archive:
            for name in archive.namelist():
                if name.startswith('data/') and name.endswith('.json') and ('/loot_table' in name or '/tags/' in name):
                    sources[name] = json.loads(archive.read(name))
    for path in (resources / 'data').rglob('*.json'):
        name = path.relative_to(resources).as_posix()
        if '/loot_table' in name or '/tags/' in name:
            sources[name] = json.loads(path.read_text(encoding='utf-8'))
    for name, value in sources.items():
        if not RETIRED.intersection(json.dumps(value).split('"')):
            continue
        if '/tags/' in name:
            value['replace'] = True
            value['values'] = [v for v in value.get('values', []) if (v if isinstance(v, str) else v.get('id')) not in RETIRED]
        else:
            value = loot(value) or {'pools': []}
        write(name, value)
    write('data/betterend/loot_table/blocks/ender_ore.json', {'type': 'minecraft:block', 'pools': []})
    write('data/betterend/worldgen/placed_feature/ender_ore.json',
          {'feature': {'type': 'minecraft:no_op', 'config': {}}, 'placement': []})
    removed = set(json.loads((resources / 'removed_items.json').read_text(encoding='utf-8')))
    write('removed_items.json', sorted(removed | RETIRED))
    hidden_path = 'data/c/tags/item/hidden_from_recipe_viewers.json'
    hidden = json.loads((resources / hidden_path).read_text(encoding='utf-8'))
    present = {v if isinstance(v, str) else v.get('id') for v in hidden['values']}
    hidden['values'].extend({'id': item, 'required': False} for item in sorted(RETIRED - present))
    write(hidden_path, hidden)
    language = 'assets/bertieprogression/lang/en_us.json'
    lang = json.loads((resources / language).read_text(encoding='utf-8'))
    lang['block.bertieprogression.eezo_ore'] = 'Eezo Ore'
    write(language, lang)
    return changed
