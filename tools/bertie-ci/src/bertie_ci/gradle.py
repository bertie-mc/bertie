from __future__ import annotations

import os
from collections.abc import Mapping, Sequence
from pathlib import Path

from .process import run


def task_path(gradle_project: str | None, task: str) -> str:
    if gradle_project is None:
        return task
    return f"{gradle_project.rstrip(':')}:{task}"


def run_gradle(
    project: Path,
    java_home: Path,
    tasks: Sequence[str],
    *,
    log: Path | None = None,
    timeout_seconds: int | None = None,
    environment: Mapping[str, str] | None = None,
) -> None:
    command: list[str | Path] = [
        os.environ.get("BERTIE_CI_GRADLE", "gradle"),
        *tasks,
        "--no-daemon",
        "--stacktrace",
    ]
    selected_environment = (
        dict(environment) if environment is not None else dict(os.environ)
    )
    selected_environment["JAVA_HOME"] = os.fspath(java_home)
    run(
        command,
        cwd=project,
        env=selected_environment,
        log=log,
        timeout_seconds=timeout_seconds,
    )
