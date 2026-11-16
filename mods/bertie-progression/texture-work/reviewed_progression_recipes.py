"""Reviewed acquisition and recipe-cleanup selections, applied after legacy generators."""
import copy
import json
from collections import Counter
from pathlib import Path
from zipfile import ZipFile

from compact_storage_recipes import load_native
from recipe_outputs import item_outputs

OFF = {'neoforge:conditions': [{'type': 'neoforge:false'}]}
SMALL_CLUMPS = {f'oritech:small_{metal}_clump' for metal in ('copper', 'gold', 'iron', 'nickel', 'platinum')}
WITHDRAWN = SMALL_CLUMPS | {'pastel:crafting_tablet', 'oritech:spawner_controller_block',
    'oritech:spawner_cage_block', 'oritech:fragment_forge_block', 'anvilcraft:oil_bucket'}


def apply(module, native, mods):
    module = Path(module)
    resources = module / 'src/main/resources'
    plan = json.loads((module / 'texture-work/recipe_cleanup_selections.json').read_text())
    audit = json.loads((module / 'texture-work/recipe_cleanup_audit.json').read_text())
    effective = copy.deepcopy(native)
    changed = []

    def write(name, value):
        path = resources / name
        text = json.dumps(value, ensure_ascii=False, indent=2) + '\n'
        if not path.exists() or path.read_text(encoding='utf-8') != text:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8', newline='\n')
            changed.append(name)

    def recipe(rid, value):
        ns, name = rid.split(':', 1)
        write(f'data/{ns}/recipe/{name}.json', value)
        effective[rid] = copy.deepcopy(value)

    for path in (resources / 'data').glob('*/recipe/**/*.json'):
        rel = path.relative_to(resources / 'data')
        effective[rel.parts[0] + ':' + '/'.join(rel.parts[2:])[:-5]] = json.loads(path.read_text(encoding='utf-8'))

    def active(value):
        return {'type': 'neoforge:false'} not in value.get('neoforge:conditions', [])

    def conditions(ids):
        return [{'type':'neoforge:mod_loaded','modid':namespace}
                for namespace in sorted({i.split(':')[0] for i in ids}-{'minecraft','bertieprogression','c'})]

    def output_recipes(output, types=None):
        return [rid for rid, value in effective.items() if active(value)
                and not rid.startswith('bertieprogression:slabs/')
                and output in item_outputs(value, set()) and (types is None or value.get('type') in types)]

    def shaped(rid, output, pattern, key, count=1, tier=None):
        value = {'type': 'minecraft:crafting_shaped' if tier is None else 'avaritia:shaped_table',
                 'pattern': pattern, 'key': {k: ({'tag': v[1:]} if v.startswith('#') else {'item': v})
                    for k, v in key.items()}, 'result': {'id': output, 'count': count}}
        if tier is not None:
            value['tier'] = tier
        recipe(rid, value)

    grid_types = {'minecraft:crafting_shaped', 'minecraft:crafting_shapeless'}
    for family, pattern, count in [('chiseled', plan['chiseled']['pattern'], 5),
                                   ('mosaics', plan['mosaics']['pattern'], 8),
                                   ('pillars', plan['pillars']['pattern'], 3)]:
        mappings = ({**plan[family]['brick_inputs'], **plan[family]['fallback_inputs']}
                    if family == 'chiseled' else plan[family]['inputs'])
        for output, material in mappings.items():
            ids = output_recipes(output, grid_types)
            # Optional/backported building families must exist before receiving a recipe.
            if not ids and not output.startswith('minecraft:'):
                continue
            if not ids:
                ids = [output]
            survivor = ids[0]
            for rid in ids[1:]:
                recipe(rid, OFF)
            shaped(survivor, output, pattern, {'B' if family == 'chiseled' else 'P': material}, count)
    for row in plan['shaped_crafts']:
        ids = output_recipes(row['output'], grid_types)
        if not ids:
            ids = [row['output']]
        for rid in ids[1:]:
            recipe(rid, OFF)
        shaped(ids[0], row['output'], row['pattern'], row['key'], row['count'])

    for rid in output_recipes('quark:cobblestone_bricks', grid_types):
        recipe(rid, OFF)
    recipe('bertieprogression:stonecutting/cobblestone_bricks', {'type': 'minecraft:stonecutting',
        'ingredient': {'item': 'minecraft:cobblestone'}, 'result': {'id': 'quark:cobblestone_bricks', 'count': 1}})
    for rid in output_recipes('pastel:polished_amethyst_block', grid_types):
        recipe(rid, OFF)
    for rid, value in list(effective.items()):
        if value.get('type') in grid_types | {'minecraft:stonecutting'}:
            if 'pastel:polished_amethyst_block' in json.dumps(value.get('ingredients', value.get('ingredient', value.get('key', {})))):
                if item_outputs(value, set()) & {'minecraft:amethyst_shard', 'minecraft:amethyst_block'}:
                    recipe(rid, OFF)
    recipe('bertieprogression:smelting/polished_amethyst_block', {'type': 'minecraft:smelting',
        'ingredient': {'item': 'minecraft:amethyst_block'}, 'result': {'id': 'pastel:polished_amethyst_block'},
        'experience': 0.1, 'cookingtime': 200})

    for wood in plan['wood']['woods']:
        output = f'quark:vertical_{wood}_planks'
        for rid in output_recipes(output, grid_types):
            recipe(rid, OFF)
        recipe(f'bertieprogression:woodcutting/vertical_{wood}_planks', {'type': 'sawmill:woodcutting',
            'ingredient': {'item': f'minecraft:{wood}_planks'}, 'result': {'id': output, 'count': 1}})
    for base in plan['malum_runic_walls']['bases']:
        output = 'malum:runic_' + base + '_wall'
        for rid in output_recipes(output, grid_types):
            recipe(rid, OFF)
        recipe('bertieprogression:stonecutting/runic_' + base + '_wall', {'type': 'minecraft:stonecutting',
            'ingredient': {'item': 'malum:' + base}, 'result': {'id': output, 'count': 1}})
    recipe(plan['charcoal_to_black_dye']['disable_recipe'], OFF)
    for detector, material in [('weather_detector', 'minecraft:gold_ingot'), ('item_detector', 'minecraft:prismarine')]:
        rid = 'pastel:pedestal/tier1/detectors/' + detector
        value = copy.deepcopy(effective[rid])
        centre = value['pattern'][1][1]
        value['key'][centre] = {'item': material}
        recipe(rid, value)

    merges = {item: row['keep'][0] for row in plan['storage'].values() for item in row['retire']}
    merges.update(zip(plan['calcite']['replace'], plan['calcite']['keep']))
    removed = WITHDRAWN | set(merges)
    content_merges = {**json.loads((resources/'merged_items.json').read_text()),**merges}
    retired_content = (set(json.loads((resources/'removed_items.json').read_text())) | WITHDRAWN) - content_merges.keys()
    def canonical_content(value):
        seen=set()
        while value in content_merges and value not in seen:
            seen.add(value);value=content_merges[value]
        return value
    def replace_content(value):
        if isinstance(value,str):return canonical_content(value)
        if isinstance(value,list):return [replace_content(v) for v in value]
        if isinstance(value,dict):return {k:replace_content(v) for k,v in value.items()}
        return value
    native_content = {}
    needles = [item.encode() for item in removed]
    for jar in sorted(Path(mods).glob('*.jar')):
        if jar.name.startswith(('bertieprogression-', 'bertieemi-')):continue
        with ZipFile(jar) as archive:
            for name in archive.namelist():
                if name.startswith('data/') and name.endswith('.json') and ('/tags/' in name or '/loot_table/' in name):
                    raw = archive.read(name)
                    if any(needle in raw for needle in needles):
                        try:native_content[name] = json.loads(raw)
                        except json.JSONDecodeError:pass
    # Storage merges preserve retained item consumers; acquisition of retired forms disappears.
    def replace(value):
        if isinstance(value, str):
            return merges.get(value, value)
        if isinstance(value, list):
            return [replace(v) for v in value]
        if isinstance(value, dict):
            return {k: replace(v) for k, v in value.items()}
        return value

    for rid, original in list(effective.items()):
        if not active(original):
            continue
        outputs = item_outputs(original, set())
        text = json.dumps(original)
        if outputs & removed or any('"' + item + '"' in text for item in WITHDRAWN):
            recipe(rid, OFF)
        elif original.get('type', '').startswith('oritech:') and ('fragment' in original.get('type', '') or 'oil' in text and 'anvilcraft:oil' in text):
            recipe(rid, OFF)
        else:
            value = replace(original)
            if value != original:
                recipe(rid, value)
    content_paths = {p.relative_to(resources).as_posix() for p in (resources/'data').glob('*/loot_table/**/*.json')}
    content_paths.update(name for name in native_content if '/loot_table/' in name)
    for name in sorted(content_paths):
        path = resources/name
        value = json.loads(path.read_text(encoding='utf-8')) if path.exists() else native_content[name]
        def loot_prune(node):
            if isinstance(node, list):
                return [loot_prune(v) for v in node if not (isinstance(v, dict) and v.get('name') in retired_content)]
            if isinstance(node, dict):
                return {k: loot_prune(v) for k, v in node.items()}
            return canonical_content(node) if isinstance(node, str) else node
        clean = loot_prune(value)
        if clean != value:
            write(name, clean)
    tag_paths = {p.relative_to(resources).as_posix() for p in (resources/'data').glob('*/tags/**/*.json')}
    tag_paths.update(name for name in native_content if '/tags/' in name)
    for name in sorted(tag_paths):
        if 'hidden_from_recipe_viewers' in name:continue
        path = resources/name
        value = json.loads(path.read_text(encoding='utf-8')) if path.exists() else native_content[name]
        if name not in native_content and not any('"'+item+'"' in json.dumps(value) for item in removed):continue
        def tags(node):
            if isinstance(node, list):
                values = [tags(v) for v in node if not (isinstance(v, str) and v in retired_content)
                          and not (isinstance(v, dict) and v.get('id') in retired_content)]
                if '/tags/block/' in name:
                    values=[v for v in values if (v if isinstance(v,str) else v.get('id'))!='mekanism:hdpe_sheet']
                values = [({'id':v,'required':False} if isinstance(v,str) and v.lstrip('#').split(':')[0] not in {'minecraft','bertieprogression'}
                           else {**v,'required':False} if isinstance(v,dict) and v.get('id','').lstrip('#').split(':')[0] not in {'minecraft','bertieprogression'} else v)
                          for v in values]
                return list({json.dumps(v,sort_keys=True):v for v in values}.values())
            return {k: tags(v) for k, v in node.items()} if isinstance(node, dict) else canonical_content(node) if isinstance(node,str) else node
        clean = tags(value)
        if clean != value:
            write(name, clean)
    existing = set(json.loads((resources / 'removed_items.json').read_text()))
    write('removed_items.json', sorted(existing | removed))
    hidden_path = 'data/c/tags/item/hidden_from_recipe_viewers.json'
    hidden = json.loads((resources / hidden_path).read_text())
    have = {v if isinstance(v, str) else v['id'] for v in hidden['values']}
    hidden['values'].extend({'id': item, 'required': False} for item in sorted(removed - have))
    write(hidden_path, hidden)
    for item in WITHDRAWN:
        ns, name = item.split(':')
        write(f'data/{ns}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': []})
    for item in merges:
        ns,name = item.split(':')
        path = f'data/{ns}/loot_table/blocks/{name}.json'
        if path in native_content:write(path, replace_content(native_content[path]))
    merge_path = 'merged_items.json'
    mappings = json.loads((resources/merge_path).read_text())
    mappings.update(merges)
    write(merge_path,dict(sorted(mappings.items())))
    for kind in ('block', 'item'):
        write(f'assets/anvilcraft/models/{kind}/gunpowder_block.json', {'parent': 'cratedelight:block/gunpowder_bag'})

    # Native routes subsume these generated returns without changing their available arrangements.
    for group in audit['M1_identical_mekanism_additions_slab_returns'] + audit['M2_create_subsumed_shaped_returns']:
        if active(effective.get(group[1], OFF)):
            recipe(group[0], OFF)
    for slab in ['alexsmobs__purpur_planks_slab', 'burnt__burning_top_slab', 'quark__polished_tuff_slab', 'quark__tuff_bricks_slab']:
        recipe('bertieprogression:slabs/' + slab, OFF)
    def canonical(value):
        value = copy.deepcopy(value)
        value.pop('category', None)
        value.pop('group', None)
        value.pop('show_notification', None)
        if value.get('type') == 'minecraft:crafting_shaped':
            value['pattern'] = [[value['key'].get(c, None) for c in row] for row in value['pattern']]
            value.pop('key')
        elif value.get('type') == 'minecraft:crafting_shapeless':
            value['ingredients'] = sorted(value['ingredients'], key=lambda v: json.dumps(v, sort_keys=True))
        return json.dumps(value, sort_keys=True)
    references = '\n'.join(p.read_text(encoding='utf-8', errors='replace')
        for folder in [resources/'data', module/'src/main/java'] for p in folder.rglob('*')
        if p.is_file() and p.suffix in {'.json','.java'})
    native_references = []
    for jar in sorted(Path(mods).glob('*.jar')):
        if jar.name.startswith(('bertieprogression-', 'bertieemi-')):continue
        with ZipFile(jar) as archive:
            for path in archive.namelist():
                if path.startswith('data/') and '/advancement/' in path and path.endswith('.json'):
                    native_references.append(archive.read(path).decode('utf-8',errors='replace'))
    references += '\n'+'\n'.join(native_references)
    for group in audit['same_method_duplicate_candidates']:
        ids = sorted([rid for rid in group if active(effective.get(rid, OFF))], key=lambda rid:(rid.startswith('bertieprogression:'),rid))
        for rid in ids[1:]:
            if '"'+rid+'"' not in references and canonical(effective[ids[0]]) == canonical(effective[rid]):
                recipe(rid, OFF)

    for rid, value in list(effective.items()):
        if not active(value):
            continue
        clean = copy.deepcopy(value)
        if not clean.get('type'):
            recipe(rid, OFF)
            continue
        if clean.get('type') == 'minecraft:crafting_shaped':
            used = set(''.join(clean.get('pattern', []))) - {' '}
            defined = set(clean.get('key', {}))
            if len(used-defined) == len(defined-used) == 1:
                clean['key'][next(iter(used-defined))] = clean['key'].pop(next(iter(defined-used)))
        def armor_tags(node):
            if isinstance(node,list):return [armor_tags(v) for v in node]
            if not isinstance(node,dict):return node
            if isinstance(node.get('item'),str) and node['item'] in {'irons_spellbooks:wizard_base_'+slot for slot in ('helmet','chestplate','leggings','boots')}:
                return {**{k:v for k,v in node.items() if k!='item'},'tag':node['item']}
            return {k:armor_tags(v) for k,v in node.items()}
        clean = armor_tags(clean)
        if clean.get('type') == 'farmersdelight:cutting':
            clean['result'] = [({'item':{k:v for k,v in result.items() if k in {'id','count','components'}},
                                 **{k:v for k,v in result.items() if k not in {'id','count','components'}}}
                                if 'id' in result else result) for result in clean.get('result',[])]
        if rid in {'ends_delight:food/bubble_tea', 'ends_delight:food/ender_congee'}:
            clean['ingredients'] = [{'type': 'neoforge:compound', 'children': v} if isinstance(v, list) else v for v in clean['ingredients']]
        if rid == 'endersdelight:chorus_pie_slice':
            clean['tool'] = {'tag': 'c:tools/knife'}
            clean['result'] = [{'item': {'id': 'endersdelight:chorus_pie_slice', 'count': 4}}]
        if rid.startswith('cocktailsdelight:') and clean.get('type', '').startswith('create:') or rid == 'malum:create/milling/grim_talc':
            for result in clean.get('results', []):
                if 'item' in result: result['id'] = result.pop('item')
                if 'fluid' in result: result['id'] = result.pop('fluid')
            for ingredient in clean.get('ingredients', []):
                if 'fluid' in ingredient: ingredient['type'] = 'fluid_stack'
        if any(c.get('type') == 'neoforge:never' for c in clean.get('neoforge:conditions', [])):
            clean = copy.deepcopy(OFF)
        if rid in audit.get('optional_compat',[]) and active(clean):
            required = set()
            def namespaces(node):
                if isinstance(node,list):
                    for child in node:namespaces(child)
                elif isinstance(node,dict):
                    for key,child in node.items():
                        if key in {'item','tag','type'} and isinstance(child,str) and ':' in child:
                            required.add(child.lstrip('#').split(':')[0])
                        else:namespaces(child)
            namespaces(clean)
            guards = list(clean.get('neoforge:conditions',[]))
            guards += [{'type':'neoforge:mod_loaded','modid':ns} for ns in sorted(required-{'minecraft','neoforge','forge','c',rid.split(':')[0]})]
            guards += [{'type':'neoforge:item_exists','item':item} for item in sorted(item_outputs(clean,set()))]
            clean['neoforge:conditions'] = list({json.dumps(g,sort_keys=True):g for g in guards}.values())
        if clean != value:
            recipe(rid, clean)
    recipe('enigmaticlegacyplus:survival_scroll', OFF)
    for jar in sorted(Path(mods).glob('*.jar')):
        if jar.name.startswith(('magitech-', 'simplymore-')):
            with ZipFile(jar) as archive:
                for path in ['data/magitech/recipe/heavy_blade_part_cutting.json', 'data/simplymore/recipe/matterbane_clean.json']:
                    if path in archive.namelist():
                        raw = archive.read(path).decode().strip()
                        clean, _ = json.JSONDecoder().raw_decode(raw)
                        recipe(path.split('/')[1] + ':' + '/'.join(path.split('/')[3:])[:-5], clean)
    rid = 'cataclysm:stonecutting/azure_seastone_wall_from_stonecutting'
    value = copy.deepcopy(effective[rid]); value['result'] = {'id': 'cataclysm:azure_seastone_wall', 'count': 1}; recipe(rid, value)
    rid = 'cataclysm_spellbooks:smithing/abyssal_warloock_leggings_smithing'
    value = copy.deepcopy(effective[rid]); value['base'] = {'tag': 'irons_spellbooks:wizard_base_leggings'}
    value['result'] = {'id': 'cataclysm_spellbooks:abyssal_warlock_leggings', 'count': 1}; recipe(rid, value)

    for row in json.loads((module / 'texture-work/queued_050_layouts.json').read_text()):
        output = row['output'] if isinstance(row['output'], str) else row['output']['id']
        target = row.get('id', output)
        for rid in output_recipes(output, grid_types | {'avaritia:shaped_table'}):
            if rid != target and not (output == 'cataclysm:enderite_ingot' and 'block' in json.dumps(effective[rid].get('ingredients', []))):
                recipe(rid, OFF)
        value = {'type': 'avaritia:shaped_table', 'pattern': row['pattern'], 'key': row['key'],
                 'tier': row.get('tier', 2), 'result': {'id': output, 'count': row.get('count', row['output'].get('count', 1) if isinstance(row['output'], dict) else 1)}}
        if row.get('catalyst'):
            value['type'] = 'bertieprogression:reusable_table'
            value['catalyst'] = {'item': row['catalyst']}
        value['neoforge:conditions'] = conditions([output,'avaritia:table']+[v['item'] for v in row['key'].values() if 'item' in v])
        recipe(target, value)
    casing = json.loads((module / 'texture-work/spatial_progression_plan.json').read_text())['recipe_changes']['sentient_casing']
    recipe('bertieprogression:sentient_casing', {'type': 'avaritia:shaped_table', 'tier': 3,
        'pattern': casing['pattern'], 'key': casing['key'], 'result': {'id': 'bertieprogression:sentient_casing', 'count': 1}})
    projector = copy.deepcopy(effective['compactcrafting:field_projector'])
    for entry in projector['key'].values():
        if entry.get('item') == 'mekanism:steel_casing': entry['item'] = 'bertieprogression:sentient_casing'
    projector['result']['count'] = 4; recipe('compactcrafting:field_projector', projector)
    for rid, count in [('mekanism:thermal_evaporation/block', 1), ('mekanism:dynamic_tank', 1)]:
        value = copy.deepcopy(effective[rid]); value['result']['count'] = count; recipe(rid, value)
    value = copy.deepcopy(effective['oritech:crafting/biogen'])
    for entry in value['key'].values():
        if entry.get('item') == 'oritech:raw_biopolymer': entry['item'] = 'oritech:biosteel_ingot'
    recipe('oritech:crafting/biogen', value)
    value = copy.deepcopy(effective['anvilcraft:charger_charging/magnet_ingot'])
    value['ingredient'] = {'item': 'oritech:nickel_ingot'}; recipe('anvilcraft:charger_charging/magnet_ingot', value)
    for rid, value in list(effective.items()):
        if rid.startswith('anvilcraft:time_warp/') and (value.get('transform') == 'anvilcraft:oil' or rid.split('/')[-1].startswith('raw_')):
            recipe(rid, OFF)
    value = copy.deepcopy(effective['anvilcraft:time_warp/wither_rose']); value['results'][0]['count']['p'] = 0.01
    recipe('anvilcraft:time_warp/wither_rose', value)
    value = copy.deepcopy(effective['anvilcraft:time_warp/wither_skeleton_skull']); value['ingredients'][1] = {'items': 'minecraft:wither_rose', 'count': 4}
    recipe('anvilcraft:time_warp/wither_skeleton_skull', value)
    for rid in ['minecraft:' + name for name in ['ancient_debris', 'budding_amethyst', 'dragon_breath', 'dragon_head', 'echo_shard', 'heart_of_the_sea', 'reinforced_deepslate', 'sculk', 'trident']] + ['avaritia:ancient_debris_eight', 'avaritia:record_fragment']:
        recipe(rid, OFF)
    shaped('avaritia:refined_coal', 'avaritia:refined_coal', ['CCC', 'CNC', 'CCC'], {'C': 'minecraft:coal', 'N': 'avaritia:neutron_nugget'}, tier=1)
    recipe('bertieprogression:crushing/music_disc_record_fragments', {'type': 'create:crushing', 'ingredients': [{'tag': 'c:music_discs'}],
        'results': [{'id': 'avaritia:record_fragment', 'count': 1}, {'id': 'avaritia:record_fragment', 'count': 1, 'chance': 0.5}], 'processing_time': 100})

    views = json.loads((resources / 'compact_mini_recipes.json').read_text())
    views = [v for v in views if not v.get('machine_layout')]
    large = json.loads((module / 'texture-work/large_machine_mini_layouts.json').read_text())
    for row in large['recipes']:
        rid = 'bertieprogression:miniaturization/' + row['output']['id'].split(':')[1]
        canonical_symbols = {}; aliases = {}; components = {}
        for symbol, block in row['key'].items():
            block = row.get('physical_variant_aliases', {}).get(block, block)
            if block not in canonical_symbols:
                canonical_symbols[block] = symbol
                components[symbol] = {'type': 'compactcrafting:block', 'block': block}
            aliases[symbol] = canonical_symbols[block]
        # Native JSON is top-to-bottom and indexes [x][z]; captures are bottom-to-top [z][x].
        layers = [{'type': 'compactcrafting:mixed', 'pattern': [[aliases[layer[z][x]] if layer[z][x] != '.' else '-'
                   for z in range(3)] for x in range(3)]} for layer in reversed(row['layers'])]
        recipe(rid, {'type': 'compactcrafting:miniaturization', 'recipeSize': 3, 'craftingTime': 200,
            'layers': layers, 'components': components, 'catalyst': {'id': row['catalyst']['item'], 'count': 1}, 'outputs': [row['output']],
            'neoforge:conditions':conditions(list(row['key'].values())+[row['catalyst']['item'],row['output']['id'],'compactcrafting:field_projector'])})
        views.append({'recipe': rid, 'size': 3, 'machine_layout': True, 'ingredients': row['ingredient_block_counts'],
            'catalyst': row['catalyst']['item'], 'output': row['output']})
    write('compact_mini_recipes.json', views)
    for tag in ['mineable/pickaxe','needs_iron_tool']:
        path=f'data/minecraft/tags/block/{tag}.json'
        value=json.loads((resources/path).read_text()) if (resources/path).exists() else {'replace':False,'values':[]}
        if not any(v=='bertieprogression:sentient_casing' or isinstance(v,dict) and v.get('id')=='bertieprogression:sentient_casing' for v in value['values']):
            value['values'].append('bertieprogression:sentient_casing')
        write(path,value)
    write('data/bertieprogression/loot_table/blocks/sentient_casing.json',{'type':'minecraft:block','pools':[{'rolls':1,
        'entries':[{'type':'minecraft:item','name':'bertieprogression:sentient_casing'}],
        'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    fluid_path = 'removed_fluids.json'
    fluids = set(json.loads((resources/fluid_path).read_text()))
    write(fluid_path, sorted(fluids | {'anvilcraft:oil','anvilcraft:flowing_oil'}))
    lang_path = 'assets/bertieprogression/lang/en_us.json'
    lang = json.loads((resources/lang_path).read_text())
    lang['block.bertieprogression.sentient_casing'] = 'Sentient Casing'
    write(lang_path, lang)
    from spatial_guides import generate
    generate(module)
    return changed


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--module', type=Path, required=True)
    parser.add_argument('--mods', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps({'changed': apply(args.module, load_native(args.mods), args.mods)}))
