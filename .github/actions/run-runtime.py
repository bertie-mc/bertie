#!/usr/bin/env python3
"""Translate a GitHub action's JSON-list inputs into repeatable CLI arguments."""

from __future__ import annotations

import json
import os
import subprocess
import sys


def required_logs() -> list[str]:
    value = json.loads(os.environ.get("BERTIE_REQUIRE_LOG", "[]"))
    if not isinstance(value, list) or not all(
        isinstance(marker, str) and marker for marker in value
    ):
        raise SystemExit("require-log must be a JSON array of non-empty strings")
    return value


def main() -> None:
    side = sys.argv[1]
    if side not in ("client", "server"):
        raise SystemExit(f"unsupported runtime side: {side}")
    command = [
        "bertie-ci",
        f"{side}-test",
        "--instance",
        os.environ["BERTIE_INSTANCE"],
        "--work-dir",
        os.environ["BERTIE_WORK_DIR"],
        "--cache-dir",
        os.environ["BERTIE_CACHE_DIR"],
        "--timeout",
        os.environ["BERTIE_TIMEOUT"],
        "--max-memory",
        os.environ["BERTIE_MAX_MEMORY"],
    ]
    test_mod = os.environ.get("BERTIE_TEST_MOD")
    if test_mod:
        command.extend(("--test-mod", test_mod))
    for marker in required_logs():
        command.extend(("--require-log", marker))
    if side == "client":
        command.extend(
            (
                "--minimum-game-tests",
                os.environ["BERTIE_MINIMUM_GAME_TESTS"],
            )
        )
    else:
        command.extend(("--command-test", os.environ["BERTIE_COMMAND_TEST"]))
    subprocess.run(command, check=True)


if __name__ == "__main__":
    main()
