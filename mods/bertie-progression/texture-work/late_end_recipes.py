"""Selected late-End crafting, forge rituals and retired storage components."""

import copy
import json
from pathlib import Path
from zipfile import ZipFile

from recipe_outputs import item_outputs
from tech_solar_recipes import DISABLED, replace

TIERS = {'mekanism': ['basic', 'advanced', 'elite', 'ultimate'],
         'mekanism_extras': ['absolute', 'supreme', 'cosmic', 'infinite']}
RETIRED = {'quark:gravisand', 'mekanism:induction_casing', 'mekanism:induction_port',
           'mekanism_extras:reinforced_induction_casing', 'mekanism_extras:reinforced_induction_port'}
RETIRED |= {ns + ':' + tier + '_induction_' + kind for ns, tiers in TIERS.items()
            for tier in tiers for kind in ['cell', 'provider']}
ENERGY = {ns + ':' + tier + '_induction_' + kind: ns + ':' + tier + '_energy_cube'
          for ns, tiers in TIERS.items() for tier in tiers for kind in ['cell', 'provider']}
RESTORED = {'cataclysm:enderite_ingot', 'cataclysm:enderite_block', 'bertieprogression:titanium_sheet'}


def apply(module, native, mods):
    resources = Path(module) / 'src/main/resources'
    native = {(k if not k.startswith('data/') else k.split('/')[1] + ':' + '/'.join(k.split('/')[3:])[:-5]): v
              for k, v in native.items() if not k.startswith('data/') or '/recipe/' in k}
    changed = []

    def write(path, value):
        target = resources / path
        text = json.dumps(value, indent=2, ensure_ascii=False) + '\n'
        if not target.exists() or target.read_text(encoding='utf-8') != text:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text, encoding='utf-8', newline='\n')
            changed.append(path)

    def recipe(key, value):
        ns, name = key.split(':', 1)
        write(f'data/{ns}/recipe/{name}.json', value)

    def effective():
        result = copy.deepcopy(native)
        for p in (resources / 'data').glob('*/recipe/**/*.json'):
            rel = p.relative_to(resources / 'data')
            result[rel.parts[0] + ':' + Path(*rel.parts[2:]).with_suffix('').as_posix()] = json.loads(p.read_text(encoding='utf-8'))
        return result

    def shaped(key, output, pattern, ingredients, tier=2, count=1):
        value = {'type': 'minecraft:crafting_shaped' if tier is None else 'avaritia:shaped_table',
                 'pattern': pattern, 'key': {k: {'item': v} for k, v in ingredients.items()},
                 'result': {'id': output, 'count': count}}
        if tier is not None:
            value['tier'] = tier
        recipe(key, value)

    def ritual(name, output, primary, inputs, essences, count=1):
        write('data/bertieprogression/forbidden_arcanus/hephaestus_forge/ritual/' + name + '.json', {
            'forge_tier': 3, 'essences': essences, 'main_ingredient': {'item': primary},
            'inputs': [{'amount': amount, 'ingredient': {'item': item}} for item, amount in inputs],
            'magic_circle': 'forbidden_arcanus:create_item',
            'result': {'type': 'forbidden_arcanus:create_item', 'result_item': {'id': output, 'count': count}}})

    for key, value in effective().items():
        if 'neoforge:false' in json.dumps(value):
            continue
        outputs = item_outputs(value, set())
        if outputs & (RETIRED | {'pastel:perturbed_eye', 'cataclysm:abyssal_sacrifice'}):
            recipe(key, DISABLED)
        elif RETIRED.intersection(json.dumps(value).split('"')):
            updated = replace(value, ENERGY)
            assert not RETIRED.intersection(json.dumps(updated).split('"')), ('unresolved retired input', key)
            recipe(key, updated)
    ritual('perturbed_eye', 'pastel:perturbed_eye', 'pastel:bottle_of_ruin',
           [('minecraft:ender_eye', 6), ('anvilcraft:levitation_powder', 6)],
           {'souls': 12, 'experience': 1200}, 12)
    ritual('abyssal_sacrifice', 'cataclysm:abyssal_sacrifice', 'deeperdarker:heart_of_the_deep', [
        ('alexsmobs:void_worm_mandible', 1), ('minecraft:heart_of_the_sea', 1),
        ('cataclysm:crystallized_coral', 1), ('cataclysm:coral_chunk', 1), ('minecraft:chorus_flower', 1),
        ('oritech:unholy_intelligence', 1), ('curseofpandora:evil_spine', 1), ('curseofpandora:evil_stone', 1),
        ('cataclysm:blessed_amethyst_crab_meat', 1), ('lendersdelight:improved_dog_food', 3)],
        {'blood': 30000, 'souls': 100})
    shaped('refinedstorage:wireless_grid', 'refinedstorage:wireless_grid', ['ETE', 'UKU', 'RFR'], {
        'E': 'deeperdarker:reinforced_echo_shard', 'T': 'mekanism:teleportation_core',
        'U': 'oritech:unholy_intelligence', 'K': 'refinedstorage:machine_casing',
        'R': 'mekanism:hdpe_rod', 'F': 'oritech:carbon_fibre_strands'}, 1)
    shaped('avaritia:end_crafting_table', 'avaritia:end_crafting_table', ['IPPPI', 'CLLLC', 'CBTAC', 'CWOVC', 'ISSSI'], {
        'I': 'apothic_enchanting:infused_breath', 'P': 'minecraft:end_portal_frame', 'C': 'minecraft:crying_obsidian',
        'L': 'anvilcraft:levitation_powder_block', 'B': 'minecraft:bedrock',
        'T': 'avaritia:double_compressed_crafting_table', 'A': 'cataclysm:abyssal_egg',
        'W': 'alexsmobs:void_worm_eye', 'O': 'oritech:overcharged_crystal',
        'V': 'l2complements:void_eye', 'S': 'l2complements:shulkerate_block'})
    frame = effective()['minecraft:end_portal_frame']
    recipe('minecraft:end_portal_frame', replace(frame, {
        'minecraft:end_crystal': 'pastel:cracked_end_portal_frame', 'minecraft:emerald_block': 'pastel:malachite_block',
        'avaritia:crystal_matrix_ingot': 'magitech:ender_metal_ingot'}))
    shaped('mekanism:dimensional_stabilizer', 'mekanism:dimensional_stabilizer',
           ['VPPVV', 'VEHFP', 'PEKFP', 'PEBFV', 'VVPPV'], {
        'V': 'anvilcraft:void_matter', 'P': 'mekanism:hdpe_sheet', 'E': 'anvilcraft:ember_metal_ingot',
        'F': 'anvilcraft:frost_metal_ingot', 'H': 'minecraft:heavy_core', 'K': 'mekanism:steel_casing',
        'B': 'anvilcraft:earth_core_shard_block'})
    shaped('mekanism:precision_sawmill', 'mekanism:precision_sawmill', ['SSSSS', 'SABAS', 'SWKWS', 'DMCMD', 'DDDDD'], {
        'S': 'bertieprogression:steel_sheet', 'D': 'bertieprogression:deep_alloy_sheet', 'A': 'mekanism:dust_diamond',
        'B': 'anvilcraft:circuit_board', 'W': 'create:mechanical_saw', 'K': 'mekanism:steel_casing',
        'M': 'oritech:motor', 'C': 'mekanism:basic_control_circuit'})
    shaped('bertieprogression:storage/enderite_block', 'cataclysm:enderite_block', ['III'] * 3,
           {'I': 'cataclysm:enderite_ingot'}, None)
    recipe('bertieprogression:storage/enderite_ingot', {'type': 'minecraft:crafting_shapeless',
           'ingredients': [{'item': 'cataclysm:enderite_block'}], 'result': {'id': 'cataclysm:enderite_ingot', 'count': 9}})
    for key, kind in [('pressing', 'create:pressing'), ('anvilcraft/stamping', 'anvilcraft:stamping')]:
        recipe('bertieprogression:' + key + '/titanium_sheet', {
            'type': kind, 'ingredients': [{'item' if kind == 'create:pressing' else 'items': 'anvilcraft:titanium_ingot'}],
            'results': [{'id': 'bertieprogression:titanium_sheet', 'count': 1}]})
    recipe('bertieprogression:stamper/titanium_sheet', {'type': 'mekmm:stamper',
           'input': {'count': 1, 'item': 'anvilcraft:titanium_ingot'},
           'mold': {'count': 1, 'item': 'bertieprogression:titanium_sheet'},
           'output': {'id': 'bertieprogression:titanium_sheet', 'count': 1}})
    write('assets/bertieprogression/models/item/titanium_sheet.json',
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'bertieprogression:item/titanium_sheet'}})
    lang = 'assets/bertieprogression/lang/en_us.json'
    values = json.loads((resources / lang).read_text(encoding='utf-8'));values['item.bertieprogression.titanium_sheet'] = 'Titanium Sheet'
    write(lang, values)
    write('data/c/tags/item/plates/titanium.json', {'replace': False, 'values': ['bertieprogression:titanium_sheet']})
    plates = 'data/c/tags/item/plates.json';values = json.loads((resources / plates).read_text(encoding='utf-8'))
    if 'bertieprogression:titanium_sheet' not in values['values']:
        values['values'].append('bertieprogression:titanium_sheet')
    write(plates, values)
    raven = effective()['bertieprogression:infusion/raven_enchanting_table']
    raven['requirements']['arcana'] = 1.2;raven['max_requirements']['arcana'] = 1.3
    recipe('bertieprogression:infusion/raven_enchanting_table', raven)

    def clean(value):
        if isinstance(value, dict):
            if value.get('type') == 'minecraft:item' and value.get('name') in RETIRED:
                return None
            result = {k: v for k, child in value.items() if (v := clean(child)) is not None}
            if 'entries' in result and not result['entries']:
                return None
            return result
        if isinstance(value, list):
            return [v for child in value if (v := clean(child)) is not None]
        return value

    sources = {}
    for jar in Path(mods).glob('*.jar'):
        if not jar.name.lower().startswith(('mekanism', 'quark')):
            continue
        with ZipFile(jar) as z:
            for name in z.namelist():
                if name.startswith('data/') and name.endswith('.json') and ('/loot_table' in name or '/tags/' in name):
                    sources[name] = json.loads(z.read(name))
    for p in (resources / 'data').rglob('*.json'):
        name = p.relative_to(resources).as_posix()
        if '/loot_table' in name or '/tags/' in name:
            sources[name] = json.loads(p.read_text(encoding='utf-8'))
    for name, value in sources.items():
        if not RETIRED.intersection(json.dumps(value).split('"')):
            continue
        if '/tags/' in name:
            value['replace'] = True
            value['values'] = [v for v in value.get('values', []) if (v if isinstance(v, str) else v.get('id')) not in RETIRED]
        else:
            value = clean(value) or {'pools': []}
        write(name, value)
    for item in sorted(RETIRED):
        ns, name = item.split(':');write(f'data/{ns}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': []})
    values = set(json.loads((resources / 'removed_items.json').read_text(encoding='utf-8')))
    write('removed_items.json', sorted((values | RETIRED) - RESTORED))
    hidden = 'data/c/tags/item/hidden_from_recipe_viewers.json';values = json.loads((resources / hidden).read_text(encoding='utf-8'))
    values['values'] = [v for v in values['values'] if (v if isinstance(v, str) else v.get('id')) not in RESTORED]
    present = {v if isinstance(v, str) else v.get('id') for v in values['values']}
    values['values'].extend({'id': item, 'required': False} for item in sorted(RETIRED - present));write(hidden, values)
    return changed
