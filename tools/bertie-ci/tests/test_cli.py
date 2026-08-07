import io
import os
import signal
import sys
from contextlib import contextmanager
from pathlib import Path
from types import SimpleNamespace

import bertie_ci.cli as cli
import pytest
from bertie_ci.deps_audit import DependencyAudit
from bertie_ci.pack import PackSummary


@pytest.mark.parametrize("value", ["0", "-1"])
def test_gradle_timeout_must_be_positive(value: str) -> None:
    with pytest.raises(SystemExit):
        cli._parser().parse_args(["gradle-task", "--task", ":test", "--timeout", value])


def test_dependency_audit_prints_advisory_findings(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
    capsys: pytest.CaptureFixture[str],
) -> None:
    monkeypatch.setattr(
        cli.Workspace, "find", lambda _path: SimpleNamespace(root=tmp_path)
    )
    monkeypatch.setattr(
        cli,
        "audit_modrinth",
        lambda _root: DependencyAudit(2, 1, ("example finding",)),
    )
    args = cli._parser().parse_args(["deps-audit", "--workspace", str(tmp_path)])

    cli._run_deps_audit(args)

    assert capsys.readouterr().out == (
        "Audited 2 Modrinth distributions across 1 projects.\n"
        "Advisory findings:\n"
        "- example finding\n"
    )


def test_gradle_task_runs_on_the_current_desktop_by_default(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    workspace = SimpleNamespace(root=tmp_path)
    observed: dict[str, object] = {}
    monkeypatch.setattr(cli.Workspace, "find", lambda _path: workspace)
    monkeypatch.setattr(cli, "load_java", lambda: tmp_path / "jdk" / "bin" / "java")
    monkeypatch.setattr(
        cli,
        "run_gradle",
        lambda *args, **kwargs: observed.update({"args": args, "kwargs": kwargs}),
    )
    args = cli._parser().parse_args(
        [
            "gradle-task",
            "--workspace",
            str(tmp_path),
            "--task",
            ":mods:example:test",
            "--task",
            ":mods:example:test",
            "--work-dir",
            "reports",
            "--timeout",
            "42",
        ]
    )

    cli._run_gradle_task(args)

    assert observed["args"] == (
        tmp_path,
        tmp_path / "jdk",
        (":mods:example:test",),
    )
    assert observed["kwargs"] == {
        "log": tmp_path / "reports" / "gradle.log",
        "timeout_seconds": 42,
        "environment": None,
        "continue_after_failure": False,
    }


def test_gradle_task_can_opt_into_an_isolated_wayland_session(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    workspace = SimpleNamespace(root=tmp_path)
    observed: dict[str, object] = {}
    tools = object()
    monkeypatch.setattr(cli.Workspace, "find", lambda _path: workspace)
    monkeypatch.setattr(cli, "load_java", lambda: tmp_path / "jdk" / "bin" / "java")
    monkeypatch.setattr(cli, "load_wayland_tools", lambda: tools)

    @contextmanager
    def session(selected: object, log: Path):
        observed["session"] = (selected, log)
        yield {"WAYLAND_DISPLAY": "wayland-bertie"}

    monkeypatch.setattr(cli, "wayland_session", session)
    monkeypatch.setattr(
        cli,
        "run_gradle",
        lambda *args, **kwargs: observed.update({"args": args, "kwargs": kwargs}),
    )
    args = cli._parser().parse_args(
        [
            "gradle-task",
            "--workspace",
            str(tmp_path),
            "--task",
            ":runClientTests",
            "--wayland",
        ]
    )

    cli._run_gradle_task(args)

    work = tmp_path / ".bertie-ci" / "gradle"
    assert observed["session"] == (tools, work / "wayland.log")
    assert observed["kwargs"]["environment"] == {  # type: ignore[index]
        "WAYLAND_DISPLAY": "wayland-bertie"
    }


def test_pack_validation_generates_exact_component_task_before_reading_output(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    project = tmp_path / "pack"
    project.mkdir()
    component = SimpleNamespace(
        subject="pack", kind="pack", path=project, gradle_project=":pack"
    )
    workspace = SimpleNamespace(
        root=tmp_path,
        component=lambda subject: component if subject == "pack" else None,
    )
    events: list[object] = []
    monkeypatch.setattr(cli.Workspace, "find", lambda _path: workspace)
    monkeypatch.setattr(cli, "load_java", lambda: tmp_path / "jdk" / "bin" / "java")

    def generate(root: Path, java_home: Path, tasks: object) -> None:
        events.append(("gradle", root, java_home, tasks))
        (project / "build" / "packwiz").mkdir(parents=True)

    def validate(generated: Path, packwiz: Path) -> PackSummary:
        events.append(("validate", generated, packwiz))
        return PackSummary(1, 1, 0)

    monkeypatch.setattr(cli, "run_gradle", generate)
    monkeypatch.setattr(cli, "load_packwiz", lambda: Path("packwiz"))
    monkeypatch.setattr(cli, "validate_pack", validate)
    args = cli._parser().parse_args(
        ["pack-validate", "--workspace", str(tmp_path), "--component", "pack"]
    )

    cli._run_pack_validate(args)

    assert events == [
        ("gradle", tmp_path, tmp_path / "jdk", [":pack:generatePackwiz"]),
        ("validate", project / "build" / "packwiz", Path("packwiz")),
    ]


def test_pack_validation_can_use_existing_generated_output(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    project = tmp_path / "pack"
    generated = project / "build" / "packwiz"
    generated.mkdir(parents=True)
    component = SimpleNamespace(
        subject="pack", kind="pack", path=project, gradle_project=":pack"
    )
    workspace = SimpleNamespace(
        root=tmp_path,
        component=lambda subject: component if subject == "pack" else None,
    )
    observed: list[Path] = []
    monkeypatch.setattr(cli.Workspace, "find", lambda _path: workspace)
    monkeypatch.setattr(cli, "load_packwiz", lambda: Path("packwiz"))
    monkeypatch.setattr(
        cli,
        "validate_pack",
        lambda path, _packwiz: observed.append(path) or PackSummary(1, 0, 0),
    )
    monkeypatch.setattr(
        cli,
        "run_gradle",
        lambda *_args, **_kwargs: pytest.fail("Gradle should not run"),
    )
    args = cli._parser().parse_args(
        [
            "pack-validate",
            "--workspace",
            str(tmp_path),
            "--component",
            "pack",
            "--generated",
        ]
    )

    cli._run_pack_validate(args)

    assert observed == [generated]


@pytest.mark.parametrize(
    ("command", "runner_name", "loader_name", "suffix"),
    [
        (
            "pack-export-server",
            "_run_pack_export_server",
            "load_packwiz_installer",
            ".zip",
        ),
    ],
)
def test_pack_exports_generate_standalone_project_before_export(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
    command: str,
    runner_name: str,
    loader_name: str,
    suffix: str,
) -> None:
    generated = tmp_path / "build" / "packwiz"
    events: list[object] = []
    monkeypatch.setattr(cli, "load_java", lambda: tmp_path / "jdk" / "bin" / "java")

    def generate(root: Path, java_home: Path, tasks: object) -> None:
        events.append(("gradle", root, java_home, tasks))
        generated.mkdir(parents=True)

    def export(project: Path, output: Path, tool: Path, *extra: Path) -> Path:
        events.append(("export", project, output, tool, *extra))
        return output

    monkeypatch.setattr(cli, "run_gradle", generate)
    monkeypatch.setattr(cli, loader_name, lambda: Path("pack-tool"))
    monkeypatch.setattr(
        cli,
        "export_client_pack" if command.endswith("client") else "export_server_pack",
        export,
    )
    output = Path("release") / f"pack{suffix}"
    args = cli._parser().parse_args(
        [command, "--project", str(tmp_path), "--output", str(output)]
    )

    getattr(cli, runner_name)(args)

    expected_export = ("export", generated, tmp_path / output, Path("pack-tool"))
    if command.endswith("server"):
        expected_export += (tmp_path / "README.md",)
    assert events == [
        ("gradle", tmp_path, tmp_path / "jdk", ["generatePackwiz"]),
        expected_export,
    ]


def test_client_export_copies_direct_gradle_mrpack(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    generated = tmp_path / "build" / "distributions" / "bertie.mrpack"
    events: list[object] = []
    monkeypatch.setattr(cli, "load_java", lambda: tmp_path / "jdk" / "bin" / "java")

    def generate(root: Path, java_home: Path, tasks: object) -> None:
        events.append(("gradle", root, java_home, tasks))
        generated.parent.mkdir(parents=True)
        generated.write_bytes(b"mrpack")

    monkeypatch.setattr(cli, "run_gradle", generate)
    args = cli._parser().parse_args(
        [
            "pack-export-client",
            "--project",
            str(tmp_path),
            "--output",
            "release/pack.mrpack",
        ]
    )

    cli._run_pack_export_client(args)

    assert events == [
        ("gradle", tmp_path, tmp_path / "jdk", ["generateMrpack"]),
    ]
    assert (tmp_path / "release/pack.mrpack").read_bytes() == b"mrpack"


def test_curseforge_export_copies_direct_gradle_archive(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    generated = tmp_path / "build" / "distributions" / "bertie-curseforge.zip"
    events: list[object] = []
    monkeypatch.setattr(cli, "load_java", lambda: tmp_path / "jdk" / "bin" / "java")

    def generate(root: Path, java_home: Path, tasks: object) -> None:
        events.append(("gradle", root, java_home, tasks))
        generated.parent.mkdir(parents=True)
        generated.write_bytes(b"curseforge")

    monkeypatch.setattr(cli, "run_gradle", generate)
    args = cli._parser().parse_args(
        [
            "pack-export-curseforge",
            "--project",
            str(tmp_path),
            "--output",
            "release/pack.zip",
        ]
    )

    cli._run_pack_export_curseforge(args)

    assert events == [
        ("gradle", tmp_path, tmp_path / "jdk", ["generateCurseForgePack"]),
    ]
    assert (tmp_path / "release/pack.zip").read_bytes() == b"curseforge"


def test_server_export_uses_component_task_and_source_readme(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    project = tmp_path / "pack"
    project.mkdir()
    readme = project / "README.md"
    readme.write_text("# Pack\n", encoding="utf-8")
    component = SimpleNamespace(
        subject="pack", kind="pack", path=project, gradle_project=":pack"
    )
    workspace = SimpleNamespace(
        root=tmp_path,
        component=lambda subject: component if subject == "pack" else None,
    )
    events: list[object] = []
    monkeypatch.setattr(cli.Workspace, "find", lambda _path: workspace)
    monkeypatch.setattr(cli, "load_java", lambda: tmp_path / "jdk" / "bin" / "java")

    def generate(root: Path, java_home: Path, tasks: object) -> None:
        events.append(("gradle", root, java_home, tasks))
        (project / "build" / "packwiz").mkdir(parents=True)

    def export(
        generated: Path, output: Path, installer: Path, selected_readme: Path
    ) -> Path:
        events.append(("export", generated, output, installer, selected_readme))
        return output

    monkeypatch.setattr(cli, "run_gradle", generate)
    monkeypatch.setattr(cli, "load_packwiz_installer", lambda: Path("installer.jar"))
    monkeypatch.setattr(cli, "export_server_pack", export)
    args = cli._parser().parse_args(
        [
            "pack-export-server",
            "--workspace",
            str(tmp_path),
            "--component",
            "pack",
            "--output",
            "release/pack.zip",
        ]
    )

    cli._run_pack_export_server(args)

    assert events == [
        ("gradle", tmp_path, tmp_path / "jdk", [":pack:generatePackwiz"]),
        (
            "export",
            project / "build" / "packwiz",
            tmp_path / "release" / "pack.zip",
            Path("installer.jar"),
            readme,
        ),
    ]


def test_streams_survive_characters_outside_the_console_code_page(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    buffer = io.BytesIO()
    stream = io.TextIOWrapper(buffer, encoding="cp1252")
    monkeypatch.setattr(sys, "stdout", stream)
    monkeypatch.setattr(sys, "stderr", stream)

    with pytest.raises(UnicodeEncodeError):
        stream.write("█ progress\n")
        stream.flush()

    cli.tolerate_unencodable_output()
    stream.write("█ progress\n")
    stream.flush()

    assert b"? progress" in buffer.getvalue()


@pytest.mark.skipif(os.name != "posix", reason="POSIX signal behavior")
def test_main_maps_sigterm_to_conventional_exit_code(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setattr(sys, "argv", ["bertie-ci", "plan"])
    monkeypatch.setattr(
        cli,
        "_run_plan",
        lambda _args: os.kill(os.getpid(), signal.SIGTERM),
    )

    with pytest.raises(SystemExit) as error:
        cli.main()

    assert error.value.code == 128 + signal.SIGTERM
