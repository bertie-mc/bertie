import json
from pathlib import Path

import pytest
from bertie_ci.instance import (
    Instance,
    install_mod,
    load_instance,
    read_pack_versions,
    write_instance,
)


def test_instance_descriptor_uses_relocatable_game_directory(tmp_path: Path) -> None:
    game_dir = tmp_path / "instance"
    game_dir.mkdir()
    descriptor = write_instance(
        tmp_path,
        Instance("client", game_dir, "1.21.1", "neoforge", "21.1.233"),
    )

    data = json.loads(descriptor.read_text(encoding="utf-8"))
    loaded = load_instance(descriptor)

    assert data["game_dir"] == "instance"
    assert loaded == Instance(
        "client", game_dir.resolve(), "1.21.1", "neoforge", "21.1.233"
    )


def test_instance_descriptor_rejects_directory_escape(tmp_path: Path) -> None:
    outside = tmp_path / "outside"
    outside.mkdir()
    descriptor_dir = tmp_path / "descriptor"
    descriptor_dir.mkdir()
    descriptor = descriptor_dir / "instance.json"
    descriptor.write_text(
        json.dumps(
            {
                "format": "bertie-ci.instance.v1",
                "side": "server",
                "game_dir": "../outside",
                "minecraft": "1.21.1",
                "loader": "neoforge",
                "loader_version": "21.1.233",
            }
        ),
        encoding="utf-8",
    )

    with pytest.raises(RuntimeError, match="escapes"):
        load_instance(descriptor)


def test_read_pack_versions(tmp_path: Path) -> None:
    (tmp_path / "pack.toml").write_text(
        '[versions]\nminecraft = "1.21.1"\nneoforge = "21.1.233"\n',
        encoding="utf-8",
    )

    assert read_pack_versions(tmp_path) == ("1.21.1", "neoforge", "21.1.233")


def test_install_mod_replaces_an_explicit_pack_artifact(tmp_path: Path) -> None:
    game_dir = tmp_path / "prepared" / "instance"
    mods = game_dir / "mods"
    mods.mkdir(parents=True)
    (mods / "example-1.0.0.jar").write_bytes(b"published")
    descriptor = write_instance(
        tmp_path / "prepared",
        Instance("client", game_dir, "1.21.1", "neoforge", "21.1.233"),
    )
    artifact = tmp_path / "artifact" / "example-1.1.0.jar"
    artifact.parent.mkdir()
    artifact.write_bytes(b"current source")

    installed = install_mod(descriptor, artifact, replace_filename="example-1.0.0.jar")

    assert not (mods / "example-1.0.0.jar").exists()
    assert installed == (mods / "example-1.1.0.jar").resolve()
    assert installed.read_bytes() == b"current source"


def test_install_mod_fails_closed_when_replacement_is_missing(tmp_path: Path) -> None:
    game_dir = tmp_path / "prepared" / "instance"
    (game_dir / "mods").mkdir(parents=True)
    descriptor = write_instance(
        tmp_path / "prepared",
        Instance("server", game_dir, "1.21.1", "neoforge", "21.1.233"),
    )
    artifact = tmp_path / "example.jar"
    artifact.touch()

    with pytest.raises(RuntimeError, match="absent mod"):
        install_mod(descriptor, artifact, replace_filename="old.jar")
