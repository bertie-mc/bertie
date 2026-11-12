"""Warden acquisition correction, engraving ingredients and Wind Generator craft."""

import copy
import json
from pathlib import Path


def apply(module, native):
    resources = Path(module) / 'src/main/resources'
    changed = []

    def save(identifier, value, folder='recipe'):
        namespace, name = identifier.split(':', 1)
        path = resources / 'data' / namespace / folder / (name + '.json')
        text = json.dumps(value, indent=2) + '\n'
        if not path.exists() or path.read_text(encoding='utf-8') != text:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8', newline='\n')
            changed.append(path.relative_to(resources).as_posix())

    # Keep a false loot predicate as a fallback for loaders without conditional GLM support.
    save('l2complements:warden_bone_shard', {
        'neoforge:conditions': [{'type': 'neoforge:false'}],
        'type': 'l2core:add_item',
        'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.0}],
        'item': 'l2complements:warden_bone_shard'}, folder='loot_modifiers')
    identifier = 'oritech:atomicforge/reinfdeepslate'
    path = resources / 'data/oritech/recipe/atomicforge/reinfdeepslate.json'
    recipe = json.loads(path.read_text()) if path.exists() else copy.deepcopy(native[identifier])
    for ingredient in recipe['ingredients']:
        if ingredient.get('item') == 'oritech:duratium_ingot':
            ingredient['item'] = 'l2complements:warden_bone_shard'
    save(identifier, recipe)
    save('mekanismgenerators:generator/wind', {
        'type': 'avaritia:shaped_table', 'tier': 2,
        'pattern': [' FRF ', 'FRPRF', 'OFKFO', 'OAMAO', 'OTMTO'],
        'key': {symbol: {'item': item} for symbol, item in {
            'F': 'oritech:carbon_fibre_strands', 'R': 'bertieprogression:steel_rod',
            'P': 'mekanismgenerators:turbine_blade', 'O': 'bertieprogression:osmium_sheet',
            'K': 'mekanism:steel_casing', 'A': 'mekanism:advanced_control_circuit',
            'M': 'oritech:magnetic_coil', 'T': 'mekanism:energy_tablet'}.items()},
        'result': {'id': 'mekanismgenerators:wind_generator', 'count': 1}})
    return changed
