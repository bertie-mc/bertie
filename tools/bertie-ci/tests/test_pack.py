import os
from pathlib import Path
from zipfile import ZipFile

import bertie_ci.pack as pack_module
import pytest
from bertie_ci.pack import (
    export_client_pack,
    export_server_pack,
    read_pack_mod,
    validate_pack,
)


def _pack(
    root: Path,
    second_filename: str | None = None,
    *,
    first_side: str = "both",
    second_side: str = "client",
) -> None:
    mods = root / "mods"
    config = root / "config"
    mods.mkdir()
    config.mkdir()
    (config / "example.json").write_text("{}\n", encoding="utf-8")
    (mods / "owned.jar").write_bytes(b"locally built mod")
    (mods / "example.pw.toml").write_text(
        f'name = "Example"\nfilename = "example.jar"\nside = "{first_side}"\n\n'
        '[download]\nurl = "https://example.invalid/example.jar"\n'
        'hash-format = "sha256"\nhash = "00"\n',
        encoding="utf-8",
    )
    entries = [
        '[[files]]\nfile = "config/example.json"\nhash = "00"',
        '[[files]]\nfile = "mods/owned.jar"\nhash = "00"',
        '[[files]]\nfile = "mods/example.pw.toml"\nhash = "00"\nmetafile = true',
    ]
    if second_filename is not None:
        (mods / "second.pw.toml").write_text(
            f'name = "Second"\nfilename = "{second_filename}"\n'
            f'side = "{second_side}"\n\n'
            '[download]\nurl = "https://example.invalid/second.jar"\n'
            'hash-format = "sha256"\nhash = "00"\n',
            encoding="utf-8",
        )
        entries.append(
            '[[files]]\nfile = "mods/second.pw.toml"\nhash = "00"\nmetafile = true'
        )
    (root / "index.toml").write_text(
        'hash-format = "sha256"\n\n' + "\n\n".join(entries) + "\n",
        encoding="utf-8",
    )
    (root / "pack.toml").write_text(
        'name = "Example"\nversion = "1.0.0"\npack-format = "packwiz:1.1.0"\n\n'
        '[index]\nfile = "index.toml"\nhash-format = "sha256"\nhash = "00"\n\n'
        '[versions]\nminecraft = "1.21.1"\nneoforge = "21.1.233"\n',
        encoding="utf-8",
    )


def test_validate_pack_is_read_only_and_reports_local_mod_jars(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    _pack(tmp_path)
    before = {
        path.relative_to(tmp_path): path.read_bytes()
        for path in tmp_path.rglob("*")
        if path.is_file()
    }
    invocation: dict[str, object] = {}

    def refresh(command: object, *, cwd: Path, **_kwargs: object) -> None:
        invocation.update(command=command, cwd=cwd)

    monkeypatch.setattr(pack_module, "run", refresh)

    summary = validate_pack(tmp_path, Path("packwiz"))

    assert summary.metafiles == 1
    assert summary.local_mod_jars == 1
    assert summary.config_files == 1
    assert invocation["command"] == [Path("packwiz"), "refresh"]
    assert invocation["cwd"] != tmp_path
    assert {
        path.relative_to(tmp_path): path.read_bytes()
        for path in tmp_path.rglob("*")
        if path.is_file()
    } == before


def test_validate_pack_allows_disjoint_client_and_server_download_names(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    _pack(
        tmp_path,
        "example.jar",
        first_side="client",
        second_side="server",
    )
    monkeypatch.setattr(pack_module, "run", lambda *args, **kwargs: None)

    summary = validate_pack(tmp_path, Path("packwiz"))

    assert summary.metafiles == 2


def test_validate_pack_rejects_overlapping_both_and_client_download_names(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    _pack(tmp_path, "example.jar")
    monkeypatch.setattr(pack_module, "run", lambda *args, **kwargs: None)

    with pytest.raises(RuntimeError, match="Duplicate target filenames"):
        validate_pack(tmp_path, Path("packwiz"))


def test_pack_mod_metadata_exposes_its_physical_side(tmp_path: Path) -> None:
    _pack(tmp_path, "second.jar")

    first = read_pack_mod(tmp_path, Path("mods/example.pw.toml"))
    second = read_pack_mod(tmp_path, Path("mods/second.pw.toml"))

    assert first.filename == "example.jar"
    assert first.side == "both"
    assert second.filename == "second.jar"
    assert second.side == "client"


def test_pack_mod_metadata_rejects_an_invalid_physical_side(tmp_path: Path) -> None:
    _pack(tmp_path)
    metafile = tmp_path / "mods" / "example.pw.toml"
    metafile.write_text(
        metafile.read_text(encoding="utf-8").replace(
            'side = "both"', 'side = "somewhere"'
        ),
        encoding="utf-8",
    )

    with pytest.raises(RuntimeError, match="Invalid artifact side"):
        read_pack_mod(tmp_path, Path("mods/example.pw.toml"))


def test_validate_pack_rejects_local_and_remote_files_with_the_same_target(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    _pack(tmp_path)
    metafile = tmp_path / "mods" / "example.pw.toml"
    metafile.write_text(
        metafile.read_text(encoding="utf-8").replace(
            'filename = "example.jar"', 'filename = "owned.jar"'
        ),
        encoding="utf-8",
    )
    monkeypatch.setattr(pack_module, "run", lambda *args, **kwargs: None)

    with pytest.raises(RuntimeError, match="Duplicate target filenames"):
        validate_pack(tmp_path, Path("packwiz"))


def test_client_export_passes_locally_built_mods_to_packwiz(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    project = tmp_path / "project"
    project.mkdir()
    _pack(project)
    output = tmp_path / "example.mrpack"
    observed: dict[str, object] = {}

    def export(command: list[object], *, cwd: Path) -> None:
        observed["command"] = command
        observed["owned"] = (cwd / "mods" / "owned.jar").read_bytes()
        Path(command[-1]).write_bytes(b"mrpack")

    monkeypatch.setattr(pack_module, "run", export)

    export_client_pack(project, output, Path("packwiz"))

    assert observed == {
        "command": [Path("packwiz"), "modrinth", "export", "-o", output],
        "owned": b"locally built mod",
    }


def test_export_server_pack_contains_target_aware_manifest_and_local_mod_jars(
    tmp_path: Path,
) -> None:
    project = tmp_path / "project"
    project.mkdir()
    _pack(project)
    installer = tmp_path / "packwiz-installer.jar"
    installer.write_bytes(b"installer")
    os.utime(installer, (0, 0))
    readme = project / "README.md"
    readme.write_text("# Example pack\n", encoding="utf-8")
    output = tmp_path / "example-server.zip"

    export_server_pack(project, output, installer, readme)

    with ZipFile(output) as archive:
        names = set(archive.namelist())
        root = "example-server"
        assert f"{root}/pack/pack.toml" in names
        assert f"{root}/pack/mods/example.pw.toml" in names
        assert f"{root}/packwiz-installer.jar" in names
        assert f"{root}/README.md" in names
        assert f"{root}/start.sh" in names
        assert archive.read(f"{root}/pack/mods/owned.jar") == b"locally built mod"
