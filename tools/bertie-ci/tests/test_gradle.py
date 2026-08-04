from pathlib import Path

import pytest
from bertie_ci.artifact import find_artifact, stage_artifact
from bertie_ci.gradle import run_gradle, task_path


def test_run_gradle_uses_managed_tools_and_passes_environment(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    invocation: dict[str, object] = {}

    def fake_run(command: object, **kwargs: object) -> None:
        invocation["command"] = command
        invocation.update(kwargs)

    gradle = tmp_path / "nix-store" / "bin" / "gradle"
    java_home = tmp_path / "jdk"
    log = tmp_path / "gradle.log"
    monkeypatch.setenv("BERTIE_CI_GRADLE", str(gradle))
    monkeypatch.setenv("DISPLAY", ":99")
    monkeypatch.setattr("bertie_ci.gradle.run", fake_run)

    run_gradle(
        tmp_path,
        java_home,
        [":mods:example:test"],
        log=log,
        timeout_seconds=42,
        environment={"WAYLAND_DISPLAY": "wayland-bertie"},
    )

    assert invocation["command"] == [
        str(gradle),
        ":mods:example:test",
        "--no-daemon",
        "--stacktrace",
    ]
    assert invocation["cwd"] == tmp_path
    assert invocation["log"] == log
    assert invocation["timeout_seconds"] == 42
    assert invocation["env"]["JAVA_HOME"] == str(java_home)  # type: ignore[index]
    assert invocation["env"]["WAYLAND_DISPLAY"] == "wayland-bertie"  # type: ignore[index]
    assert "DISPLAY" not in invocation["env"]  # type: ignore[operator]


def test_task_path_supports_root_and_nested_projects() -> None:
    assert task_path(None, "test") == "test"
    assert task_path(":", "test") == ":test"
    assert task_path(":mods:example", "test") == ":mods:example:test"


def test_find_artifact_ignores_documentation_jars(tmp_path: Path) -> None:
    libraries = tmp_path / "build" / "libs"
    libraries.mkdir(parents=True)
    runtime = libraries / "example-1.0.0.jar"
    runtime.touch()
    (libraries / "example-1.0.0-sources.jar").touch()
    (libraries / "example-1.0.0-javadoc.jar").touch()

    assert find_artifact(tmp_path) == runtime


def test_stage_artifact_preserves_release_filename(tmp_path: Path) -> None:
    runtime = tmp_path / "build" / "libs" / "example-1.0.0.jar"
    runtime.parent.mkdir(parents=True)
    runtime.write_bytes(b"jar")

    staged = stage_artifact(runtime, tmp_path / ".bertie-ci" / "artifact")

    assert staged.name == runtime.name
    assert staged.read_bytes() == b"jar"


def test_stage_artifact_rejects_ambiguous_output(tmp_path: Path) -> None:
    runtime = tmp_path / "build" / "libs" / "example-1.0.0.jar"
    runtime.parent.mkdir(parents=True)
    runtime.touch()
    output = tmp_path / ".bertie-ci" / "artifact"
    output.mkdir(parents=True)
    (output / "old-version.jar").touch()

    with pytest.raises(RuntimeError, match="contains other JARs"):
        stage_artifact(runtime, output)
