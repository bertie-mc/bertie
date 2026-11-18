"""Keep the Bottle of Nothing resource bundle reproducible."""
import json
import shutil
from pathlib import Path

def apply(module):
    module=Path(module);res=module/'src/main/resources'
    source=module/'texture-work/approved/bottle_of_nothing'
    for path in source.iterdir():
        shutil.copyfile(path,res/'assets/bertieprogression/textures/item'/path.name)
    path=res/'assets/bertieprogression/lang/en_us.json'
    lang=json.loads(path.read_text(encoding='utf-8'));lang['item.bertieprogression.bottle_of_nothing']='Bottle of Nothing'
    path.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    path=res/'assets/bertieprogression/models/item/bottle_of_nothing.json'
    path.write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'bertieprogression:item/bottle_of_nothing'}},indent=2)+'\n',encoding='utf-8')
