#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path


def main() -> None:
    repository = Path(__file__).resolve().parents[3]
    source = repository / "tools" / "bertie-ci" / "src"
    environment = os.environ.copy()
    environment["PYTHONPATH"] = os.pathsep.join(
        filter(None, (str(source), environment.get("PYTHONPATH", "")))
    )
    result = subprocess.run(
        [
            sys.executable,
            "-m",
            "bertie_ci.cli",
            "release-plan",
            "--workspace",
            ".",
            "--tag",
            os.environ["BERTIE_RELEASE_TAG"],
        ],
        check=True,
        capture_output=True,
        env=environment,
        text=True,
    )
    plan = json.loads(result.stdout)
    output = Path(os.environ["GITHUB_OUTPUT"])
    with output.open("a", encoding="utf-8") as stream:
        for name in ("subject", "version", "kind", "project"):
            stream.write(f"{name}={plan[name]}\n")


if __name__ == "__main__":
    main()
