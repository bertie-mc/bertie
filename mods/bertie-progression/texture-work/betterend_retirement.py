"""Remove BetterEnd data while preserving the pack's surviving recipe consumers."""

import json
from pathlib import Path

REPLACEMENTS = {
    "betterend:leather_wrapped_stick": "minecraft:stick",
    "betterend:leather_stripe": "minecraft:leather",
}


def rewrite(value):
    if isinstance(value, str):
        return REPLACEMENTS.get(value, value)
    if isinstance(value, list):
        return [rewrite(child) for child in value]
    if isinstance(value, dict):
        result = {key: rewrite(child) for key, child in value.items()}
        if "neoforge:conditions" in result:
            result["neoforge:conditions"] = [
                condition for condition in result["neoforge:conditions"]
                if not (condition.get("type") == "neoforge:mod_loaded"
                        and condition.get("modid") == "betterend")
            ]
        return result
    return value


def apply(module):
    resources = Path(module) / "src/main/resources"
    removed, changed = [], []
    for path in sorted(resources.rglob("*.json")):
        relative = path.relative_to(resources).as_posix()
        if relative.startswith(("data/betterend/", "assets/betterend/")):
            path.unlink()
            removed.append(relative)
            continue
        if "/recipe/" not in relative:
            continue
        original = json.loads(path.read_text(encoding="utf-8"))
        outputs = original.get("result", original.get("results", []))
        if '"betterend:' in json.dumps(outputs) or relative.startswith(
                "data/bertieprogression/recipe/slabs/betterend__"):
            path.unlink()
            removed.append(relative)
            continue
        updated = rewrite(original)
        if '"betterend:' in json.dumps(updated):
            raise ValueError(f"Unresolved BetterEnd ingredient in {relative}")
        if updated != original:
            path.write_text(json.dumps(updated, indent=2, ensure_ascii=False) + "\n",
                            encoding="utf-8", newline="\n")
            changed.append(relative)
    return removed, changed


if __name__ == "__main__":
    import sys

    removed, changed = apply(Path(sys.argv[1]))
    print(f"Retired {len(removed)} BetterEnd resources; updated {len(changed)} recipes")
