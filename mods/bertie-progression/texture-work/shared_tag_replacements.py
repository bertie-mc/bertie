"""Retire individual native tag entries without replacing other mods' contributions."""

import json
from pathlib import Path
from zipfile import ZipFile

SHARED = {'minecraft', 'neoforge', 'forge', 'c'}


def retire_native_tag_entries(resources, jar, token, write):
    resources = Path(resources)
    with ZipFile(jar) as archive:
        for name in archive.namelist():
            if '/tags/' not in name or not name.endswith('.json') or token.encode() not in archive.read(name):
                continue
            original = json.loads(archive.read(name))
            target = resources / name
            value = json.loads(target.read_text()) if target.exists() else dict(original)
            if token in Path(name).stem:
                value = {'replace': True, 'values': []}
            else:
                removed = [v for v in original.get('values', []) if token in str(v)]
                value['values'] = [v for v in value.get('values', []) if token not in str(v)]
                value['replace'] = False
                removals = value.setdefault('remove', [])
                for entry in removed:
                    entry = {'id': entry if isinstance(entry, str) else entry['id'], 'required': False}
                    if entry not in removals:
                        removals.append(entry)
            write(name, value)


def normalize_optional_references(resources, write):
    """Optional integration entries must not invalidate shared tags when their provider is absent."""
    resources = Path(resources)
    for path in (resources / 'data').glob('*/tags/**/*.json'):
        if path.relative_to(resources / 'data').parts[0] not in SHARED:
            continue
        original = json.loads(path.read_text())
        value = dict(original)
        value['values'] = []
        for entry in original.get('values', []):
            identifier = entry if isinstance(entry, str) else entry['id']
            if identifier.lstrip('#').split(':')[0] not in {'minecraft', 'bertieprogression'}:
                entry = {'id': identifier, 'required': False}
            if entry not in value['values']:
                value['values'].append(entry)
        if value != original:
            write(path.relative_to(resources).as_posix(), value)


def replace_entries(original, replacements, block=False):
    values, removed = [], []
    for value in original.get('values', []):
        identifier = value if isinstance(value, str) else value.get('id', '')
        if block and identifier == 'mekanism:hdpe_sheet':
            continue
        if identifier in replacements:
            removed.append({'id': identifier, 'required': False})
            if block:
                continue
            replacement = replacements[identifier]
            value = replacement if isinstance(value, str) else {**value, 'id': replacement}
        if value not in values:
            values.append(value)
    result = dict(original)
    result['values'] = []
    for value in values:
        identifier = value if isinstance(value, str) else value['id']
        if identifier.lstrip('#').split(':')[0] not in {'minecraft', 'bertieprogression'}:
            value = {'id': identifier, 'required': False}
        if value not in result['values']:
            result['values'].append(value)
    result['remove'] = original.get('remove', []) + [v for v in removed if v not in original.get('remove', [])]
    return result


def apply(resources, mods, replacements, write, scan_replacements=None):
    resources = Path(resources)
    affected = {}
    for jar in Path(mods).glob('*.jar'):
        if jar.name.startswith('bertieprogression'):
            continue
        with ZipFile(jar) as archive:
            for name in archive.namelist():
                if '/tags/' not in name or not name.endswith('.json'):
                    continue
                raw = archive.read(name)
                if any(item.encode() in raw for item in (scan_replacements or replacements)):
                    affected.setdefault(name, []).append(json.loads(raw))
    for name, originals in affected.items():
        target = resources / name
        value = json.loads(target.read_text()) if target.exists() else {'values': []}
        for original in originals:
            for entry in original.get('values', []):
                if entry not in value['values']:
                    value['values'].append(entry)
        value = replace_entries(value, replacements, '/tags/block/' in name)
        value['replace'] = False if name.split('/')[1] in SHARED else value.get('replace', False)
        write(name, value)
