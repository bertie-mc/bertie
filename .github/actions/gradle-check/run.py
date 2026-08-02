#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import subprocess


def entries(name: str) -> list[dict[str, object]]:
    value = json.loads(os.environ[name])
    if not isinstance(value, list) or not all(isinstance(item, dict) for item in value):
        raise SystemExit(f"{name} must be a JSON array of objects")
    return value


def subjects(items: list[dict[str, object]]) -> list[str]:
    values = [item.get("subject") for item in items]
    if not all(isinstance(value, str) and value for value in values):
        raise SystemExit("Every plan entry must have a non-empty subject")
    return list(dict.fromkeys(values))


def main() -> None:
    command = [
        "bertie-ci",
        "gradle-check",
        "--workspace",
        ".",
        "--artifact-dir",
        ".bertie-ci/gradle-output/artifacts",
        "--client-test-dir",
        ".bertie-ci/gradle-output/client-test",
    ]
    for subject in subjects(entries("BERTIE_BUILD_PLAN")):
        command.extend(("--build-component", subject))
    for subject in subjects(entries("BERTIE_UNIT_PLAN")):
        command.extend(("--unit-component", subject))
    client_tests = [
        item
        for item in entries("BERTIE_CLIENT_PLAN")
        if item.get("build_client_test_mod") is True
    ]
    for subject in subjects(client_tests):
        command.extend(("--client-test-component", subject))
    subprocess.run(command, check=True)


if __name__ == "__main__":
    main()
