import io
import sys
from pathlib import Path
from types import SimpleNamespace

import bertie_ci.cli as cli
import pytest
from bertie_ci.instance import Instance
from bertie_ci.pack import PackMod


def test_empty_adapter_path_means_omitted_optional_input() -> None:
    assert cli._optional_path("") is None
    assert cli._optional_path("build/client-tests.jar") == Path(
        "build/client-tests.jar"
    )


@pytest.mark.parametrize("value", ["0", "-1"])
def test_runtime_timeout_must_be_positive(value: str) -> None:
    with pytest.raises(SystemExit):
        cli._parser().parse_args(
            ["client-test", "--instance", "instance.json", "--timeout", value]
        )


def test_client_test_defaults_to_world_join(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    args = cli._parser().parse_args(
        [
            "client-test",
            "--instance",
            "instance.json",
            "--test-mod",
            "",
            "--require-log",
            "",
        ]
    )
    context = object()
    observed: dict[str, object] = {}
    monkeypatch.setattr(cli, "_runtime_context", lambda *_: context)

    def capture_test(*test_args: object, **test_kwargs: object) -> None:
        observed["args"] = test_args
        observed["kwargs"] = test_kwargs

    monkeypatch.setattr(cli, "run_client_test", capture_test)

    cli._run_test(args, "client")

    assert observed["args"] == (context, 1500, "4G")
    assert observed["kwargs"] == {
        "minimum_game_tests": 0,
        "test_mods": (),
        "required_log_markers": (),
    }


def test_server_test_composes_optional_extensions(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    command_test = tmp_path / "server.json"
    test_mod = tmp_path / "server-tests.jar"
    args = cli._parser().parse_args(
        [
            "server-test",
            "--instance",
            "instance.json",
            "--command-test",
            str(command_test),
            "--test-mod",
            str(test_mod),
            "--require-log",
            "SERVER_ASSERTIONS_OK",
        ]
    )
    context = object()
    observed: dict[str, object] = {}
    monkeypatch.setattr(cli, "_runtime_context", lambda *_: context)

    def capture_test(*test_args: object, **test_kwargs: object) -> None:
        observed["args"] = test_args
        observed["kwargs"] = test_kwargs

    monkeypatch.setattr(cli, "run_server_test", capture_test)

    cli._run_test(args, "server")

    assert observed["args"] == (context, 900, "3G")
    assert observed["kwargs"] == {
        "command_test": command_test,
        "test_mods": (test_mod,),
        "required_log_markers": ("SERVER_ASSERTIONS_OK",),
    }


def test_runtime_context_rejects_wrong_side_before_loading_tools(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    descriptor = tmp_path / "instance.json"
    descriptor.touch()
    game_dir = tmp_path / "instance"
    game_dir.mkdir()
    instance = Instance("server", game_dir, "1.21.1", "neoforge", "21.1.233")
    loaded: list[str] = []
    monkeypatch.setattr(cli, "load_instance", lambda _path: instance)
    monkeypatch.setattr(
        cli, "load_client_runtime_tools", lambda: loaded.append("client")
    )
    monkeypatch.setattr(
        cli, "load_server_runtime_tools", lambda: loaded.append("server")
    )

    with pytest.raises(RuntimeError, match="client test cannot consume a server"):
        cli._runtime_context(descriptor, None, None, "client")

    assert loaded == []


def test_workspace_overlay_skips_mods_not_installed_on_instance_side(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    descriptor = tmp_path / "instance.json"
    descriptor.touch()
    server_dir = tmp_path / "server"
    server_dir.mkdir()
    common = SimpleNamespace(subject="common", pack_metafile=Path("common.pw.toml"))
    client = SimpleNamespace(subject="client", pack_metafile=Path("client.pw.toml"))
    pack = SimpleNamespace(kind="pack", path=tmp_path / "pack")
    workspace = SimpleNamespace(
        root=tmp_path,
        component=lambda subject: pack,
    )
    args = SimpleNamespace(
        artifact_dir=Path("artifacts"),
        instance=descriptor,
        pack_component="pack",
    )
    installed: list[tuple[str, str]] = []
    monkeypatch.setattr(
        cli, "_selected_mods", lambda _args: (workspace, (client, common))
    )
    monkeypatch.setattr(
        cli,
        "load_instance",
        lambda _path: Instance("server", server_dir, "1.21.1", "neoforge", "21.1.233"),
    )
    monkeypatch.setattr(
        cli,
        "_pack_mod",
        lambda _pack, mod: PackMod(
            f"{mod.subject}-old.jar", "client" if mod is client else "both"
        ),
    )
    monkeypatch.setattr(
        cli,
        "install_mod",
        lambda _descriptor, artifact, *, replace_filename: installed.append(
            (artifact.name, replace_filename)
        ),
    )

    cli._run_overlay_components(args)

    assert installed == [("common", "common-old.jar")]


def test_gradle_check_combines_tasks_and_stages_outputs(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    first = SimpleNamespace(
        subject="first",
        kind="neoforge-mod",
        gradle_project=":mods:first",
        path=tmp_path / "mods" / "first",
    )
    second = SimpleNamespace(
        subject="second",
        kind="neoforge-mod",
        gradle_project=":mods:second",
        path=tmp_path / "mods" / "second",
    )
    for component in (first, second):
        libraries = component.path / "build" / "libs"
        libraries.mkdir(parents=True)
        (libraries / f"{component.subject}.jar").write_bytes(b"production")
    test_libraries = second.path / "build" / "test-libs"
    test_libraries.mkdir()
    (test_libraries / "second-client-tests.jar").write_bytes(b"test")
    components = {component.subject: component for component in (first, second)}
    workspace = SimpleNamespace(
        root=tmp_path,
        component=lambda subject: components[subject],
    )
    observed: dict[str, object] = {}
    monkeypatch.setattr(cli.Workspace, "find", lambda _path: workspace)
    monkeypatch.setattr(cli, "load_java", lambda: tmp_path / "jdk" / "bin" / "java")
    monkeypatch.setattr(
        cli,
        "run_gradle",
        lambda project, java, tasks: observed.update(
            {"project": project, "java": java, "tasks": tasks}
        ),
    )
    args = cli._parser().parse_args(
        [
            "gradle-check",
            "--workspace",
            str(tmp_path),
            "--build-component",
            "second",
            "--build-component",
            "first",
            "--unit-component",
            "first",
            "--client-test-component",
            "second",
        ]
    )

    cli._run_gradle_check(args)

    assert observed == {
        "project": tmp_path,
        "java": tmp_path / "jdk",
        "tasks": (
            ":mods:first:assemble",
            ":mods:second:assemble",
            ":mods:first:test",
            ":mods:second:clientTestJar",
        ),
    }
    assert (tmp_path / ".bertie-ci" / "artifacts" / "first" / "first.jar").is_file()
    assert (
        tmp_path / ".bertie-ci" / "client-test" / "second" / "client-test-mod.jar"
    ).is_file()


def test_streams_survive_characters_outside_the_ansi_code_page(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """A redirected stdout on Windows encodes as cp1252, and Minecraft logs do not."""
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
