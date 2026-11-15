"""Cybernetic research, storage policy and late material/refinery recipes."""

import copy
import json
from pathlib import Path
from zipfile import ZipFile

from recipe_outputs import item_outputs
from tech_solar_recipes import DISABLED

RETIRED_STORAGE = {f'mekanism_extras:{tier}_{family}'
                   for tier in ('absolute', 'supreme', 'cosmic', 'infinite')
                   for family in ('chemical_tank', 'fluid_tank', 'energy_cube')}


def ore_dusts(mods, resources, removed):
    """Match registered ore forms to clean dust tags without inspecting worldgen."""
    tags = {'block': {}, 'item': {}}
    models, blocks = set(), set()
    def collect(name, raw):
        parts = name.split('/')
        if name.startswith('assets/') and '/models/item/' in name and name.endswith('.json'):
            models.add(parts[1] + ':' + '/'.join(parts[4:])[:-5])
        if name.startswith('assets/') and '/blockstates/' in name and name.endswith('.json'):
            blocks.add(parts[1] + ':' + '/'.join(parts[3:])[:-5])
        if len(parts) > 5 and parts[0] == 'data' and parts[2] == 'tags' and parts[3] in tags and name.endswith('.json'):
            kind = parts[3]; identifier = parts[1] + ':' + '/'.join(parts[4:])[:-5]
            value = json.loads(raw)
            if value.get('replace'): tags[kind][identifier] = []
            tags[kind].setdefault(identifier, []).extend(value.get('values', []))
    jars = sorted(Path(mods).glob('*.jar'))
    vanilla = Path(mods).parents[3] / 'libraries/com/mojang/minecraft/1.21.1/minecraft-1.21.1-client.jar'
    if vanilla.exists(): jars.append(vanilla)
    pack = Path(mods).parents[1] / 'mmc-pack.json'
    if pack.exists():
        version = next((x['version'] for x in json.loads(pack.read_text())['components'] if x['uid']=='net.neoforged'), None)
        if version:
            neoforge = Path(mods).parents[3] / f'libraries/net/neoforged/neoforge/{version}/neoforge-{version}-universal.jar'
            if neoforge.exists(): jars.append(neoforge)
    for jar in jars:
        if jar.name.startswith(('bertieprogression-', 'bertieemi-')): continue
        with ZipFile(jar) as z:
            for name in z.namelist():
                if name.endswith('.json') and (('/tags/' in name and name.startswith('data/')) or
                      ('/models/item/' in name and name.startswith('assets/')) or '/blockstates/' in name):
                    collect(name, z.read(name))
    for path in Path(resources).rglob('*.json'):
        name = path.relative_to(resources).as_posix()
        if '/tags/' in name or '/models/item/' in name or '/blockstates/' in name:
            collect(name, path.read_bytes())
    def resolve(kind, identifier, seen=frozenset()):
        if identifier in seen: return set()
        result = set()
        for value in tags[kind].get(identifier, []):
            value = value.get('id', '') if isinstance(value, dict) else value
            if value.startswith('#'): result |= resolve(kind, value[1:], seen | {identifier})
            elif value not in removed: result.add(value)
        return result
    dusts, families = set(), []
    for tag in sorted(tags['item']):
        if not tag.startswith('c:dusts/'): continue
        material = tag[len('c:dusts/'):]
        ore = resolve('block', 'c:ores/' + material) & blocks
        # Some planet/Armageddon ores lack common ore tags but have registered ore block forms.
        ore |= {v for v in blocks-removed if v.split(':',1)[1] == material+'_ore'
                or v.split(':',1)[1].endswith('_'+material+'_ore')}
        clean = resolve('item', tag) & models
        if ore and clean:
            dusts |= clean
            families.append({'material': material, 'ores': sorted(ore), 'dusts': sorted(clean)})
    return sorted(dusts), families


def apply(module, native, mods):
    module = Path(module); resources = module / 'src/main/resources'
    original = {(k if not k.startswith('data/') else k.split('/')[1] + ':' + '/'.join(k.split('/')[3:])[:-5]): v
                for k,v in native.items() if not k.startswith('data/') or '/recipe/' in k}
    effective = copy.deepcopy(original)
    for path in (resources / 'data').glob('*/recipe/**/*.json'):
        rel = path.relative_to(resources / 'data')
        effective[rel.parts[0] + ':' + Path(*rel.parts[2:]).with_suffix('').as_posix()] = json.loads(path.read_text(encoding='utf-8'))
    changed = []
    def write(name, value):
        path = resources / name; text = json.dumps(value, indent=2, ensure_ascii=False) + '\n'
        if not path.exists() or path.read_text(encoding='utf-8') != text:
            path.parent.mkdir(parents=True, exist_ok=True); path.write_text(text, encoding='utf-8', newline='\n'); changed.append(name)
    def recipe(identifier, value):
        ns, name = identifier.split(':', 1); write(f'data/{ns}/recipe/{name}.json', value)
    def shaped(identifier, output, pattern, key, tier):
        for old, value in effective.items():
            if old != identifier and output in item_outputs(value, set()): recipe(old, DISABLED)
        recipe(identifier, {'type':'avaritia:shaped_table', 'tier':tier, 'pattern':pattern,
                            'key':{k:{'item':v} for k,v in key.items()}, 'result':{'id':output,'count':1}})
    removed = set(json.loads((resources/'removed_items.json').read_text(encoding='utf-8')))
    write('removed_items.json', sorted(removed | RETIRED_STORAGE))
    for identifier, value in effective.items():
        if item_outputs(value, set()) & RETIRED_STORAGE:
            recipe(identifier, DISABLED)
    def prune(value):
        if isinstance(value, list):
            return [prune(v) for v in value if not (isinstance(v,str) and v.lstrip('#') in RETIRED_STORAGE)
                    and not (isinstance(v,dict) and v.get('id') in RETIRED_STORAGE)]
        if isinstance(value,dict): return {k:prune(v) for k,v in value.items()}
        return value
    for path in (resources/'data').glob('*/tags/**/*.json'):
        value = json.loads(path.read_text(encoding='utf-8')); cleaned = prune(value)
        if value != cleaned: write(path.relative_to(resources).as_posix(),cleaned)
    hidden_path = 'data/c/tags/item/hidden_from_recipe_viewers.json'
    hidden = json.loads((resources/hidden_path).read_text(encoding='utf-8'))
    existing_hidden = {v if isinstance(v,str) else v['id'] for v in hidden['values']}
    hidden['values'].extend({'id':v,'required':False} for v in sorted(RETIRED_STORAGE-existing_hidden))
    write(hidden_path,hidden)

    # Retire recipes that still consume stock Clumps, preserving repurposed Dirty Dust IDs.
    for identifier, value in effective.items():
        if value.get('type','').startswith('oritech:centrifuge') and 'clump' in json.dumps(value.get('ingredients', [])):
            recipe(identifier, DISABLED)
    for name in ['plating','iron','nickel','carbon']:
        identifier = 'oritech:crafting/' + name
        value = copy.deepcopy(effective[identifier]); value['result']['count'] = 8
        if name == 'carbon': value['key']['c'] = {'item':'oritech:carbon_fibre_strands'}
        recipe(identifier, value)
    for name in ['plating','platingiron','platingnickel','platingcarbon']:
        recipe('oritech:assembler/' + name, DISABLED)
    for identifier in ['oritech:atomicforge/duratium','oritech:mixing/compat/create/duratium']:
        recipe(identifier, DISABLED)

    controller = copy.deepcopy(effective['mekanism_extras:naquadah_reactor/controller'])
    def ultimate_tank(value):
        if value == 'mekanism_extras:absolute_chemical_tank': return 'mekanism:ultimate_chemical_tank'
        if isinstance(value,list): return [ultimate_tank(v) for v in value]
        if isinstance(value,dict): return {k:ultimate_tank(v) for k,v in value.items()}
        return value
    recipe('mekanism_extras:naquadah_reactor/controller',ultimate_tank(controller))

    # Native installers honor this block blacklist before changing the world or consuming an item.
    write('data/mekanism/tags/block/blacklist/installer_upgradeable.json', {'replace':False,
          'values':['mekanism:ultimate_' + family for family in ['chemical_tank','fluid_tank','energy_cube']]
                   + [{'id':v,'required':False} for v in sorted(RETIRED_STORAGE)]})
    for item in sorted(RETIRED_STORAGE):
        write('data/mekanism_extras/loot_table/blocks/' + item.split(':')[1] + '.json',
              {'type':'minecraft:block','pools':[]})
    dynamic = copy.deepcopy(effective['mekanism:dynamic_tank'])
    dynamic['key']['S'] = {'item':'bertieprogression:steel_sheet'}
    recipe('mekanism:dynamic_tank', dynamic)

    empty = 'bertieprogression:empty_reinforced_carbon_sheet'
    shaped('bertieprogression:empty_reinforced_carbon_sheet', empty,
           ['       ','OOAAAOO','OFFFFFO','AFVVVFA','OFFFFFO','OOAAAOO','       '],
           dict(O='mekanism:ingot_refined_obsidian', A='oritech:adamant_ingot', F='oritech:carbon_fibre_strands', V='anvilcraft:void_matter'),3)
    carbon = copy.deepcopy(effective['oritech:refinery/carbonsheet'])
    carbon['ingredients'] = [{'item':empty}]; carbon['fluidInput']['amount'] = 3000
    recipe('oritech:refinery/carbonsheet', carbon)
    write('assets/bertieprogression/models/item/empty_reinforced_carbon_sheet.json',
          {'parent':'minecraft:item/generated','textures':{'layer0':'bertieprogression:item/empty_reinforced_carbon_sheet'}})
    lang_path = 'assets/bertieprogression/lang/en_us.json'
    lang = json.loads((resources/lang_path).read_text(encoding='utf-8'))
    lang['item.bertieprogression.empty_reinforced_carbon_sheet'] = 'Empty Reinforced Carbon Sheet'
    write(lang_path, lang)
    removed = set(json.loads((resources/'removed_items.json').read_text(encoding='utf-8')))
    dusts, families = ore_dusts(mods, resources, removed)
    assert dusts and 'mekanism:dust_refined_obsidian' not in dusts
    write('data/bertieprogression/tags/item/ore_material_dusts.json', {'replace':False, 'values':dusts})
    write('ore_dust_families.json', families)
    recipe('bertieprogression:refinery/mineral_slurry', {
        'type':'oritech:refinery','ingredients':[{'tag':'bertieprogression:ore_material_dusts'}],
        'fluidInput':{'fluid':'oritech:still_sulfuric_acid','amount':50},'results':[],
        'fluidOutputs':[{'fluid':'oritech:still_mineral_slurry','amount':100}], 'time':80})
    for identifier, ingredient, amount in [
            ('oritech:refinery/lavaalt', 'forbidden_arcanus:ender_pearl_fragment', 10),
            ('bertieprogression:refinery/lava_end_dust','anvilcraft:levitation_powder_block',200)]:
        value = copy.deepcopy(effective[identifier]); value['ingredients'] = [{'item':ingredient}]
        for output in value['fluidOutputs']:
            if output['fluid'] == 'oritech:still_strange_matter': output['amount'] = amount
        recipe(identifier,value)
    recipe('bertieprogression:centrifuge/colossal_iron', {'type':'oritech:centrifuge',
        'ingredients':[{'item':'minecraft:iron_block'}],
        'results':[{'id':'armageddon_mod:colossal_iron_ingot','count':1}], 'time':100})
    for research in json.loads((module/'texture-work/augmentation_research_costs.json').read_text())['research']:
        value = copy.deepcopy(effective[research['id']]); value['researchCost'] = research['researchCost']
        recipe(research['id'],value)
    shaped('oritech:crafting/augment/applicator','oritech:augment_application_block',
           ['ABBBA','CDEDC','CFGFC','CHIHC','ABBBA'],
           dict(A='iceandfire:dragonsteel_lightning_ingot',B='mekanism:ingot_refined_obsidian',C='bertieprogression:bronze_sheet',
                D='oritech:enderic_lens',E='oritech:overcharged_crystal',F='mekanism_extras:absolute_control_circuit',
                G='mekanism:steel_casing',H='oritech:unholy_intelligence',I='oritech:motor'),2)
    shaped('oritech:crafting/augment/basic','oritech:simple_augment_station',
           ['AAAAB','ACDEB','AAFGB','HACEB','HAAAB'],
           dict(A='bertieprogression:bronze_sheet',B='mekanism:ingot_refined_obsidian',C='oritech:unholy_intelligence',
                D='minecraft:brewing_stand',E='magitech:radiant_steel_ingot',F='mekanism:steel_casing',G='cognition:cognitive_crystal',
                H='oritech:carbon_fibre_strands'),2)
    shaped('oritech:crafting/unstablecontainer','oritech:unstable_container',
           ['ESSSESS','SLAACLS','SAKVVCE','SAVUVIS','ECVVHIS','SLCIILS','SSESSSE'],
           dict(E='oritech:energite_block',S='oritech:superconductor',L='anvilcraft:levitation_powder_block',A='oritech:super_ai_chip',
                C='oritech:overcharged_crystal',K='l2complements:sculkium_block',V='anvilcraft:void_matter_block',
                U='mekanism:ultimate_energy_cube',I='oritech:unholy_intelligence',H='anvilcraft:overheated_ember_metal_block'),3)
    for output, source, base in [('fire','lightning','anvilcraft:ember_metal_block'),
                                 ('ice','fire','anvilcraft:frost_metal_block'),
                                 ('lightning','ice','elemental_metals:lightning_infused_netherite_block')]:
        recipe('bertieprogression:anvil_collision/dragonsteel_' + output, {
            'type':'anvilcraft:anvil_collision',
            'anvil':{'blocks':'#bertieprogression:collision_anvils/tier_1'},'consume':False,'speed':32,
            'hitBlock':{'blocks':'iceandfire:dragonsteel_' + source + '_block'},
            'transform_blocks':[{'input':{'blocks':base},'max_count':1,'output':{'block':'minecraft:air'}}],
            'output_items':[{'id':'iceandfire:dragonsteel_' + output + '_ingot','count':8}]})
    write('data/oritech/tags/block/unstable_container/high.json',
          {'replace':False,'values':['anvilcraft:confined_energy_anvilon']})
    return changed


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--module', type=Path, required=True)
    parser.add_argument('--mods', type=Path, required=True)
    args = parser.parse_args()
    native = {}
    for jar in sorted(args.mods.glob('*.jar')):
        if jar.name.startswith(('bertieprogression-', 'bertieemi-')): continue
        with ZipFile(jar) as z:
            for name in z.namelist():
                if name.startswith('data/') and '/recipe/' in name and name.endswith('.json'):
                    try:
                        native[name] = json.loads(z.read(name))
                    except json.JSONDecodeError:
                        continue
    print(json.dumps({'changed':apply(args.module, native, args.mods)}))
