"""Selected computing, machine, material and wireless progression recipes."""

import copy
import json
from pathlib import Path

from recipe_outputs import item_outputs
from tech_solar_recipes import DISABLED

BLOCK_FLOODER_RECIPE = {
    'type': 'malum:spirit_infusion',
    'input': {'count': 1, 'item': 'pastel:mermaids_gem'},
    'extraInputs': [{'count': 16, 'tag': 'minecraft:dirt'}, {'count': 16, 'tag': 'c:stones'}],
    'spirits': [{'type': 'malum:earthen', 'count': 3}, {'type': 'malum:aqueous', 'count': 2}],
    'result': {'id': 'pastel:block_flooder', 'count': 4},
    'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': mod} for mod in ['pastel', 'malum']],
}


def apply(module, native):
    resources = Path(module) / 'src/main/resources'
    effective = copy.deepcopy(native)
    for path in (resources / 'data').glob('*/recipe/**/*.json'):
        rel = path.relative_to(resources / 'data')
        effective[rel.parts[0] + ':' + Path(*rel.parts[2:]).with_suffix('').as_posix()] = json.loads(path.read_text())
    changed = []

    def write(path, value):
        target = resources / path
        text = json.dumps(value, indent=2, ensure_ascii=False) + '\n'
        if not target.exists() or target.read_text(encoding='utf-8') != text:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text, encoding='utf-8', newline='\n')
            changed.append(path)

    def recipe(identifier, value):
        ns, name = identifier.split(':', 1)
        write(f'data/{ns}/recipe/{name}.json', value)

    def shaped(identifier, output, pattern, key, tier=None, count=1):
        value = {'type': 'minecraft:crafting_shaped' if tier is None else 'avaritia:shaped_table',
                 'pattern': pattern, 'key': {k: v if isinstance(v, dict) else {'item': v} for k, v in key.items()},
                 'result': {'id': output, 'count': count}}
        if tier is not None:
            value['tier'] = tier
        namespaces = {output.split(':')[0]}
        namespaces.update(v['item'].split(':')[0] for v in value['key'].values() if 'item' in v)
        if tier is not None:
            namespaces.add('avaritia')
        value['neoforge:conditions'] = [{'type': 'neoforge:mod_loaded', 'modid': ns}
                                       for ns in sorted(namespaces - {'minecraft', 'bertieprogression'})]
        recipe(identifier, value)

    ace = 'oritech:advanced_computing_engine'
    for identifier, value in effective.items():
        if ace in item_outputs(value, set()) and not identifier.startswith('bertieprogression:atomic_forge/ace_stage_'):
            recipe(identifier, DISABLED)
    previous = 'oritech:processing_unit'
    for index, circuit in enumerate(['basic', 'advanced', 'elite', 'ultimate'], 1):
        output = f'bertieprogression:advanced_computing_engine_stage_{index}' if index < 4 else ace
        recipe(f'bertieprogression:atomic_forge/ace_stage_{index}', {
            'type': 'oritech:atomic_forge',
            'ingredients': [{'item': previous}, {'item': f'mekanism:{circuit}_control_circuit'},
                            {'item': 'oritech:silicon_wafer'}],
            'results': [{'id': output, 'count': 1}], 'time': 5,
            'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': m} for m in ['oritech', 'mekanism']]})
        previous = output
    for identifier in ['oritech:atomicforge/urandust', 'oritech:atomicforge/urandustgem']:
        recipe(identifier, DISABLED)

    shaped('oritech:crafting/atomicforge', 'oritech:atomic_forge_block',
           ['AAAAAAA', 'ABBBCCA', 'ABDEDCA', 'ABFGFCA', 'ACDHDIA', 'ACIIIIA', 'AAAAAAA'],
           dict(A='bertieprogression:bronze_sheet', B='oritech:carbon_fibre_strands',
                C='bertieprogression:titanium_sheet', D='oritech:unholy_intelligence',
                E='oritech:flux_gate', F='oritech:enderic_lens', G='mekanism:steel_casing',
                H='mekanism:ultimate_control_circuit', I='oritech:superconductor'), tier=3)
    for machine, form in [('cnc_stamper', 'sheet'), ('cnc_lathe', 'rod'), ('cnc_rolling_mill', 'wire')]:
        shaped('mekmm:' + machine, 'mekmm:' + machine,
               ['TTTTT', 'TLXLT', 'TMKMT', 'RCACR', 'RRRRR'],
               dict(T='bertieprogression:titanium_sheet', L='oritech:enderic_lens',
                    X='bertieprogression:copper_' + form, M='oritech:motor', K='mekanism:steel_casing',
                    R='bertieprogression:royal_steel_sheet', C='refinedstorage:construction_core', A=ace), tier=2)

    key = dict(S='bertieprogression:steel_sheet', D='bertieprogression:deep_alloy_sheet',
               A='mekanism_extras:alloy_thermonuclear', B='anvilcraft:circuit_board',
               C='mekanism:advanced_control_circuit', G='anvilcraft:magnet_ingot',
               K='mekanism:steel_casing', M='oritech:motor', P='mekanism:hdpe_sheet',
               R='mekanism:hdpe_rod', T='mekanism:basic_chemical_tank', Y='mekanism:dye_base')
    for machine, pattern in [('pigment_extractor', ['SSSSS', 'SABAS', 'SMKTS', 'DGPGD', 'DDDDD']),
                             ('pigment_mixer', ['SSSSS', 'SMAMS', 'STKTS', 'DRBRD', 'DDDDD']),
                             ('painting_machine', ['SSSSS', 'SCBCS', 'SRKRS', 'DYMYD', 'DDDDD'])]:
        used = set(''.join(pattern))
        shaped('mekanism:' + machine, 'mekanism:' + machine, pattern,
               {symbol: value for symbol, value in key.items() if symbol in used}, tier=2)

    shaped('l2complements:craft/shulkerate_ingot', 'l2complements:shulkerate_ingot', ['SSS', 'DBD', 'SSS'],
           dict(S='minecraft:shulker_shell', D='oritech:duratium_ingot', B='l2complements:captured_shulker_bullet'))
    shaped('irons_spellbooks:mithril_ingot_from_scrap', 'irons_spellbooks:mithril_ingot', ['MMM', 'IMI', 'MMM'],
           dict(M='irons_spellbooks:mithril_scrap', I='irons_spellbooks:arcane_ingot'))
    recipe('irons_spellbooks:mithril_weave', DISABLED)
    write('data/bertieprogression/forbidden_arcanus/hephaestus_forge/ritual/mithril_weave.json', {
        'forge_tier': 3, 'essences': {}, 'main_ingredient': {'item': 'magitech:alchaefabric'},
        'inputs': [{'amount': amount, 'ingredient': {'item': item}} for item, amount in [
            ('malum:astral_weave', 2), ('irons_spellbooks:mithril_ingot', 6),
            ('pastel:onyx_shard', 3), ('l2complements:warden_bone_shard', 1)]],
        'magic_circle': 'forbidden_arcanus:create_item',
        'result': {'type': 'forbidden_arcanus:create_item',
                   'result_item': {'id': 'irons_spellbooks:mithril_weave', 'count': 1}}})

    shaped('refinedstorage:wireless_transmitter', 'refinedstorage:wireless_transmitter', ['ETE', 'UKU', 'RFR'],
           dict(E='deeperdarker:reinforced_echo_shard', T='mekanism:teleportation_core',
                U='oritech:unholy_intelligence', K='refinedstorage:machine_casing',
                R='mekanism:hdpe_rod', F='oritech:carbon_fibre_strands'), tier=1)
    recipe('bertieprogression:anvil_collision/wireless_transmitter', DISABLED)
    shaped('refinedstorage:wireless_grid', 'refinedstorage:wireless_grid', ['QTQ', 'QGQ', 'QAQ'],
           dict(Q='refinedstorage:quartz_enriched_iron', T='mekanism:teleportation_core',
                G={'tag': 'refinedstorage:grids'}, A='refinedstorage:advanced_processor'))

    for kind in ['create:pressing', 'anvilcraft:stamping']:
        key = kind.split(':')[0]
        recipe('bertieprogression:' + key + '/duratium_sheet', {
            'type': kind, 'ingredients': [{'item' if key == 'create' else 'items': 'oritech:duratium_ingot'}],
            'results': [{'id': 'bertieprogression:duratium_sheet', 'count': 1}]})
    recipe('bertieprogression:stamper/duratium_sheet', {
        'type': 'mekmm:stamper', 'input': {'count': 1, 'item': 'berlordscarving:duratium_big_slate'},
        'mold': {'count': 1, 'item': 'bertieprogression:duratium_sheet'},
        'output': {'id': 'bertieprogression:duratium_sheet', 'count': 8},
        'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': m} for m in ['berlordscarving', 'mekmm']]})
    for path in (resources / 'data').glob('*/recipe/**/*.json'):
        value = json.loads(path.read_text())
        output = value.get('output', {})
        if value.get('type') != 'mekmm:stamper' or not output.get('id', '').startswith('bertieprogression:'):
            continue
        item = output['id'].split(':', 1)[1]
        if not item.endswith('_sheet'):
            continue
        value['input'] = {'count': 1, 'item': 'berlordscarving:' + item.removesuffix('_sheet') + '_big_slate'}
        value['output']['count'] = 8
        write(path.relative_to(resources).as_posix(), value)
    write('assets/bertieprogression/models/item/duratium_sheet.json', {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': 'bertieprogression:item/duratium_sheet'}})
    lang = 'assets/bertieprogression/lang/en_us.json'
    values = json.loads((resources / lang).read_text());values['item.bertieprogression.duratium_sheet'] = 'Duratium Sheet'
    write(lang, values)
    write('data/c/tags/item/plates/duratium.json', {'replace': False, 'values': ['bertieprogression:duratium_sheet']})
    plates = 'data/c/tags/item/plates.json';values = json.loads((resources / plates).read_text())
    if 'bertieprogression:duratium_sheet' not in values['values']:
        values['values'].append('bertieprogression:duratium_sheet')
    write(plates, values)
    from shared_tag_replacements import normalize_optional_references
    normalize_optional_references(resources, write)
    recipe('pastel:pedestal/tier2/block_flooder', BLOCK_FLOODER_RECIPE)
    return changed
