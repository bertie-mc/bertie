from __future__ import annotations

import os
from collections.abc import Mapping, Sequence
from pathlib import Path

from .process import run


def _enabled(environment: Mapping[str, str], name: str) -> bool:
    return environment.get(name, "").lower() in {"1", "true", "yes"}


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
    selected_environment = (
        dict(environment) if environment is not None else dict(os.environ)
    )
    command: list[str | Path] = [
        os.environ.get("BERTIE_CI_GRADLE", "gradle"),
        *tasks,
        "--no-daemon",
        "--stacktrace",
    ]
    if _enabled(selected_environment, "BERTIE_CI_GRADLE_OFFLINE"):
        command.append("--offline")
    selected_environment["JAVA_HOME"] = os.fspath(java_home)
    run(
        command,
        cwd=project,
        env=selected_environment,
        log=log,
        timeout_seconds=timeout_seconds,
    )
