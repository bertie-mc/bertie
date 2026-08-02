from __future__ import annotations

import json
import shutil
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Literal

import tomllib

from .artifact import find_artifact
from .config import FixtureTools, PackTools, Versions
from .filesystem import remove_file, remove_tree, replace_file
from .fixture import install_fixtures
from .process import run
from .web import serve_directory

Side = Literal["client", "server"]
DESCRIPTOR_NAME = "instance.json"
DESCRIPTOR_FORMAT = "bertie-ci.instance.v1"


@dataclass(frozen=True)
class Instance:
    side: Side
    game_dir: Path
    minecraft: str
    loader: str
    loader_version: str


def _read_toml(path: Path) -> dict[str, Any]:
    try:
        data = tomllib.loads(path.read_text(encoding="utf-8"))
    except (OSError, tomllib.TOMLDecodeError) as error:
        raise RuntimeError(f"Cannot read {path}: {error}") from error
    if not isinstance(data, dict):
        raise RuntimeError(f"Invalid TOML document: {path}")
    return data


def read_pack_versions(project: Path) -> tuple[str, str, str]:
    pack = _read_toml(project / "pack.toml")
    versions = pack.get("versions")
    if not isinstance(versions, dict):
        raise RuntimeError(f"Missing [versions] in {project / 'pack.toml'}")
    minecraft = versions.get("minecraft")
    loaders = [
        (name, versions[name])
        for name in ("neoforge",)
        if isinstance(versions.get(name), str)
    ]
    if not isinstance(minecraft, str) or len(loaders) != 1:
        raise RuntimeError(
            "Pack must declare one supported loader and a Minecraft version"
        )
    loader, loader_version = loaders[0]
    return minecraft, loader, loader_version


def _reset_instance(output: Path) -> Path:
    output.mkdir(parents=True, exist_ok=True)
    game_dir = (output / "instance").resolve()
    if game_dir.parent != output.resolve():
        raise RuntimeError(f"Unsafe instance directory: {game_dir}")
    if game_dir.exists():
        remove_tree(game_dir)
    game_dir.mkdir(parents=True)
    remove_file(output / DESCRIPTOR_NAME)
    return game_dir


def write_instance(output: Path, instance: Instance) -> Path:
    output = output.resolve()
    try:
        game_dir = instance.game_dir.resolve().relative_to(output)
    except ValueError as error:
        raise RuntimeError(
            "Prepared instance must be inside its output directory"
        ) from error
    descriptor = output / DESCRIPTOR_NAME
    descriptor.write_text(
        json.dumps(
            {
                "format": DESCRIPTOR_FORMAT,
                "side": instance.side,
                "game_dir": game_dir.as_posix(),
                "minecraft": instance.minecraft,
                "loader": instance.loader,
                "loader_version": instance.loader_version,
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    return descriptor


def load_instance(descriptor: Path) -> Instance:
    descriptor = descriptor.resolve(strict=True)
    try:
        data = json.loads(descriptor.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise RuntimeError(
            f"Cannot read instance descriptor {descriptor}: {error}"
        ) from error
    required = {
        "format": str,
        "side": str,
        "game_dir": str,
        "minecraft": str,
        "loader": str,
        "loader_version": str,
    }
    if not isinstance(data, dict) or any(
        not isinstance(data.get(name), kind) for name, kind in required.items()
    ):
        raise RuntimeError(f"Invalid instance descriptor: {descriptor}")
    if data["format"] != DESCRIPTOR_FORMAT:
        raise RuntimeError(f"Unsupported instance descriptor format: {data['format']}")
    if data["side"] not in ("client", "server"):
        raise RuntimeError(f"Invalid instance side: {data['side']}")
    relative = Path(data["game_dir"])
    if relative.is_absolute():
        raise RuntimeError("Instance game_dir must be relative to the descriptor")
    game_dir = (descriptor.parent / relative).resolve(strict=True)
    try:
        game_dir.relative_to(descriptor.parent)
    except ValueError as error:
        raise RuntimeError(
            "Instance game_dir escapes the descriptor directory"
        ) from error
    return Instance(
        side=data["side"],
        game_dir=game_dir,
        minecraft=data["minecraft"],
        loader=data["loader"],
        loader_version=data["loader_version"],
    )


def install_pack(
    project: Path,
    destination: Path,
    side: Literal["client", "server", "both"],
    tools: PackTools,
    log: Path,
) -> None:
    with serve_directory(project) as url:
        run(
            [
                tools.java,
                "-cp",
                tools.packwiz_installer,
                "link.infra.packwiz.installer.Main",
                "--bootstrap-no-update",
                "-g",
                "-s",
                side,
                url,
            ],
            cwd=destination,
            log=log,
            stream_output=False,
        )


def prepare_mod_instance(
    project: Path,
    requested_artifact: Path | None,
    profiles: list[str],
    side: Side,
    output: Path,
    versions: Versions,
    tools: FixtureTools,
    instance_files: Path | None = None,
) -> Path:
    output = output.resolve()
    game_dir = _reset_instance(output)
    (game_dir / "mods").mkdir()
    install_fixtures(tools, versions, game_dir, output, profiles, side)
    if instance_files is not None:
        shutil.copytree(instance_files, game_dir, dirs_exist_ok=True)
    artifact = find_artifact(project, requested_artifact)
    replace_file(artifact, game_dir / "mods" / "mod-under-test.jar")
    return write_instance(
        output,
        Instance(side, game_dir, versions.minecraft, "neoforge", versions.neoforge),
    )


def prepare_pack_instance(
    project: Path,
    side: Side,
    output: Path,
    tools: PackTools,
) -> Path:
    output = output.resolve()
    game_dir = _reset_instance(output)
    minecraft, loader, loader_version = read_pack_versions(project)
    print(f"Installing {side} pack into {game_dir}", flush=True)
    install_pack(project, game_dir, side, tools, output / "pack-install.log")
    return write_instance(
        output, Instance(side, game_dir, minecraft, loader, loader_version)
    )


def install_mod(
    descriptor: Path,
    requested_artifact: Path,
    *,
    replace_filename: str | None = None,
) -> Path:
    """Install one built mod into an existing prepared instance.

    ``replace_filename`` is deliberately explicit. Instance assembly does not guess mod
    identity from filenames or silently leave the pack's published copy beside a
    current-source artifact.
    """
    instance = load_instance(descriptor)
    artifact = find_artifact(descriptor.parent, requested_artifact)
    mods = instance.game_dir / "mods"
    mods.mkdir(parents=True, exist_ok=True)

    if replace_filename is not None:
        replacement = Path(replace_filename)
        if replacement.name != replace_filename or replace_filename in ("", ".", ".."):
            raise RuntimeError(
                f"Replacement mod filename must be a plain filename: {replace_filename!r}"
            )
        installed = mods / replace_filename
        if not installed.is_file():
            raise RuntimeError(
                f"Cannot replace absent mod {replace_filename!r} in {mods}"
            )
        if installed.name != artifact.name:
            remove_file(installed)

    destination = mods / artifact.name
    if destination.exists() and (
        replace_filename is None or destination.name != replace_filename
    ):
        raise RuntimeError(f"Mod destination already exists: {destination}")
    replace_file(artifact, destination)
    return destination.resolve(strict=True)
