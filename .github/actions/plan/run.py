#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import subprocess
from pathlib import Path


def enabled(name: str) -> bool:
    return os.environ.get(name, "false").lower() == "true"


def main() -> None:
    command = ["bertie-ci", "plan", "--workspace", "."]
    base = os.environ.get("BERTIE_PLAN_BASE", "")
    if base and set(base) != {"0"}:
        command.extend(("--base", base, "--head", os.environ["BERTIE_PLAN_HEAD"]))
    elif base or enabled("BERTIE_PLAN_ALL"):
        command.append("--all")
    component = os.environ.get("BERTIE_PLAN_COMPONENT", "")
    if component:
        command.extend(("--component", component))
    if enabled("BERTIE_PLAN_INCLUDE_MANUAL"):
        command.append("--include-manual")

    result = subprocess.run(
        command,
        check=True,
        stdout=subprocess.PIPE,
        text=True,
    )
    plan = json.loads(result.stdout)
    output = Path(os.environ["GITHUB_OUTPUT"])
    with output.open("a", encoding="utf-8") as stream:
        for name in ("build", "unit", "gametest", "client", "server", "validate"):
            value = json.dumps(plan[name], separators=(",", ":"))
            stream.write(f"{name}={value}\n")


if __name__ == "__main__":
    main()
