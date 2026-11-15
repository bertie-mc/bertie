"""Selected raw-metal yields, special scrap routes and captured chemical machines."""

import copy
import json
from pathlib import Path
from zipfile import ZipFile

from recipe_outputs import item_outputs
from tech_solar_recipes import DISABLED

RETIRED = {'twilightforest:charm_of_life_1', 'twilightforest:charm_of_life_2'}


def apply(module, native, mods):
    resources = Path(module) / 'src/main/resources'
    native = {(k if not k.startswith('data/') else k.split('/')[1] + ':' + '/'.join(k.split('/')[3:])[:-5]): v
              for k, v in native.items() if not k.startswith('data/') or '/recipe/' in k}
    effective = copy.deepcopy(native)
    for path in (resources / 'data').glob('*/recipe/**/*.json'):
        rel = path.relative_to(resources / 'data')
        effective[rel.parts[0] + ':' + Path(*rel.parts[2:]).with_suffix('').as_posix()] = json.loads(path.read_text(encoding='utf-8'))
    changed = []

    def write(name, value):
        path = resources / name
        text = json.dumps(value, indent=2, ensure_ascii=False) + '\n'
        if not path.exists() or path.read_text(encoding='utf-8') != text:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8', newline='\n')
            changed.append(name)

    def recipe(identifier, value):
        ns, name = identifier.split(':', 1)
        write(f'data/{ns}/recipe/{name}.json', value)

    def contains(value, identifiers):
        return bool(set(json.dumps(value).split('"')) & identifiers)

    def clean_refs(value):
        if isinstance(value, str):
            return None if value in RETIRED else value
        if isinstance(value, list):
            return [v for child in value if (v := clean_refs(child)) is not None]
        if isinstance(value, dict):
            if any(isinstance(value.get(k), str) and value[k] in RETIRED for k in ['item', 'id']):
                return None
            return {k: v for k, child in value.items() if (v := clean_refs(child)) is not None}
        return value

    for identifier, original in effective.items():
        value = copy.deepcopy(original)
        if 'neoforge:false' in json.dumps(value):
            continue
        kind = value.get('type', '')
        outputs = item_outputs(value, set())
        if contains(value, RETIRED):
            recipe(identifier, clean_refs(value) if kind.startswith('jeed:') else DISABLED)
            continue
        if kind == 'twilightdelight:frozen' or identifier == 'create:splashing/magma_block':
            recipe(identifier, DISABLED)
            continue
        if kind.startswith('oritech:centrifuge'):
            for result in value.get('results', []):
                if result.get('id', '').startswith('bertieprogression:') and result['id'].endswith('_spec'):
                    result['count'] = 3
        if kind in {'mekanism:purifying', 'mekanism:injecting'}:
            source = value.get('item_input', {})
            names = json.dumps(source)
            output = value.get('output', {}).get('id', '')
            if not any(x in names for x in ['mithril', 'netherite']) and ('clump' in output or 'shard' in output):
                if 'storage_blocks/raw_' in names:
                    source['count'] = 2 if kind == 'mekanism:purifying' else 1
                    value['output']['count'] = 45 if kind == 'mekanism:purifying' else 36
                elif 'raw_materials/' in names or ':raw_' in names:
                    source['count'] = 2 if kind == 'mekanism:purifying' else 1
                    value['output']['count'] = 5 if kind == 'mekanism:purifying' else 4
        if kind == 'oritech:pulverizer' and outputs & {
                'mekanism:dirty_netherite_scrap', 'bertieprogression:dirty_mithril_scrap'}:
            value = DISABLED
        if value != original:
            recipe(identifier, value)

    special = copy.deepcopy(effective['forbidden_arcanus:clibano_combustion/netherite_scrap_from_clibano_combustion'])
    special['residue']['chance'] = 0.15
    recipe('forbidden_arcanus:clibano_combustion/netherite_scrap_from_clibano_combustion', special)
    for kind, source, output, amount in [
            ('crushing', 'irons_spellbooks:raw_mithril', 'bertieprogression:dirty_mithril_scrap', 3),
            ('enriching', 'bertieprogression:dirty_mithril_scrap', 'irons_spellbooks:mithril_scrap', 1)]:
        recipe('bertieprogression:' + kind + '/mithril_scrap', {
            'type': 'mekanism:' + kind, 'input': {'count': 1, 'item': source},
            'output': {'id': output, 'count': amount},
            'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': x} for x in ['mekanism', 'irons_spellbooks']]})

    def shaped(identifier, output, pattern, key, tier=None, count=1, kind=None):
        for existing, value in effective.items():
            if existing != identifier and output in item_outputs(value, set()):
                recipe(existing, DISABLED)
        keys = {k: v if isinstance(v, dict) else {'item': v} for k, v in key.items()}
        value = {'type': kind or ('minecraft:crafting_shaped' if tier is None else 'avaritia:shaped_table'),
                 'pattern': pattern, 'key': keys, 'result': {'id': output, 'count': count}}
        namespaces = {output.split(':')[0]} | {v['item'].split(':')[0] for v in keys.values() if 'item' in v}
        if tier is not None:
            value['tier'] = tier
            namespaces.add('avaritia')
        value['neoforge:conditions'] = [{'type': 'neoforge:mod_loaded', 'modid': x}
                                       for x in sorted(namespaces - {'minecraft', 'bertieprogression'})]
        recipe(identifier, value)

    casing = 'mekanism:steel_casing'
    ace = 'oritech:advanced_computing_engine'
    shaped('mekanism:energized_smelter', 'mekanism:energized_smelter',
           ['AAAAA', 'ABCBA', 'ABDBA', 'EFGFE', 'EEEEE'],
           dict(A='bertieprogression:steel_sheet', B='anvilcraft:ember_metal_ingot', C=ace, D=casing,
                E='bertieprogression:deep_alloy_sheet', F='oritech:magnetic_coil', G='oritech:motor'), tier=2)
    shaped('mekanism:crusher', 'mekanism:crusher',
           ['AAAAA', 'ABCBA', 'ABDBA', 'EFGFE', 'EEAEE'],
           dict(A='bertieprogression:steel_sheet', B='anvilcraft:titanium_ingot', C='anvilcraft:earth_core_shard_block',
                D=casing, E='bertieprogression:deep_alloy_sheet', F='oritech:motor', G=ace), tier=2)
    shaped('mekanism:chemical_infuser', 'mekanism:chemical_infuser',
           ['ABB BBA', 'ABBCBBA', 'ACDDDCA', 'AEEFEEA', 'ACGGGCA', 'DHHCHHD', 'DDDDDDD'],
           dict(A='bertieprogression:osmium_sheet', B='bertieprogression:titanium_sheet', C='oritech:carbon_fibre_strands',
                D='bertieprogression:steel_sheet', E=ace, F=casing, G='mekanism:elite_chemical_tank', H='oritech:motor'), tier=3)
    shaped('mekanism:chemical_injection_chamber', 'mekanism:chemical_injection_chamber',
           ['AAAAA', 'ABCBA', 'ADEDA', 'FGHGF', 'FFFFF'],
           dict(A='bertieprogression:osmium_sheet', B='oritech:super_ai_chip', C='mekanism_extras:absolute_control_circuit',
                D='mekanism_extras:alloy_radiance', E='mekanism:purification_chamber', F='bertieprogression:deep_alloy_sheet',
                G='bertieprogression:platinum_coil', H='oritech:motor'), tier=2)
    shaped('mekanism:thermal_evaporation/block', 'mekanism:thermal_evaporation_block',
           ['ABA', 'B B', 'ABA'], dict(A='bertieprogression:steel_rod', B='bertieprogression:copper_sheet'), count=4)
    shaped('mekanism:thermal_evaporation/valve', 'mekanism:thermal_evaporation_valve',
           ['ABA', 'BCB', 'ABA'], dict(A='mekanism:thermal_evaporation_block', B='oritech:carbon_fibre_strands',
                                    C='mekanism:advanced_control_circuit'))
    shaped('mekanism:thermal_evaporation/controller', 'mekanism:thermal_evaporation_controller',
           ['ABABA', 'BCDCB', 'AEFEA', 'BCECB', 'ABABA'],
           dict(A='mekanism:alloy_infused', B='bertieprogression:lead_sheet', C='mekanism:thermal_evaporation_block',
                D=ace, E='bertieprogression:steel_rod', F='pastel:onyx_glass'), tier=2)
    shaped('oritech:crafting/cooler', 'oritech:cooler_block',
           ['AABBB', 'AABCB', 'DEFGB', 'DHHHB', 'DDDDD'],
           dict(A='minecraft:glass', B='bertieprogression:bronze_sheet', C='oritech:magnetic_coil',
                D='bertieprogression:deep_alloy_sheet', E='oritech:processing_unit', F=casing,
                G='oritech:motor', H='anvilcraft:frost_metal_ingot'), tier=2)
    shaped('mekanism:seismic_reader', 'mekanism:seismic_reader', ['TST', 'TCT', 'TTT'],
           dict(T={'tag': 'c:ingots/tin'}, S='anvilcraft:earth_core_shard', C='anvilcraft:capacitor_empty'), kind='mekanism:mek_data')
    vibrator = copy.deepcopy(effective['mekanism:seismic_vibrator'])
    vibrator['key']['#'] = {'item': 'anvilcraft:earth_core_shard'}
    recipe('mekanism:seismic_vibrator', vibrator)

    for provider in ['create', 'anvilcraft']:
        recipe('bertieprogression:' + provider + '/lead_sheet', {
            'type': provider + (':pressing' if provider == 'create' else ':stamping'),
            'ingredients': [{'item' if provider == 'create' else 'items': 'anvilcraft:lead_ingot'}],
            'results': [{'id': 'bertieprogression:lead_sheet', 'count': 1}],
            'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': x}
                                   for x in dict.fromkeys([provider, 'anvilcraft'])]})
    recipe('bertieprogression:stamper/lead_sheet', {
        'type': 'mekmm:stamper', 'input': {'count': 1, 'item': 'berlordscarving:lead_big_slate'},
        'mold': {'count': 1, 'item': 'bertieprogression:lead_sheet'},
        'output': {'id': 'bertieprogression:lead_sheet', 'count': 8},
        'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': x} for x in ['mekmm', 'berlordscarving']]})
    write('data/c/tags/item/plates/lead.json', {'replace': False, 'values': ['bertieprogression:lead_sheet']})
    path = 'data/c/tags/item/plates.json'
    plates = json.loads((resources / path).read_text(encoding='utf-8'))
    if 'bertieprogression:lead_sheet' not in plates['values']:
        plates['values'].append('bertieprogression:lead_sheet')
    write(path, plates)
    for name, title in [('lead_sheet', 'Lead Sheet'), ('dirty_mithril_scrap', 'Dirty Mithril Scrap')]:
        write('assets/bertieprogression/models/item/' + name + '.json', {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': 'bertieprogression:item/' + name}})
    lang = json.loads((resources / 'assets/bertieprogression/lang/en_us.json').read_text(encoding='utf-8'))
    lang.update({'item.bertieprogression.lead_sheet': 'Lead Sheet', 'item.bertieprogression.dirty_mithril_scrap': 'Dirty Mithril Scrap'})
    write('assets/bertieprogression/lang/en_us.json', lang)

    def clean_loot(value):
        if isinstance(value, dict):
            if value.get('type') == 'minecraft:item' and value.get('name') in RETIRED:
                return None
            result = {k: v for k, child in value.items() if (v := clean_loot(child)) is not None}
            if ('entries' in result and not result['entries']) or (result.get('type') in {
                    'minecraft:alternatives', 'minecraft:sequence', 'minecraft:group'} and not result.get('children')):
                return None
            return result
        if isinstance(value, list):
            return [v for child in value if (v := clean_loot(child)) is not None]
        return value

    sources = {}
    for jar in sorted(Path(mods).glob('*.jar')):
        if jar.name.startswith('bertieprogression'):
            continue
        with ZipFile(jar) as archive:
            for name in archive.namelist():
                if name.startswith('data/') and name.endswith('.json') and any(x in name for x in ['/loot_table/', '/tags/', '/museumexhibits/']):
                    raw = archive.read(name)
                    if any(item.encode() in raw for item in RETIRED):
                        sources[name] = json.loads(raw)
    for path in (resources / 'data').rglob('*.json'):
        name = path.relative_to(resources).as_posix()
        if name in sources or any(x in name for x in ['/loot_table/', '/tags/', '/museumexhibits/']):
            raw = path.read_text(encoding='utf-8')
            if name in sources or contains(json.loads(raw), RETIRED):
                sources[name] = json.loads(raw)
    for name, value in sources.items():
        if not contains(value, RETIRED) or name.endswith('/hidden_from_recipe_viewers.json'):
            continue
        if '/tags/' in name:
            value['values'] = [v for v in value.get('values', []) if (v if isinstance(v, str) else v.get('id')) not in RETIRED]
            removals = value.setdefault('remove', [])
            for item in sorted(RETIRED):
                entry = {'id': item, 'required': False}
                if entry not in removals:
                    removals.append(entry)
        elif '/loot_table/' in name:
            value = clean_loot(value) or {'pools': []}
        else:
            value = clean_refs(value)
        write(name, value)
    removed = set(json.loads((resources / 'removed_items.json').read_text(encoding='utf-8')))
    write('removed_items.json', sorted(removed | RETIRED))
    path = 'data/c/tags/item/hidden_from_recipe_viewers.json'
    hidden = json.loads((resources / path).read_text(encoding='utf-8'))
    identifiers = {v if isinstance(v, str) else v['id'] for v in hidden['values']}
    hidden['values'].extend({'id': item, 'required': False} for item in sorted(RETIRED - identifiers))
    write(path, hidden)
    write('data/twilightforest/advancement/recipes/tools/charm_of_life_2.json', {
        'neoforge:conditions': [{'type': 'neoforge:false'}], 'criteria': {'disabled': {'trigger': 'minecraft:impossible'}}})
    metadata = json.loads((resources / 'ore_materials.json').read_text(encoding='utf-8'))
    metadata['raw_yields'] = 'enrichment 1 raw -> 2 Dust; purification 2 raw -> 5 Clumps; injection 1 raw -> 4 Shards; native dissolution ratios'
    metadata['centrifuge']['spec_count'] = 3
    write('ore_materials.json', metadata)
    return changed
