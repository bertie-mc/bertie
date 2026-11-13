import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from shared_tag_replacements import apply, replace_entries


def test_partial_pack_tag_does_not_erase_other_mods(tmp_path):
    resources = tmp_path / 'resources'
    mods = tmp_path / 'mods'
    mods.mkdir()
    name = 'data/minecraft/tags/block/mineable/pickaxe.json'
    target = resources / name
    target.parent.mkdir(parents=True)
    target.write_text(json.dumps({'replace': True, 'values': [{'id': 'anvilcraft:sturdy_deepslate', 'required': False}]}))
    with ZipFile(mods / 'native.jar', 'w') as jar:
        jar.writestr(name, json.dumps({'values': ['oritech:plastic_sheet_block', 'oritech:ore_block']}))
    result = {}
    def write(name, value):
        result[name] = value
        (resources / name).write_text(json.dumps(value))
    apply(resources, mods, {'oritech:plastic_sheet_block': 'mekanism:hdpe_sheet'}, write)
    tag = result[name]
    assert tag['replace'] is False
    assert {'id': 'anvilcraft:sturdy_deepslate', 'required': False} in tag['values']
    assert {'id': 'oritech:ore_block', 'required': False} in tag['values']
    assert all(v.get('id') != 'mekanism:hdpe_sheet' for v in tag['values'])
    assert {'id': 'oritech:plastic_sheet_block', 'required': False} in tag['remove']
    apply(resources, mods, {'oritech:plastic_sheet_block': 'mekanism:hdpe_sheet'}, write)
    assert result[name] == tag


def test_replacement_preserves_vanilla_and_optional_reference_semantics():
    tag = replace_entries({'replace': False, 'values': ['minecraft:glass',
                          {'id': 'pastel:semi', 'required': False}]}, {'pastel:semi': 'pastel:normal'})
    assert tag['values'] == ['minecraft:glass', {'id': 'pastel:normal', 'required': False}]
    assert tag['remove'] == [{'id': 'pastel:semi', 'required': False}]


def test_item_only_sheet_cannot_enter_block_tags():
    tag = replace_entries({'values': ['mekanism:hdpe_sheet', 'minecraft:stone']}, {}, block=True)
    assert tag['values'] == ['minecraft:stone']


class SharedTagReplacementTests(unittest.TestCase):
    def test_partial_tag_merge(self):
        with TemporaryDirectory() as directory:
            test_partial_pack_tag_does_not_erase_other_mods(Path(directory))

    def test_optional_references(self):
        test_replacement_preserves_vanilla_and_optional_reference_semantics()

    def test_item_only_sheet(self):
        test_item_only_sheet_cannot_enter_block_tags()
