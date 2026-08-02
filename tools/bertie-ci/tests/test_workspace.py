from __future__ import annotations

import json
import subprocess
from pathlib import Path

import pytest
from bertie_ci.workspace import Workspace, plan_json


def _write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def _git(root: Path, *arguments: str) -> str:
    return subprocess.run(
        ["git", *arguments],
        cwd=root,
        check=True,
        stdout=subprocess.PIPE,
        text=True,
    ).stdout.strip()


def _commit_all(root: Path, message: str) -> str:
    _git(root, "add", ".")
    _git(
        root,
        "-c",
        "user.name=Bertie CI",
        "-c",
        "user.email=ci@bertie.invalid",
        "commit",
        "-qm",
        message,
    )
    return _git(root, "rev-parse", "HEAD")


def _workspace(root: Path) -> Workspace:
    _write(
        root / "bertie-ci.toml",
        """format = "bertie-ci.workspace.v1"

[workspace]
component-descriptors = ["mods/*/bertie-ci.toml", "pack/bertie-ci.toml"]
shared-mod-paths = ["build-logic/", "settings.gradle.kts"]
shared-all-paths = ["flake.nix", ".github/actions/", "tools/bertie-ci/"]
ignored-paths = [".github/dependabot.yml", "README.md", "docs/**"]
""",
    )
    _write(
        root / "mods" / "base" / "bertie-ci.toml",
        """format = "bertie-ci.component.v1"
subject = "base"
kind = "neoforge-mod"
gradle-project = ":mods:base"
mod-id = "base"
pack-metafile = "mods/base.pw.toml"

[version]
file = "mod.properties"
key = "mod_version"

[[suite]]
id = "unit"
runner = "unit"

[[suite]]
id = "client-contract"
runner = "client"
fixtures = ["example", "library"]
instance-files = "src/clientTest/instance"
build-client-test-mod = true
require-log = ["BASE_CLIENT_OK", "BASE_SECOND_ASSERTION_OK"]
timeout = 42
max-memory = "5g"
""",
    )
    _write(root / "mods" / "base" / "mod.properties", "mod_version=1.2.3\n")
    _write(
        root / "mods" / "consumer" / "bertie-ci.toml",
        """format = "bertie-ci.component.v1"
subject = "consumer"
kind = "neoforge-mod"
gradle-project = ":mods:consumer"
mod-id = "consumer"
pack-metafile = "mods/consumer.pw.toml"
depends-on = ["base"]

[version]
file = "mod.properties"
key = "mod_version"

[[suite]]
id = "world-behavior"
runner = "gametest"
""",
    )
    _write(root / "mods" / "consumer" / "mod.properties", "mod_version=2.0.0\n")
    _write(
        root / "pack" / "bertie-ci.toml",
        """format = "bertie-ci.component.v1"
subject = "pack"
kind = "pack"

[version]
file = "pack.toml"
key = "version"

[[suite]]
id = "manifest"
runner = "validate"

[[suite]]
id = "world-join"
runner = "client"
automatic = false
timeout = 4800
max-memory = "10G"
""",
    )
    _write(root / "pack" / "pack.toml", 'version = "0.1.0"\n')
    return Workspace.load(root)


def test_discovers_components_and_versions(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    assert set(workspace.components) == {"base", "consumer", "pack"}
    assert workspace.component("base").version() == "1.2.3"
    assert Workspace.find(tmp_path / "mods" / "base").root == tmp_path


def test_owned_mod_id_matches_directory_without_hyphens(tmp_path: Path) -> None:
    _workspace(tmp_path)
    descriptor = tmp_path / "mods" / "base" / "bertie-ci.toml"
    descriptor.write_text(
        descriptor.read_text(encoding="utf-8").replace(
            'mod-id = "base"', 'mod-id = "base_mod"'
        ),
        encoding="utf-8",
    )

    with pytest.raises(RuntimeError, match="directory name without hyphens"):
        Workspace.load(tmp_path)


def test_affected_component_includes_reverse_dependents(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    affected = workspace.affected(("mods/base/src/main/java/Example.java",))

    assert affected == {"base", "consumer"}


def test_shared_and_ignored_paths_have_explicit_scope(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    assert workspace.affected(("build-logic/src/Plugin.kt",)) == {"base", "consumer"}
    assert workspace.affected(("tools/bertie-ci/src/runner.py",)) == {
        "base",
        "consumer",
        "pack",
    }
    assert workspace.affected((".github/actions/build-mod/action.yml",)) == {
        "base",
        "consumer",
        "pack",
    }
    assert workspace.affected((".github/dependabot.yml",)) == set()
    assert workspace.affected(("README.md", "docs/design.md")) == set()


def test_plan_excludes_manual_pack_runtime_by_default(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    automatic = workspace.plan({"pack"})
    complete = workspace.plan({"pack"}, include_manual=True)

    assert automatic["validate"][0]["subject"] == "pack"
    assert automatic["client"] == []
    assert complete["client"] == [
        {
            "subject": "pack",
            "project": "pack",
            "suite": "world-join",
            "fixture": "",
            "build_client_test_mod": False,
            "require_log": [],
            "minimum_game_tests": 0,
            "timeout": 4800,
            "max_memory": "10G",
        }
    ]


def test_client_matrix_contains_project_owned_configuration(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    plan = json.loads(plan_json(workspace.plan({"base"})))

    assert plan["client"] == [
        {
            "build_client_test_mod": True,
            "fixture": "example,library",
            "gradle_project": ":mods:base",
            "instance_files": "src/clientTest/instance",
            "max_memory": "5G",
            "minimum_game_tests": 0,
            "project": "mods/base",
            "require_log": ["BASE_CLIENT_OK", "BASE_SECOND_ASSERTION_OK"],
            "suite": "client-contract",
            "subject": "base",
            "timeout": 42,
        }
    ]


def test_release_plan_requires_exact_subject_version(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    plan = workspace.release_plan("base/v1.2.3")

    assert plan.subject == "base"
    assert plan.version == "1.2.3"
    assert json.loads(plan.to_json())["project"] == "mods/base"

    with pytest.raises(RuntimeError, match="exact form"):
        workspace.release_plan("v1.2.3")
    with pytest.raises(RuntimeError, match="does not match"):
        workspace.release_plan("base/v1.2.4")


def test_unknown_product_path_is_conservatively_global(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    assert workspace.affected(("new-shared-file.toml",)) == {
        "base",
        "consumer",
        "pack",
    }


def test_missing_base_revision_is_conservatively_global(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)
    _git(tmp_path, "init", "-q")
    _commit_all(tmp_path, "test fixture")

    changed = workspace.changed_files("f" * 40, "HEAD")

    assert workspace.affected(changed) == {"base", "consumer", "pack"}


def test_missing_head_revision_still_fails(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)
    _git(tmp_path, "init", "-q")

    with pytest.raises(RuntimeError, match="Head Git revision .* is unavailable"):
        workspace.changed_files("f" * 40, "HEAD")


def test_available_backward_base_uses_endpoint_diff(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)
    _git(tmp_path, "init", "-q")
    parent = _commit_all(tmp_path, "base")
    _write(tmp_path / "removed-on-rollback.txt", "content\n")
    child = _commit_all(tmp_path, "child")

    changed = workspace.changed_files(child, parent)

    assert changed == ("removed-on-rollback.txt",)


def test_component_descriptor_is_an_implicit_standalone_workspace(
    tmp_path: Path,
) -> None:
    _write(
        tmp_path / "bertie-ci.toml",
        """format = "bertie-ci.component.v1"
subject = "standalone"
kind = "neoforge-mod"
gradle-project = ":"
mod-id = "standalone"

[version]
file = "mod.properties"
key = "mod_version"

[[suite]]
id = "unit"
runner = "unit"
""",
    )
    _write(tmp_path / "mod.properties", "mod_version=1.0.0\n")

    workspace = Workspace.find(tmp_path)

    component = workspace.component("standalone")
    assert component.path == tmp_path
    assert component.relative_path == Path(".")
    assert workspace.plan({"standalone"})["unit"][0]["suite"] == "unit"


def test_rejects_suite_runner_incompatible_with_component_kind(tmp_path: Path) -> None:
    _workspace(tmp_path)
    with (tmp_path / "pack" / "bertie-ci.toml").open("a", encoding="utf-8") as stream:
        stream.write('\n[[suite]]\nid = "unit"\nrunner = "unit"\n')

    with pytest.raises(RuntimeError, match="cannot run suite"):
        Workspace.load(tmp_path)


def test_rejects_client_test_artifact_for_server_suite(tmp_path: Path) -> None:
    _workspace(tmp_path)
    with (tmp_path / "mods" / "base" / "bertie-ci.toml").open(
        "a", encoding="utf-8"
    ) as stream:
        stream.write(
            '\n[[suite]]\nid = "server"\nrunner = "server"\n'
            "build-client-test-mod = true\n"
        )

    with pytest.raises(RuntimeError, match="only for a client runner"):
        Workspace.load(tmp_path)


def test_rejects_unknown_suite_key(tmp_path: Path) -> None:
    _workspace(tmp_path)
    descriptor = tmp_path / "pack" / "bertie-ci.toml"
    descriptor.write_text(
        descriptor.read_text(encoding="utf-8") + "automtic = false\n",
        encoding="utf-8",
    )

    with pytest.raises(RuntimeError, match=r"Unknown suite key\(s\): automtic"):
        Workspace.load(tmp_path)


def test_rejects_multiple_suites_for_a_single_task_runner(tmp_path: Path) -> None:
    _workspace(tmp_path)
    with (tmp_path / "mods" / "base" / "bertie-ci.toml").open(
        "a", encoding="utf-8"
    ) as stream:
        stream.write('\n[[suite]]\nid = "unit-again"\nrunner = "unit"\n')

    with pytest.raises(RuntimeError, match="multiple 'unit' suites"):
        Workspace.load(tmp_path)
