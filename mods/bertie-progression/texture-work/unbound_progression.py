"""Apply the selected Unbound acquisition and material bindings."""
import json
import re
from pathlib import Path

def apply(module):
    module=Path(module);res=module/'src/main/resources'
    bundle=module/'texture-work/unbound_055'
    plan=json.loads((bundle/'plan.json').read_text(encoding='utf-8'))
    retired=set(plan['retired'])|{'bertieprogression:pocket_watch','bertieprogression:pocket_dimension'}
    def write(path,value):
        p=res/path;p.parent.mkdir(parents=True,exist_ok=True)
        text=json.dumps(value,indent=2,ensure_ascii=False)+'\n'
        if not p.exists() or p.read_text(encoding='utf-8')!=text:p.write_text(text,encoding='utf-8')
    def remap(value):
        if isinstance(value,str):return plan['merges'].get(value,value)
        if isinstance(value,list):return [remap(v) for v in value]
        if isinstance(value,dict):return {plan['merges'].get(k,k):remap(v) for k,v in value.items()}
        return value
    for p in (res/'data').glob('*/recipe/**/*.json'):
        value=json.loads(p.read_text(encoding='utf-8'));updated=remap(value)
        result=value.get('result',{})
        identifier=result.get('id',result.get('item','')) if isinstance(result,dict) else result
        if (isinstance(identifier,str) and identifier in retired and identifier not in {'unbound:titanium_ingot','unbound:titanium_dust'}) or 'pocket_dimension:' in json.dumps(value):updated={'neoforge:conditions':[{'type':'neoforge:false'}]}
        if updated!=value:write(p.relative_to(res),updated)
    for path,value in {**plan['recipes'],**plan['loot']}.items():write(path,value)
    removed=json.loads((res/'removed_items.json').read_text(encoding='utf-8'));write('removed_items.json',sorted(set(removed)|retired))
    merged=json.loads((res/'merged_items.json').read_text(encoding='utf-8'));merged.update(plan['merges']);write('merged_items.json',merged)
    for path in ['data/c/tags/item/hidden_from_recipe_viewers.json','data/emi/tag/exclusions/hidden.json']:
        p=res/path
        if p.exists():
            value=json.loads(p.read_text(encoding='utf-8'))
            values=value.get('values',[]);present={str(v) for v in values}
            values.extend(v for v in sorted(retired) if str(v) not in present);value['values']=values;write(path,value)
    langpath='assets/unbound/lang/en_us.json';p=res/langpath
    lang=json.loads(p.read_text(encoding='utf-8')) if p.exists() else {};lang.update(plan['lang']);write(langpath,lang)
    lang=json.loads((res/'assets/bertieprogression/lang/en_us.json').read_text(encoding='utf-8'))
    for k in ['item.bertieprogression.pocket_watch','block.bertieprogression.pocket_dimension']:lang.pop(k,None)
    lang['item.bertieprogression.incomplete_copper_gear']='Incomplete Copper Gear';write('assets/bertieprogression/lang/en_us.json',lang)
    write('assets/bertieprogression/models/item/incomplete_copper_gear.json',{'parent':'minecraft:item/generated','textures':{'layer0':'unbound:item/copper_gear'}})
    # Keep Ilmenite out of ordinary raw-metal block compression/smelting shortcuts.
    processing_tag='bertieprogression:raw_titanium_processing'
    write('data/bertieprogression/tags/item/raw_titanium_processing.json',{'replace':False,'values':[{'id':'#c:raw_materials/titanium','required':False},{'id':'anvilcraft:raw_titanium','required':False},'unbound:raw_ilmenite']})
    def processing(value):
        if isinstance(value,dict):
            if value.get('item')=='anvilcraft:raw_titanium':return {**{k:v for k,v in value.items() if k!='item'},'tag':processing_tag}
            return {k:(processing_tag if k=='tag' and v=='c:raw_materials/titanium' else processing(v)) for k,v in value.items()}
        if isinstance(value,list):return [processing(v) for v in value]
        return value
    for p in (res/'data').glob('*/recipe/**/*.json'):
        value=json.loads(p.read_text(encoding='utf-8'));rtype=value.get('type','')
        if rtype.startswith(('mekanism:','oritech:','create:','magitech:')) and any(raw in json.dumps(value) for raw in ['c:raw_materials/titanium','anvilcraft:raw_titanium']):write(p.relative_to(res),processing(value))
    p=res/'data/bertieprogression/recipe/centrifuge/titanium.json';recipe=json.loads(p.read_text());recipe['results'][1]={'id':'bertieprogression:iron_spec','count':3};write(p.relative_to(res),recipe)
    p=res/'ore_materials.json';table=json.loads(p.read_text())
    for material in table['materials']:
        if material['name']=='titanium':material['byproduct']='iron'
    write('ore_materials.json',table)
    write('unbound_material_aliases.json',{'merges':plan['merges'],'retired':sorted(retired),'guide_removed':plan['guide_removed'],'guide_merges':plan['guide_merges']})
    write('unbound_spectral_extension.json',json.loads((bundle/'spectral.json').read_text()))
    write('unbound_spectral_bindings.json',json.loads((bundle/'bindings.json').read_text()))
    for path in ['assets/bertieprogression/blockstates/mechanical_adapter.json']:
        p=res/path;data=json.loads(p.read_text());data['variants']={k.replace(',assembled=',',machine_assembled='):v for k,v in data['variants'].items()};write(path,data)
    old_loot=res/'data/bertieprogression/loot_table/blocks/pocket_dimension.json'
    if old_loot.exists():old_loot.unlink()
    for base in ['models/item','models/block','blockstates','textures/item','textures/block']:
        for name in ['pocket_dimension','pocket_watch']:
            for p in (res/'assets/bertieprogression'/base).glob(name+'.*'):
                if p.is_file():p.unlink()

if __name__=='__main__':
    import sys
    apply(sys.argv[1])
