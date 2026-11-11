"""Apply Pastel's content selection without regenerating unrelated pack integrations."""

import argparse
import copy
import json
import os
import re
import tomllib
import zipfile
from pathlib import Path

from pastel_cleanup import apply_policy
from recipe_outputs import item_outputs


DISABLED = {
    "neoforge:conditions": [{"type": "neoforge:false"}],
    "type": "minecraft:crafting_shapeless",
    "ingredients": [{"item": "minecraft:stone"}],
    "result": {"id": "minecraft:stone", "count": 1},
}
WITHDRAWN_TYPES = (
    "pastel:enchanter", "pastel:enchantment_upgrade", "pastel:crystallarieum",
    "pastel:cinderhearth", "pastel:potion_workshop", "pastel:titration_barrel",
    "pastel:spirit_instiller", "pastel:natures_staff", "pastel:heating", "pastel:freezing",
    "pastel:ink_converting", "pastel:humus_converting", "pastel:dragonrot_converting",
)


def strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, list):
        for child in value:
            yield from strings(child)
    elif isinstance(value, dict):
        for child in value.values():
            yield from strings(child)


def disabled(value):
    return {"type": "neoforge:false"} in value.get("neoforge:conditions", [])


def withdrawn_recipe(value, removed, retired_fluids=()):
    outputs = item_outputs(value, set())
    retired_fusion = value.get("type") == "pastel:fusion_shrine" and (
        "minecraft:netherite_ingot" in outputs or any(item.endswith("_horse_armor") for item in outputs))
    return (bool(outputs & removed) or value.get("type", "").startswith(WITHDRAWN_TYPES) or retired_fusion
            or any(entry.removeprefix("#") in retired_fluids for entry in strings(value)))


def strip_loot(value, removed):
    if isinstance(value, list):
        return [child for entry in value if (child := strip_loot(entry, removed)) is not None]
    if not isinstance(value, dict):
        return value
    if value.get("type") == "minecraft:item" and value.get("name") in removed:
        return None
    result = {}
    for key, child in value.items():
        fixed = strip_loot(child, removed)
        if key in {"entries", "children"} and child and not fixed:
            return None
        result[key] = fixed
    return result


def redirect(value, merges):
    if isinstance(value, str):
        return merges.get(value, value)
    if isinstance(value, list):
        return [redirect(child, merges) for child in value]
    if isinstance(value, dict):
        return {key: redirect(child, merges) for key, child in value.items()}
    return value


def clean_feature(value, removed):
    if isinstance(value, list):
        return [clean_feature(child, removed) for child in value]
    if not isinstance(value, dict):
        return value
    if value.get("Name") in removed:
        return {"Name": "minecraft:air"}
    result = {key: clean_feature(child, removed) for key, child in value.items()}
    if value.get("type") == "minecraft:weighted_state_provider":
        result["entries"] = [clean_feature(entry, removed) for entry in value["entries"]
                             if entry.get("data", {}).get("Name") not in removed]
        if not result["entries"]:
            return {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:air"}}
    return result


def read(path, default=None):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else copy.deepcopy(default)


def canonical_pastel_removals(workspace):
    text = (workspace / "docs/removed/pastel.md").read_text(encoding="utf-8").split("<!-- LEAKS:", 1)[0]
    ids = set(re.findall(r"\|\s*`(pastel:[a-z0-9_./-]+)`\s*\|", text))
    if not ids:
        raise ValueError("The canonical Pastel removal table is empty")
    return ids


def pack_jars(repo, mods):
    locked = set()
    for directory in (repo / "deps/components", repo / "deps/locks"):
        for path in directory.glob("*.toml"):
            locked.update(re.findall(r'^filename = "([^"]+)"$', path.read_text(encoding="utf-8"), re.M))
    jars = [path for path in sorted(mods.glob("*.jar")) if path.name in locked]
    platform = tomllib.loads((repo / "deps/platform.toml").read_text(encoding="utf-8"))
    libraries = Path(os.environ["APPDATA"]) / "PrismLauncher/libraries"
    minecraft, loader = platform["minecraft"], platform["loader-version"]
    for path in (libraries / f"com/mojang/minecraft/{minecraft}/minecraft-{minecraft}-client.jar",
                 libraries / f"net/neoforged/neoforge/{loader}/neoforge-{loader}-universal.jar"):
        if not path.exists():
            raise FileNotFoundError(f"Required tag contributions are missing: {path}")
        jars.append(path)
    return jars


def load_sources(jars):
    recipes, loot, tags, features = {}, {}, {}, {}
    for jar in jars:
        with zipfile.ZipFile(jar) as archive:
            for name in archive.namelist():
                if not name.startswith("data/") or not name.endswith(".json"):
                    continue
                relevant = "/recipe/" in name or "/loot_table/" in name or "/tags/item/" in name
                worldgen = name.startswith(("data/pastel/worldgen/configured_feature/",
                                            "data/pastel/worldgen/placed_feature/"))
                if not relevant and not worldgen:
                    continue
                data = archive.read(name)
                if "/tags/item/" not in name and b"pastel:" not in data and not name.startswith("data/pastel/"):
                    continue
                value = json.loads(data)
                if "/recipe/" in name:
                    recipes[name] = value
                elif "/loot_table/" in name:
                    loot[name] = value
                elif "/tags/item/" in name:
                    tags.setdefault(name, []).append(value)
                elif worldgen:
                    features[name] = value
    return recipes, loot, tags, features


def finalize(module, workspace, jars):
    resources = module / "src/main/resources"
    policy = read(module / "texture-work/pastel_content_policy.json")
    worldgen = read(module / "texture-work/pastel_worldgen_policy.json")
    fluid_replacements = worldgen.get("fluid_replacements", {})
    retired_fluids = set(fluid_replacements)
    for identifier in fluid_replacements:
        namespace, name = identifier.split(":", 1)
        retired_fluids.add(f"{namespace}:flowing_{name}")
    old_manifest = read(module / "texture-work/.pastel_finalized.json", {})
    old_removed = set(read(resources / "removed_items.json", []))
    removed = {item for item in old_removed if not item.startswith("pastel:")} | canonical_pastel_removals(workspace)
    merges = read(resources / "merged_items.json", {})
    item_merges = policy.get("item_merges", {})
    merges.update(item_merges)
    recipes, loot, tags, features = load_sources(jars)
    recipes = {path: redirect(recipe, item_merges) for path, recipe in recipes.items()}
    written = []

    def write(relative, value):
        path = resources / relative
        if read(path) != value:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
        written.append(relative)

    write("removed_items.json", sorted(removed))
    if item_merges:
        write("merged_items.json", merges)
    restored = old_removed - removed
    restored_resources = restored | set(policy.get("restore_recipes_for", []))
    pending_paths = {row["path"] for row in read(resources / "pastel_recipe_pending.json", [])}
    recovery_paths = pending_paths | set(old_manifest.get("disabled_recipes", []))
    restore_outputs = restored_resources
    for path, original in recipes.items():
        current = read(resources / path)
        outputs = item_outputs(original, set())
        recover = path in recovery_paths and any(not item.startswith("minecraft:") for item in outputs)
        can_rehome = original.get("type") in {"pastel:pedestal", "pastel:pedestal_shapeless", "pastel:spirit_instiller"}
        if (current is None or disabled(current)) and not disabled(original) and (can_rehome or not withdrawn_recipe(
                original, removed, retired_fluids)) and (
                outputs & restore_outputs or recover and not outputs & removed):
            write(path, redirect(original, merges))

    # Existing active overrides include newer integration recipes absent from the legacy generator.
    authored = [path.relative_to(resources).as_posix() for path in (resources / "data").rglob("*.json")
                if "/recipe/" in path.as_posix()]
    for path in authored:
        current = read(resources / path)
        fixed = redirect(current, item_merges)
        if fixed != current:
            write(path, fixed)
    apply_policy(write, recipes, resources, authored, removed, DISABLED)

    effective = dict(recipes)
    for path in (resources / "data").rglob("*.json"):
        if "/recipe/" in path.as_posix():
            effective[path.relative_to(resources).as_posix()] = read(path)
    disabled_paths = set()
    for path, value in effective.items():
        if disabled(value):
            if withdrawn_recipe(recipes.get(path, {}), removed, retired_fluids):
                disabled_paths.add(path)
            continue
        if withdrawn_recipe(value, removed, retired_fluids):
            write(path, DISABLED)
            disabled_paths.add(path)
        elif value.get("type", "").startswith("pastel:") and value.get("required_advancement"):
            value = copy.deepcopy(value)
            value.pop("required_advancement")
            write(path, value)

    removed_data_path = module / "texture-work/.removed_data.json"
    removed_data = set(read(removed_data_path, []))
    for path, original in loot.items():
        if not set(strings(original)) & (removed | restored_resources):
            continue
        current = read(resources / path)
        source = original if path in removed_data or current is None else current
        source = redirect(source, merges)
        fixed = strip_loot(source, removed)
        if fixed is None:
            fixed = {key: value for key, value in source.items() if key not in {"pools", "functions"}}
            fixed["pools"] = []
        if fixed != source or path in removed_data:
            write(path, fixed)
            removed_data.add(path)

    merged_tags = set(read(module / "texture-work/.merged_tags.json", []))
    for path, contributions in tags.items():
        values = []
        for contribution in contributions:
            values = ([] if contribution.get("replace") else values) + contribution.get("values", [])
        original_values = list(values)
        override = read(resources / path)
        if override is not None:
            # Preserve authored restrictions and contributions from nested dependencies.
            values = list(override.get("values", [])) if override.get("replace") else values + override.get("values", [])
            for entry in original_values:
                identifier = entry.get("id") if isinstance(entry, dict) else entry
                if identifier in restored_resources:
                    values.append(entry)
        if path.endswith("/hidden_from_recipe_viewers.json"):
            values = [entry for entry in values
                      if (entry.get("id") if isinstance(entry, dict) else entry) not in restored_resources]
            existing = set(strings(values))
            values += [{"id": item, "required": False} for item in sorted(removed - existing)
                       if item.startswith("pastel:")]
            write(path, {"replace": True, "values": values})
            continue
        fixed, seen = [], set()
        for entry in values:
            value = entry.get("id") if isinstance(entry, dict) else entry
            value = merges.get(value, value)
            if value in removed or value in seen:
                continue
            seen.add(value)
            fixed.append(value if value.startswith("#") else {"id": value, "required": False})
        if set(strings(original_values + values)) & {item for item in removed | restored_resources if item.startswith("pastel:")}:
            write(path, {"replace": True, "values": fixed})
    for path in (resources / "data").glob("*/tags/item/**/*.json"):
        relative = path.relative_to(resources).as_posix()
        if relative in tags:
            continue
        value = read(path)
        fixed = [entry for entry in value.get("values", [])
                 if (entry.get("id") if isinstance(entry, dict) else entry) not in removed]
        if fixed != value.get("values", []):
            write(relative, {**value, "values": fixed})

    for feature in worldgen["configured_features"]:
        write(f"data/pastel/worldgen/configured_feature/{feature}.json", {"type": "minecraft:no_op", "config": {}})
    for modifier in worldgen["biome_modifiers"]:
        write(f"data/pastel/neoforge/biome_modifier/{modifier}.json", {"type": "neoforge:none"})
    for path, feature in features.items():
        configured = path.startswith("data/pastel/worldgen/configured_feature/")
        name = path.removeprefix("data/pastel/worldgen/configured_feature/").removesuffix(".json")
        if configured and (name in worldgen["configured_features"] or name.startswith("colored_trees/")):
            continue
        fixed = redirect(feature, fluid_replacements)
        if configured and set(strings(fixed)) & removed:
            fixed = clean_feature(fixed, removed)
        if fixed != feature:
            # These overrides reference registries owned by Pastel, including fossil and lake materials.
            if set(strings(feature)) & set(fluid_replacements):
                fixed["neoforge:conditions"] = [{"type": "neoforge:mod_loaded", "modid": "pastel"}]
            write(path, fixed)

    recipe_manifest = module / "texture-work/.removed_recipes.json"
    manifest = set(read(recipe_manifest, []))
    manifest = {path for path in manifest if not item_outputs(recipes.get(path, {}), set()) & restored}
    manifest |= {path for path in disabled_paths if item_outputs(recipes.get(path, {}), set()) & removed}
    recipe_manifest.write_text(json.dumps(sorted(manifest), indent=2) + "\n", encoding="utf-8", newline="\n")
    removed_data_path.write_text(json.dumps(sorted(removed_data), indent=2) + "\n", encoding="utf-8", newline="\n")
    result = {"removed_ids": sorted(item for item in removed if item.startswith("pastel:")),
              "disabled_recipes": sorted(disabled_paths)}
    (module / "texture-work/.pastel_finalized.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8", newline="\n")
    audit_path = workspace / "s1-build/pastel-removal-audit.json"
    if audit_path.exists():
        audit = read(audit_path)
        audit["remove_ids"] = result["removed_ids"]
        audit_path.write_text(json.dumps(audit, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    doc_path = workspace / "docs/removed/pastel.md"
    document = doc_path.read_text(encoding="utf-8")
    opening, closing = "<!-- LEAKS: generated every build, do not edit by hand -->", "<!-- /LEAKS -->"
    if opening in document and closing in document:
        leaks = {}

        def collect(value, path):
            if isinstance(value, list):
                for child in value:
                    collect(child, path)
            elif isinstance(value, dict):
                if value.get("type") == "minecraft:item" and value.get("name") in removed:
                    leaks.setdefault(value["name"], set()).add(path)
                for child in value.values():
                    collect(child, path)

        for path, value in loot.items():
            collect(value, path)
        lines = ["", "", "## Loot", "", "Loot tables rewritten without withdrawn item entries.", ""]
        for item in result["removed_ids"]:
            if item not in leaks:
                continue
            sources = sorted(leaks[item])
            lines.append(f"- **{item}** — removed from {len(sources)} loot table(s):")
            lines.extend(f"  - `{path}`" for path in sources[:8])
            if len(sources) > 8:
                lines.append(f"  - ...and {len(sources) - 8} more")
        before, tail = document.split(opening, 1)
        after = tail.split(closing, 1)[1]
        doc_path.write_text(before + opening + "\n".join(lines) + "\n\n" + closing + after,
                            encoding="utf-8", newline="\n")
    return {"pastel_removed": len(result["removed_ids"]), "restored": len(restored),
            "disabled_recipes": len(disabled_paths), "written": len(set(written))}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--workspace", type=Path, default=os.environ.get("BERTIE_WORKSPACE"), required=not os.environ.get("BERTIE_WORKSPACE"))
    parser.add_argument("--mods", type=Path, default=Path(os.environ["APPDATA"]) / "PrismLauncher/instances/bertie-no-worldgen/minecraft/mods")
    args = parser.parse_args()
    module = Path(__file__).resolve().parent.parent
    print(json.dumps(finalize(module, args.workspace, pack_jars(module.parent.parent, args.mods)), indent=2))
