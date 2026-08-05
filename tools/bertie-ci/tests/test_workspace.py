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
        """format = "bertie-ci.workspace.v2"

[workspace]
component-descriptors = ["mods/*/bertie-ci.toml", "pack/bertie-ci.toml"]
shared-gradle-paths = ["build-logic/", "settings.gradle.kts", "core/"]
shared-all-paths = ["flake.nix", ".github/actions/", "tools/bertie-ci/"]
ignored-paths = [".github/dependabot.yml", "README.md", "docs/**"]
""",
    )
    _write(
        root / "mods" / "base" / "bertie-ci.toml",
        """format = "bertie-ci.component.v2"
subject = "base"
kind = "neoforge-mod"
gradle-project = ":mods:base"

[version]
file = "mod.properties"
key = "mod_version"
""",
    )
    _write(root / "mods" / "base" / "mod.properties", "mod_version=1.2.3\n")
    _write(root / "mods" / "base" / "src" / "test" / "Unit.java", "class Unit {}\n")
    _write(
        root / "mods" / "base" / "src" / "clienttest" / "Client.java",
        "class Client {}\n",
    )
    _write(
        root / "mods" / "consumer" / "bertie-ci.toml",
        """format = "bertie-ci.component.v2"
subject = "consumer"
kind = "neoforge-mod"
gradle-project = ":mods:consumer"
depends-on = ["base"]

[version]
file = "mod.properties"
key = "mod_version"
""",
    )
    _write(root / "mods" / "consumer" / "mod.properties", "mod_version=2.0.0\n")
    _write(
        root / "mods" / "consumer" / "src" / "gametest" / "Game.java",
        "class Game {}\n",
    )
    _write(
        root / "pack" / "bertie-ci.toml",
        """format = "bertie-ci.component.v2"
subject = "pack"
kind = "pack"
gradle-project = ":pack"

[version]
file = "pack.properties"
key = "version"
""",
    )
    _write(root / "pack" / "pack.properties", "version=0.1.0\n")
    for source_set in ("test", "gametest", "clienttest"):
        _write(
            root / "pack" / "src" / source_set / "java" / "PackTest.java",
            "class PackTest {}\n",
        )
    return Workspace.load(root)


def test_discovers_components_and_versions(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    assert set(workspace.components) == {"base", "consumer", "pack"}
    assert workspace.component("base").version() == "1.2.3"
    assert workspace.component("pack").version() == "0.1.0"
    assert Workspace.find(tmp_path / "mods" / "base").root == tmp_path


@pytest.mark.parametrize(
    ("replacement", "message"),
    [
        ("", "version.file must be a non-empty string"),
        (
            'workspace-file = "gradle/libs.versions.toml"',
            r"Unknown version key\(s\): workspace-file",
        ),
    ],
)
def test_version_source_requires_a_component_file(
    tmp_path: Path, replacement: str, message: str
) -> None:
    _workspace(tmp_path)
    descriptor = tmp_path / "pack" / "bertie-ci.toml"
    descriptor.write_text(
        descriptor.read_text(encoding="utf-8").replace(
            'file = "pack.properties"', replacement
        ),
        encoding="utf-8",
    )

    with pytest.raises(RuntimeError, match=message):
        Workspace.load(tmp_path)


def test_affected_mod_includes_dependents_and_pack_tests(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    affected = workspace.affected(("mods/base/src/main/java/Example.java",))

    assert affected == {"base", "consumer", "pack"}


def test_affected_checks_plan_structural_pack_validation_not_release_export(
    tmp_path: Path,
) -> None:
    workspace = _workspace(tmp_path)
    affected = workspace.affected(("mods/base/src/main/java/Example.java",))

    plan = workspace.plan(affected)

    assert plan["validate"] == [
        {
            "gradle_project": ":pack",
            "project": "pack",
            "subject": "pack",
        }
    ]
    assert "export" not in plan


def test_shared_and_ignored_paths_have_explicit_scope(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    assert workspace.affected(("build-logic/src/Plugin.kt",)) == {
        "base",
        "consumer",
        "pack",
    }
    assert workspace.affected(("tools/bertie-ci/src/runner.py",)) == {
        "base",
        "consumer",
        "pack",
    }
    assert workspace.affected((".github/dependabot.yml",)) == set()
    assert workspace.affected(("README.md", "docs/design.md")) == set()


def test_plan_infers_tasks_from_conventional_source_sets(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    plan = json.loads(plan_json(workspace.plan({"base", "consumer", "pack"})))

    assert plan["build"] == [
        {
            "gradle_project": ":mods:base",
            "project": "mods/base",
            "subject": "base",
            "task": ":mods:base:assemble",
        },
        {
            "gradle_project": ":mods:consumer",
            "project": "mods/consumer",
            "subject": "consumer",
            "task": ":mods:consumer:assemble",
        },
    ]
    assert plan["unit"] == [
        {
            "gradle_project": ":mods:base",
            "project": "mods/base",
            "subject": "base",
            "task": ":mods:base:test",
        },
        {
            "gradle_project": ":pack",
            "project": "pack",
            "subject": "pack",
            "task": ":pack:test",
        },
    ]
    assert plan["gametest"] == [
        {
            "gradle_project": ":mods:consumer",
            "project": "mods/consumer",
            "subject": "consumer",
            "task": ":mods:consumer:runGameTests",
        },
        {
            "gradle_project": ":pack",
            "project": "pack",
            "subject": "pack",
            "task": ":pack:runGameTests",
        },
    ]
    assert plan["client"] == [
        {
            "gradle_project": ":mods:base",
            "project": "mods/base",
            "subject": "base",
            "task": ":mods:base:runClientTests",
        },
        {
            "gradle_project": ":pack",
            "project": "pack",
            "subject": "pack",
            "task": ":pack:runClientTests",
        },
    ]
    assert plan["validate"] == [
        {
            "gradle_project": ":pack",
            "project": "pack",
            "subject": "pack",
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


def test_unknown_product_path_is_global(tmp_path: Path) -> None:
    workspace = _workspace(tmp_path)

    assert workspace.affected(("new-shared-file.toml",)) == {
        "base",
        "consumer",
        "pack",
    }


def test_missing_base_revision_is_global(tmp_path: Path) -> None:
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

    assert workspace.changed_files(child, parent) == ("removed-on-rollback.txt",)


def test_rejects_legacy_suite_configuration(tmp_path: Path) -> None:
    _workspace(tmp_path)
    descriptor = tmp_path / "mods" / "base" / "bertie-ci.toml"
    with descriptor.open("a", encoding="utf-8") as stream:
        stream.write('\n[[suite]]\nid = "unit"\nrunner = "unit"\n')

    with pytest.raises(
        RuntimeError, match=r"Unknown component descriptor key\(s\): suite"
    ):
        Workspace.load(tmp_path)
