"""Item-output schemas, including recipes which copy their input equipment."""

import re


def item_outputs(obj, out):
    """Pull every result item id out of a recipe of ANY type/shape."""
    if isinstance(obj, dict):
        if obj.get("type") == "magitech:tool_material":
            return out
        if isinstance(obj.get("type"), str) and obj.get("type") in {"twilightforest:travellers_gear_modifier_shaped_recipe",
                                "twilightforest:travellers_gear_modifier_shapeless_recipe"}:
            def copied_gear(value):
                if isinstance(value, str) and re.fullmatch(r"twilightforest:travellers_(?:goggles|vest|boots|belt|gloves|wings)", value):
                    out.add(value)
                elif isinstance(value, dict):
                    for child in value.values():
                        copied_gear(child)
                elif isinstance(value, list):
                    for child in value:
                        copied_gear(child)
            copied_gear(obj.get("ingredients", obj.get("pattern", {})))
        if obj.get("type") == "twilightforest:travellers_vest_gloves_merge_recipe":
            out.add("twilightforest:travellers_vest")
        for k, v in obj.items():
            if k in ("result", "results"):
                for e in (v if isinstance(v, list) else [v]):
                    if isinstance(e, str):
                        out.add(e)
                    elif isinstance(e, dict):
                        for kk in ("id", "item"):
                            if isinstance(e.get(kk), str):
                                out.add(e[kk])
            elif isinstance(v, (dict, list)):
                item_outputs(v, out)
    elif isinstance(obj, list):
        for v in obj:
            item_outputs(v, out)
    return out
