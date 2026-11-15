"""Compact-space acquisition, storage-part progression and selected catalogue retirement."""

import copy
import json
from pathlib import Path
from zipfile import ZipFile

from recipe_outputs import item_outputs
from tech_solar_recipes import DISABLED

POWAH_FAMILIES = ('furnator','magmator','thermo_generator','reactor','energy_hopper','energy_discharger','ender_cell')
TIERS = ('starter','basic','hardened','blazing','niotic','spirited','nitro')
RETIRED = {f'powah:{family}_{tier}' for family in POWAH_FAMILIES for tier in TIERS} | {'compactcrafting:base','compactcrafting:projector_dish'}


def load_native(mods):
    native = {}
    for jar in sorted(Path(mods).glob('*.jar')):
        if jar.name.startswith(('bertieprogression-','bertieemi-')): continue
        with ZipFile(jar) as z:
            for path in z.namelist():
                prefix = 'data/compactmachines/datapacks/basic_templates/'
                normalized = path[len(prefix):] if path.startswith(prefix) else path
                if normalized.startswith('data/') and '/recipe/' in normalized and normalized.endswith('.json'):
                    try:
                        value = json.loads(z.read(path))
                    except json.JSONDecodeError:
                        continue
                    ns = normalized.split('/')[1]; name = '/'.join(normalized.split('/')[3:])[:-5]
                    native[ns+':'+name] = value
    return native


def apply(module, native, mods):
    module = Path(module); resources = module/'src/main/resources'
    original = {(k if not k.startswith('data/') else k.split('/')[1]+':'+ '/'.join(k.split('/')[3:])[:-5]):v
                for k,v in native.items() if not k.startswith('data/') or '/recipe/' in k}
    # Built-in Compact Machine packs are not ordinary top-level mod recipes.
    original.update({k:v for k,v in load_native(mods).items() if k.startswith('compactmachines:')})
    effective = copy.deepcopy(original)
    for path in (resources/'data').glob('*/recipe/**/*.json'):
        rel = path.relative_to(resources/'data')
        effective[rel.parts[0]+':'+Path(*rel.parts[2:]).with_suffix('').as_posix()] = json.loads(path.read_text(encoding='utf-8'))
    changed = []
    def write(name,value):
        path=resources/name; text=json.dumps(value,indent=2,ensure_ascii=False)+'\n'
        if not path.exists() or path.read_text(encoding='utf-8')!=text:
            path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text,encoding='utf-8',newline='\n');changed.append(name)
    def recipe(identifier,value):
        ns,name=identifier.split(':',1);write(f'data/{ns}/recipe/{name}.json',value)
    def conditions(ids):
        return [{'type':'neoforge:mod_loaded','modid':n} for n in sorted({x.split(':')[0] for x in ids}-{'minecraft','bertieprogression','c'})]

    for identifier,value in effective.items():
        if item_outputs(value,set()) & RETIRED: recipe(identifier,DISABLED)
    removed=set(json.loads((resources/'removed_items.json').read_text(encoding='utf-8')))
    write('removed_items.json',sorted(removed|RETIRED))
    def prune(value):
        if isinstance(value,list):return [prune(v) for v in value if not (isinstance(v,str) and v.lstrip('#') in RETIRED) and not(isinstance(v,dict) and v.get('id') in RETIRED)]
        if isinstance(value,dict):return {k:prune(v) for k,v in value.items()}
        return value
    for path in (resources/'data').glob('*/tags/**/*.json'):
        value=json.loads(path.read_text(encoding='utf-8'));clean=prune(value)
        if value!=clean:write(path.relative_to(resources).as_posix(),clean)
    hidden_path='data/c/tags/item/hidden_from_recipe_viewers.json'
    hidden=json.loads((resources/hidden_path).read_text(encoding='utf-8'))
    existing={v if isinstance(v,str) else v['id'] for v in hidden['values']}
    hidden['values'].extend({'id':v,'required':False} for v in sorted(RETIRED-existing));write(hidden_path,hidden)
    for item in sorted(RETIRED):
        ns,name=item.split(':');write(f'data/{ns}/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[]})

    layouts=json.loads((module/'texture-work/compact_storage_layouts.json').read_text())
    for layout in layouts:
        for old,value in effective.items():
            if old!=layout['id'] and layout['output'] in item_outputs(value,set()):recipe(old,DISABLED)
        value={'type':'minecraft:crafting_shaped' if layout['tier'] is None else 'avaritia:shaped_table',
               'pattern':layout['pattern'],'key':layout['key'],'result':{'id':layout['output'],'count':layout['count']}}
        if layout['tier'] is not None:value['tier']=layout['tier']
        ids=[layout['output']]+[v['item'] for v in layout['key'].values()]
        if layout['tier'] is not None:ids.append('avaritia:table')
        value['neoforge:conditions']=conditions(ids);recipe(layout['id'],value)

    plan=json.loads((module/'texture-work/storage_part_layouts.json').read_text())
    for family in plan['families'].values():
        for row in family:
            value=copy.deepcopy(effective[row['recipe']]);value.update(row['overrides']);recipe(row['recipe'],value)
    replacements={r['output']:r['replace_silicon_with'] for r in plan['processor_substitutions']}
    def silicon(value,replacement):
        if isinstance(value,list):return [silicon(v,replacement) for v in value]
        if isinstance(value,dict):
            if value.get('tag')=='c:silicon' or value.get('item') in {'oritech:silicon','refinedstorage:silicon'}:
                result={k:v for k,v in value.items() if k not in {'tag','item'}};result['item']=replacement;return result
            return {k:silicon(v,replacement) for k,v in value.items()}
        return value
    for identifier,value in effective.items():
        if {'type':'neoforge:false'} in value.get('neoforge:conditions',[]):continue
        match=item_outputs(value,set()) & replacements.keys()
        if match:recipe(identifier,silicon(value,replacements[next(iter(match))]))

    for path,key,value in [
            ('assets/anvilcraft/lang/en_us.json','item.anvilcraft.neutronium_ingot','Gravis Ingot'),
            ('assets/anvilcraft/lang/en_us.json','item.anvilcraft.charged_neutronium_ingot','Charged Gravis Ingot'),
            ('assets/bertieprogression/lang/en_us.json','item.bertieprogression.empty_reinforced_carbon_sheet','Untreated Reinforced Carbon Sheet')]:
        lang=json.loads((resources/path).read_text(encoding='utf-8')) if (resources/path).exists() else {}
        lang[key]=value;write(path,lang)
    sheet=copy.deepcopy(effective['bertieprogression:empty_reinforced_carbon_sheet'])
    for value in sheet['key'].values():
        if value.get('item')=='mekanism:ingot_refined_obsidian':value['item']='minecraft:diamond'
    recipe('bertieprogression:empty_reinforced_carbon_sheet',sheet)
    eye=copy.deepcopy(effective['cataclysm:void_eye']);eye['type']='avaritia:shaped_table';eye['tier']=1
    recipe('cataclysm:void_eye',eye)

    # The cap/workstation layer is handled by AnvilCraft's ordinary Giant Anvil matcher.
    size=5;layers=[];out_layers=[]
    for y in range(size):
        layer=[];out=[]
        for z in range(size):
            line='';out_line=''
            for x in range(size):
                boundary=any(c in (0,size-1) for c in (x,y,z))
                line += 'D' if boundary else ('H' if x==y==z==2 else 'L')
                out_line += 'W' if x==y==z==2 else ' '
            layer.append(line);out.append(out_line)
        layers.append(layer);out_layers.append(out)
    wall='compactmachines:wall'
    giant_input={'layers':layers,'symbols':{'D':{'block':'minecraft:polished_deepslate'},'L':{'block':'anvilcraft:levitation_powder_block'},'H':{'block':'anvilcraft:heavy_iron_block'}}}
    recipe('compactmachines:wall',DISABLED)
    recipe('bertieprogression:multiblock/compact_machine_wall',{'type':'anvilcraft:multiblock','pattern':giant_input,'result':{'id':wall,'count':1},'neoforge:conditions':conditions([wall,'anvilcraft:giant_anvil'])})
    recipe('bertieprogression:multiblock_conversion/compact_machine_wall',{'type':'anvilcraft:multiblock_conversion','inputPattern':giant_input,
        'outputPattern':{'layers':out_layers,'symbols':{'W':{'block':wall}}},'neoforge:conditions':conditions([wall,'anvilcraft:giant_anvil'])})
    views=[]
    def mini(identifier,n,wall_block,core,catalyst,output):
        components={'wall':{'type':'compactcrafting:block','block':wall_block}}
        if core:components['core']={'type':'compactcrafting:block','block':core}
        layers=[]
        for y in range(n):
            pattern=[]
            for z in range(n):
                row=[]
                for x in range(n):
                    row.append('wall' if any(c in (0,n-1) for c in (x,y,z)) else ('core' if core and x==y==z==n//2 else '-'))
                pattern.append(row)
            layers.append({'type':'compactcrafting:mixed','pattern':pattern})
        value={'type':'compactcrafting:miniaturization','recipeSize':n,'craftingTime':200,'layers':layers,'components':components,
               'catalyst':{'id':catalyst,'count':1},'outputs':[output],
               'neoforge:conditions':conditions([wall_block,catalyst,output['id'],'compactcrafting:field_projector']+([core] if core else []))}
        recipe(identifier,value)
        views.append({'recipe':identifier,'size':n,'wall':wall_block,'wall_count':n**3-(n-2)**3,'core':core,'catalyst':catalyst,'output':output})
    mini('bertieprogression:miniaturization/compact_machine_wall',3,'minecraft:polished_deepslate',None,'anvilcraft:levitation_powder',{'id':wall,'count':4})
    specs=[('tiny',3,'anvilcraft:heavy_iron_block','mekanism:ingot_steel'),('small',3,'magitech:fluorite_block','irons_spellbooks:arcane_ingot'),
           ('normal',5,'malum:block_of_hallowed_gold','mythsandlegends:bound_soul_ingot'),('large',5,'cognition:cognitive_crystal_block','cognition:astute_assembly'),
           ('giant',7,'anvilcraft:overheated_ember_metal_block','elemental_metals:arcane_infused_netherite_ingot'),('colossal',7,'cataclysm:witherite_block','avaritia:blaze_cube'),
           ('soaryn',9,'avaritia:neutron','anvilcraft:charged_neutronium_ingot')]
    for template,n,core,catalyst in specs:
        old='compactmachines:new_machine_'+template
        output=copy.deepcopy(original[old]['result'])
        mini('bertieprogression:miniaturization/compact_machine_'+template,n,wall,core,catalyst,output)
        recipe(old,DISABLED)
    recipe('compactmachines:new_machine_farming',DISABLED)
    for template in ['tiny','small','normal','large','giant','colossal','soaryn','farming']:
        write('data/compactmachines/advancement/recipes/misc/new_machine_'+template+'.json',{'criteria':{'disabled':{'trigger':'minecraft:impossible'}},'neoforge:conditions':[{'type':'neoforge:false'}]})
    write('compact_mini_recipes.json',views)
    return changed


if __name__=='__main__':
    import argparse
    parser=argparse.ArgumentParser();parser.add_argument('--module',type=Path,required=True);parser.add_argument('--mods',type=Path,required=True)
    args=parser.parse_args();print(json.dumps({'changed':apply(args.module,load_native(args.mods),args.mods)}))
