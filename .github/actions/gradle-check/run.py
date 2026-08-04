#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import subprocess


def tasks(name: str) -> list[str]:
    value = json.loads(os.environ[name])
    if not isinstance(value, list) or not all(isinstance(item, dict) for item in value):
        raise SystemExit(f"{name} must be a JSON array of objects")
    selected = [item.get("task") for item in value]
    if not all(isinstance(task, str) and task for task in selected):
        raise SystemExit(f"Every {name} entry must contain a non-empty task")
    return selected


def main() -> None:
    selected = list(
        dict.fromkeys(tasks("BERTIE_BUILD_PLAN") + tasks("BERTIE_UNIT_PLAN"))
    )
    if not selected:
        raise SystemExit("The combined Gradle plan is empty")
    command = [
        "bertie-ci",
        "gradle-task",
        "--workspace",
        ".",
        "--work-dir",
        ".bertie-ci/gradle",
        "--timeout",
        "2700",
    ]
    for task in selected:
        command.extend(("--task", task))
    subprocess.run(command, check=True)


if __name__ == "__main__":
    main()
