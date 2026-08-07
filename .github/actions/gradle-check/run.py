#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import subprocess
import sys


def array(name: str) -> list[object]:
    value = json.loads(os.environ[name])
    if not isinstance(value, list):
        raise SystemExit(f"{name} must be a JSON array")
    return value


def main() -> None:
    selected = array("BERTIE_GRADLE_TASKS")
    if not selected or not all(isinstance(task, str) and task for task in selected):
        raise SystemExit("BERTIE_GRADLE_TASKS must contain non-empty task paths")
    validations = array("BERTIE_PACK_VALIDATIONS")
    if not all(
        isinstance(entry, dict)
        and isinstance(entry.get("subject"), str)
        and entry["subject"]
        for entry in validations
    ):
        raise SystemExit("Every pack validation must contain a subject")
    dependency_check = subprocess.run(
        ["bertie-ci", "deps-check", "--workspace", "."],
        check=False,
    )
    if dependency_check.returncode != 0:
        sys.exit(dependency_check.returncode)
    command = [
        "bertie-ci",
        "gradle-task",
        "--workspace",
        ".",
        "--work-dir",
        os.environ["BERTIE_GRADLE_WORK_DIR"],
        "--timeout",
        os.environ["BERTIE_GRADLE_TIMEOUT"],
        "--continue",
    ]
    if os.environ.get("BERTIE_CHECK_WAYLAND", "false").lower() == "true":
        command.append("--wayland")
    for task in selected:
        command.extend(("--task", str(task)))
    result = subprocess.run(command, check=False)

    validation_failed = False
    for entry in validations:
        validation = subprocess.run(
            [
                "bertie-ci",
                "pack-validate",
                "--workspace",
                ".",
                "--component",
                str(entry["subject"]),
                "--generated",
            ],
            check=False,
        )
        validation_failed = validation.returncode != 0 or validation_failed

    if result.returncode != 0 or validation_failed:
        sys.exit(1)


if __name__ == "__main__":
    main()
