"""Keep the approved computing-engine stages available as quest objectives."""

import json
from pathlib import Path


def apply(module):
    resources = Path(module) / 'src/main/resources'
    language = resources / 'assets/bertieprogression/lang/en_us.json'
    lang = json.loads(language.read_text(encoding='utf-8'))
    for stage in range(1, 4):
        identifier = f'advanced_computing_engine_stage_{stage}'
        path = resources / f'assets/bertieprogression/models/item/{identifier}.json'
        path.write_text(json.dumps({'parent': 'minecraft:item/generated',
                                   'textures': {'layer0': 'bertieprogression:item/' + identifier}}, indent=2) + '\n',
                        encoding='utf-8', newline='\n')
        lang['item.bertieprogression.' + identifier] = f'Advanced Computing Engine Stage {stage}'
    language.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + '\n', encoding='utf-8', newline='\n')
