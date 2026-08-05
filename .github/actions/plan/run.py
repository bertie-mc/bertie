#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path


def enabled(name: str) -> bool:
    return os.environ.get(name, "false").lower() == "true"


def main() -> None:
    repository = Path(__file__).resolve().parents[3]
    source = repository / "tools" / "bertie-ci" / "src"
    environment = os.environ.copy()
    environment["PYTHONPATH"] = os.pathsep.join(
        filter(None, (str(source), environment.get("PYTHONPATH", "")))
    )
    command = [sys.executable, "-m", "bertie_ci.cli", "plan", "--workspace", "."]
    base = os.environ.get("BERTIE_PLAN_BASE", "")
    if base and set(base) != {"0"}:
        command.extend(("--base", base, "--head", os.environ["BERTIE_PLAN_HEAD"]))
    elif base or enabled("BERTIE_PLAN_ALL"):
        command.append("--all")
    component = os.environ.get("BERTIE_PLAN_COMPONENT", "")
    if component:
        command.extend(("--component", component))
    result = subprocess.run(
        command,
        check=True,
        env=environment,
        stdout=subprocess.PIPE,
        text=True,
    )
    plan = json.loads(result.stdout)
    has_work = any(
        plan[name] for name in ("build", "unit", "gametest", "client", "validate")
    )
    build_unit_tasks: list[str] = []
    if has_work:
        build_unit_tasks.append("testInfrastructure")
    for name in ("build", "unit"):
        build_unit_tasks.extend(entry["task"] for entry in plan[name])
    generation_tasks = [
        f"{entry['gradle_project'].rstrip(':')}:generatePackwiz"
        for entry in plan["validate"]
    ]
    build_unit_tasks.extend(generation_tasks)
    build_unit_tasks = list(dict.fromkeys(build_unit_tasks))
    tasks = list(build_unit_tasks)
    for name in ("gametest", "client"):
        tasks.extend(entry["task"] for entry in plan[name])
    tasks = list(dict.fromkeys(tasks))
    output = Path(os.environ["GITHUB_OUTPUT"])
    with output.open("a", encoding="utf-8") as stream:
        for name in ("build", "unit", "gametest", "client", "validate"):
            value = json.dumps(plan[name], separators=(",", ":"))
            stream.write(f"{name}={value}\n")
        stream.write(
            f"build-unit-tasks={json.dumps(build_unit_tasks, separators=(',', ':'))}\n"
        )
        stream.write(f"tasks={json.dumps(tasks, separators=(',', ':'))}\n")
        stream.write(f"has-work={str(has_work).lower()}\n")
        stream.write(f"wayland={str(bool(plan['client'])).lower()}\n")


if __name__ == "__main__":
    main()
