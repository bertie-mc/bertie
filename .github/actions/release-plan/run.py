#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import subprocess
from pathlib import Path


def main() -> None:
    result = subprocess.run(
        [
            "bertie-ci",
            "release-plan",
            "--workspace",
            ".",
            "--tag",
            os.environ["BERTIE_RELEASE_TAG"],
        ],
        check=True,
        capture_output=True,
        text=True,
    )
    plan = json.loads(result.stdout)
    output = Path(os.environ["GITHUB_OUTPUT"])
    with output.open("a", encoding="utf-8") as stream:
        for name in ("subject", "version", "kind", "project"):
            stream.write(f"{name}={plan[name]}\n")


if __name__ == "__main__":
    main()
