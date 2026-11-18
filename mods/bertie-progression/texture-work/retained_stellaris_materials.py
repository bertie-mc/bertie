"""Host the selected material families and original licensed art in Bertie."""
import json
import re
from pathlib import Path


def apply(module):
    module=Path(module);resources=module/'src/main/resources'
    source=module/'texture-work/retained_stellaris'
    plan=json.loads((source/'plan.json').read_text(encoding='utf-8'))
    changed=[]
    def write(relative,value):
        path=resources/relative;text=json.dumps(value,indent=2,ensure_ascii=False)+'\n'
        if not path.exists() or path.read_text(encoding='utf-8')!=text:
            path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text,encoding='utf-8',newline='\n');changed.append(str(relative))
    for path in (source/'assets').rglob('*'):
        if path.is_file():
            relative=path.relative_to(source/'assets');target=resources/relative
            if not target.exists() or target.read_bytes()!=path.read_bytes():
                target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(path.read_bytes());changed.append(relative.as_posix())
    for relative,value in {**plan['recipes'],**plan['loot']}.items():write(relative,value)
    for relative,values in plan['tags'].items():
        path=resources/relative
        current=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace':False,'values':[]}
        for value in values:
            if value not in current['values']:current['values'].append(value)
        write(relative,current)
    langpath='assets/bertieprogression/lang/en_us.json'
    lang=json.loads((resources/langpath).read_text(encoding='utf-8'));lang.update(plan['lang']);write(langpath,lang)
    spec={**plan['registration'],'recipe_ids':[
        Path(p).parts[1]+':'+ '/'.join(Path(p).parts[3:])[:-5] for p in plan['recipes']]}
    write('retained_stellaris_materials.json',spec)
    write('retained_stellaris_asset_provenance.json',{'source':plan['source'],'assets':plan['native_assets'],
          'adaptation':'Original pixels; namespace and material references adapted for Bertie Progression.'})

    meta=json.loads((resources/'ore_materials.json').read_text(encoding='utf-8'))
    retained={r['name'] for r in plan['rows']}
    meta['materials']=[r for r in meta['materials'] if r['name'] not in retained]+plan['rows']
    item_order={name:i for i,name in enumerate(plan['old_item_order'])}
    meta['new_items']=sorted(set(meta['new_items'])|set(plan['items']),key=lambda name:(item_order.get(name,100000),name))
    meta['new_slurries'].update(plan['slurries'])
    meta['copied']=[r for r in meta['copied'] if r.get('material') not in retained]+plan['copied']
    for relative in plan['recipes']:
        parts=Path(relative).parts;rid=parts[1]+':'+ '/'.join(parts[3:])[:-5]
        if rid not in meta['recipes']:meta['recipes'].append(rid)
    meta['retained_stellaris_families']=['corronium','solerium','plutonium']
    meta['corronium_byproduct']='eezo; former Tharsite -> Desh -> Eezo chain contracted'
    write('ore_materials.json',meta)
    java=module/'src/main/java/io/github/bertie_mc/bertieprogression/OreMaterials.java'
    items=',\n'.join('            '+json.dumps(name) for name in meta['new_items'])
    chemicals=',\n'.join(f'            Map.entry("{name}", 0x{color:06X})' for name,color in meta['new_slurries'].items())
    content=('package io.github.bertie_mc.bertieprogression;\n\nimport java.util.List;\nimport java.util.Map;\n\n'
             'public final class OreMaterials {\n    public static final List<String> ITEMS = List.of(\n'+items+');\n\n'
             '    public static final Map<String, Integer> SLURRIES = Map.ofEntries(\n'+chemicals+');\n\n'
             '    private OreMaterials() {}\n}\n')
    # Source formatting is delegated to Spotless, as with the original ore importer.
    if re.sub(r'\s+','',java.read_text(encoding='utf-8'))!=re.sub(r'\s+','',content):
        java.write_text(content,encoding='utf-8',newline='\n')
    eligibility_path='ore_dust_families.json'
    eligible=json.loads((resources/eligibility_path).read_text(encoding='utf-8'))
    names={r['material'] for r in plan['eligibility']}
    eligible=[r for r in eligible if r['material'] not in names]+plan['eligibility']
    write(eligibility_path,eligible)
    dust_tag='data/bertieprogression/tags/item/ore_material_dusts.json'
    tag=json.loads((resources/dust_tag).read_text(encoding='utf-8'))
    for row in plan['eligibility']:
        for item in row['dusts']:
            if item not in tag['values']:tag['values'].append(item)
    write(dust_tag,tag)
    return changed


if __name__=='__main__':
    import sys
    print(json.dumps({'changed':apply(Path(sys.argv[1]))}))
