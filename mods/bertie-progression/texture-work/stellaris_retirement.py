"""Remove Stellaris-only ore forms while preserving unrelated material identities."""
import json
import re
from pathlib import Path

MATERIALS = {'desh', 'neptunium', 'tharsite'}
REMOVED_PROVIDERS = {'stellaris', 'perspatium', 'stellaris_stellarview_compatibility'}
OFF = {'neoforge:conditions': [{'type': 'neoforge:false'}]}


def apply(module):
    module = Path(module)
    resources = module/'src/main/resources'
    table_path = resources/'ore_materials.json'
    table = json.loads(table_path.read_text(encoding='utf-8'))
    rows = [r for r in table['materials'] if r['name'] in MATERIALS]
    previous = table.get('retired_stellaris_materials', {})
    retired_items = {i for i in previous.get('items', []) if i.split(':',1)[1].split('_',1)[0] in MATERIALS}
    retired_chemicals = {i for i in previous.get('chemicals', []) if i.rsplit('_',1)[-1] in MATERIALS}
    for row in rows:
        assert row['ingot'].startswith('stellaris:'), row
        retired_items.update(i for i in row['items'].values() if i.startswith('bertieprogression:'))
        retired_chemicals.update(row['slurries'].values())
    retired = retired_items | retired_chemicals
    assert len(retired_items) == 27 and len(retired_chemicals) == 6, (len(retired_items),len(retired_chemicals))
    changed, deleted = [], []

    def write(path, value):
        text = json.dumps(value,indent=2,ensure_ascii=False)+'\n'
        if not path.exists() or path.read_text(encoding='utf-8') != text:
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(text,encoding='utf-8',newline='\n')
            changed.append(path.relative_to(module).as_posix())

    def bad(identifier):
        return isinstance(identifier,str) and (identifier.lstrip('#') in retired
                or identifier.lstrip('#').split(':',1)[0] in REMOVED_PROVIDERS)

    def contains(value):
        if isinstance(value,str):return bad(value)
        if isinstance(value,list):return any(contains(v) for v in value)
        if isinstance(value,dict):return any(bad(k) or contains(v) for k,v in value.items())
        return False

    table['materials'] = [r for r in table['materials'] if r['name'] not in MATERIALS]
    assert not any(r.get('byproduct') in MATERIALS for r in table['materials'])
    table['new_items'] = [n for n in table['new_items'] if 'bertieprogression:'+n not in retired_items]
    table['new_slurries'] = {n:c for n,c in table['new_slurries'].items() if n not in MATERIALS}
    table['copied'] = [r for r in table['copied'] if r.get('material') not in MATERIALS]
    table['recipes'] = [r for r in table['recipes'] if not any(re.search(r'(?:/|:)'+m+r'(?:_|/|$)',r) for m in MATERIALS)]
    table['retired_stellaris_materials'] = {'materials':sorted(MATERIALS),'items':sorted(retired_items),'chemicals':sorted(retired_chemicals)}
    write(table_path,table)

    java = module/'src/main/java/io/github/bertie_mc/bertieprogression/OreMaterials.java'
    source = java.read_text(encoding='utf-8')
    source = re.sub(r'^\s*"(?:'+ '|'.join(sorted(MATERIALS)) + r')_[^"]+",?\n','',source,flags=re.M)
    source = re.sub(r'^\s*Map\.entry\("(?:'+ '|'.join(sorted(MATERIALS)) + r')",[^\n]+\n','',source,flags=re.M)
    if source != java.read_text(encoding='utf-8'):
        java.write_text(source,encoding='utf-8',newline='\n');changed.append(java.relative_to(module).as_posix())

    names = {i.split(':',1)[1] for i in retired}
    for path in list((resources/'assets/bertieprogression').rglob('*')):
        if path.is_file() and path.name.split('.',1)[0] in names:
            assert path.resolve().is_relative_to(resources.resolve())
            path.unlink();deleted.append(path.relative_to(module).as_posix())
    for path in (resources/'assets').glob('*/lang/*.json'):
        value = json.loads(path.read_text(encoding='utf-8'))
        selected = {f'{kind}.bertieprogression.{i.split(":",1)[1]}' for i in retired for kind in ('item','block','chemical')}
        updated = {k:v for k,v in value.items() if k not in selected}
        if updated != value:write(path,updated)

    def prune(value):
        if isinstance(value,list):
            return [prune(v) for v in value if not bad(v) and not(isinstance(v,dict) and bad(v.get('id')))]
        if isinstance(value,dict):return {k:prune(v) for k,v in value.items() if not bad(k)}
        return value
    for path in (resources/'data').glob('*/recipe/**/*.json'):
        value = json.loads(path.read_text(encoding='utf-8'))
        if contains(value):write(path,OFF)
    for path in (resources/'data').glob('*/tags/**/*.json'):
        value = json.loads(path.read_text(encoding='utf-8'))
        updated = prune(value)
        if updated != value:write(path,updated)

    dust_path = resources/'ore_dust_families.json'
    families = json.loads(dust_path.read_text(encoding='utf-8'))
    old_dusts = {i for row in families for i in row['dusts']}
    eligible = []
    for row in families:
        row = {**row, 'ores':[i for i in row['ores'] if not bad(i)],
               'dusts':[i for i in row['dusts'] if not bad(i)]}
        if row['ores'] and row['dusts']:eligible.append(row)
    write(dust_path,eligible)
    lost_dusts = old_dusts - {i for row in eligible for i in row['dusts']}
    tag_path = resources/'data/bertieprogression/tags/item/ore_material_dusts.json'
    tag = json.loads(tag_path.read_text(encoding='utf-8'))
    tag['values'] = [v for v in tag['values'] if (v.get('id') if isinstance(v,dict) else v) not in lost_dusts]
    write(tag_path,tag)

    removed_path = resources/'removed_items.json'
    removed = json.loads(removed_path.read_text(encoding='utf-8'))
    write(removed_path,[i for i in removed if not bad(i)])
    merged_path = resources/'merged_items.json'
    merged = json.loads(merged_path.read_text(encoding='utf-8'))
    write(merged_path,{k:v for k,v in merged.items() if not bad(k) and not bad(v)})
    for namespace in REMOVED_PROVIDERS:
        for path in (resources/'data'/namespace).rglob('*'):
            if path.is_file():
                assert path.resolve().is_relative_to((resources/'data').resolve())
                path.unlink();deleted.append(path.relative_to(module).as_posix())
    return {'materials':len(table['materials']),'deleted_items':len(retired_items),
            'deleted_chemicals':len(retired_chemicals),'changed':changed,'deleted_assets':deleted}


if __name__ == '__main__':
    import sys
    print(json.dumps(apply(Path(sys.argv[1])),indent=2))
