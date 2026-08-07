from __future__ import annotations

import hashlib
import io
import json
import zipfile
from pathlib import Path
from urllib.parse import parse_qs, urlparse

import pytest
import tomllib
from bertie_ci.deps import (
    LOCK_POLICY,
    check_locks,
    inputs_hash,
    load_inputs,
    refresh_locks,
    select_distribution,
)
from bertie_ci.deps_audit import audit_modrinth


def _write(path: Path, contents: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(contents.strip() + "\n", encoding="utf-8")


def _jar(*entries: str) -> bytes:
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w") as archive:
        for entry in entries:
            archive.writestr(entry, "")
    return output.getvalue()


def _fixture(root: Path) -> None:
    _write(
        root / "pack/build.gradle.kts",
        """
        dependencies {
            packComponents(deps.content)
        }
        """,
    )
    _write(
        root / "deps/platform.toml",
        """
        minecraft = "1.21.1"
        loader = "neoforge"
        loader-version = "21.1.233"
        """,
    )
    _write(
        root / "deps/components/content.toml",
        """
        [distributions.modrinth-datapack]
        provider = "modrinth"
        kind = "datapack"
        project-id = "content"
        version-id = "data-version"
        filename = "content.zip"

        [distributions.curseforge-mod]
        provider = "curseforge"
        kind = "mod"
        slug = "content"
        project-id = 10
        file-id = 20
        filename = "content.jar"
        """,
    )
    _write(
        root / "deps/components/paxi.toml",
        """
        [distributions.modrinth-mod]
        provider = "modrinth"
        kind = "mod"
        project-id = "paxi"
        version-id = "paxi-version"
        filename = "paxi.jar"
        """,
    )
    _write(
        root / "deps/profiles/development.toml",
        """
        selection-priority = ["representation", "provider"]
        representation-preference = ["mod", "datapack", "resourcepack", "shaderpack"]
        provider-preference = ["maven", "modrinth", "curseforge"]

        [native-packs.datapack]
        mode = "loader"
        component = "paxi"
        """,
    )
    _write(
        root / "deps/profiles/release-modrinth.toml",
        """
        selection-priority = ["provider", "representation"]
        representation-preference = ["datapack", "resourcepack", "shaderpack", "mod"]
        provider-preference = ["modrinth", "curseforge"]

        [native-packs.datapack]
        mode = "loader"
        component = "paxi"
        """,
    )
    _write(
        root / "deps/profiles/release-curseforge.toml",
        """
        selection-priority = ["provider", "representation"]
        representation-preference = ["datapack", "resourcepack", "shaderpack", "mod"]
        provider-preference = ["curseforge", "modrinth"]

        [native-packs.datapack]
        mode = "loader"
        component = "paxi"
        """,
    )


def _artifact(
    *, component: str, provider: str, kind: str, module: str, version: str
) -> str:
    identity = (
        f"modrinth:{module}:{version}" if provider == "modrinth" else "curseforge:10:20"
    )
    extra = (
        f'project-id = "{module}"\nversion-id = "{version}"'
        if provider == "modrinth"
        else 'slug = "content"\nproject-id = 10\nfile-id = 20'
    )
    group = "maven.modrinth" if provider == "modrinth" else "curse.maven"
    gradle_module = module if provider == "modrinth" else "content-10"
    return f"""
    [artifacts."{identity}"]
    component = "{component}"
    provider = "{provider}"
    kind = "{kind}"
    side = "both"
    group = "{group}"
    module = "{gradle_module}"
    version = "{version}"
    filename = "{component}.{"jar" if kind == "mod" else "zip"}"
    {extra}
    provides = []
    bundled-provides = []
    required = []
    optional = []
    incompatible = []
    bundled = []
    integrations = []
    """


def _lock(root: Path, profile_id: str) -> None:
    inputs = load_inputs(root)
    profile = inputs.profiles[profile_id]
    content = select_distribution(inputs.components["content"], profile)
    paxi = select_distribution(inputs.components["paxi"], profile)
    content_mod = select_distribution(inputs.components["content"], profile, kind="mod")
    text = f"""
    profile = "{profile_id}"
    inputs-hash = "{inputs_hash(inputs, profile)}"
    relationships = []

    [components.content]
    any = "{content.identity}"
    mod = "{content_mod.identity}"

    [components.paxi]
    any = "{paxi.identity}"
    mod = "{paxi.identity}"
    """
    distributions = {
        content.identity: content,
        content_mod.identity: content_mod,
        paxi.identity: paxi,
    }
    for identity, distribution in distributions.items():
        if distribution.provider == "curseforge":
            text += _artifact(
                component="content",
                provider="curseforge",
                kind="mod",
                module="content",
                version="20",
            )
        else:
            component = "paxi" if identity == paxi.identity else "content"
            text += _artifact(
                component=component,
                provider="modrinth",
                kind=distribution.kind,
                module=str(distribution.values["project-id"]),
                version=str(distribution.values["version-id"]),
            )
    _write(root / f"deps/locks/{profile_id}.lock.toml", text)


def test_profiles_choose_representation_and_provider_in_declared_order(
    tmp_path: Path,
) -> None:
    _fixture(tmp_path)
    inputs = load_inputs(tmp_path)
    component = inputs.components["content"]

    assert select_distribution(component, inputs.profiles["development"]).kind == "mod"
    assert (
        select_distribution(component, inputs.profiles["release-modrinth"]).provider
        == "modrinth"
    )
    assert (
        select_distribution(component, inputs.profiles["release-curseforge"]).provider
        == "curseforge"
    )


def test_modrinth_audit_reports_an_available_alternate_representation(
    tmp_path: Path,
) -> None:
    _fixture(tmp_path)
    versions = {
        "data-version": {
            "id": "data-version",
            "project_id": "content",
            "game_versions": ["1.21.1"],
            "loaders": ["datapack"],
            "files": [{"filename": "content.zip", "primary": True}],
        },
        "mod-version": {
            "id": "mod-version",
            "project_id": "content",
            "game_versions": ["1.21.1"],
            "loaders": ["forge"],
            "files": [
                {
                    "filename": "content.jar",
                    "primary": True,
                    "url": "https://cdn.example/content.jar",
                }
            ],
        },
        "paxi-version": {
            "id": "paxi-version",
            "project_id": "paxi",
            "game_versions": ["1.21.1"],
            "loaders": ["neoforge"],
            "files": [{"filename": "paxi.jar", "primary": True}],
        },
    }
    projects = {
        "content": {
            "id": "content",
            "project_type": "mod",
            "loaders": ["datapack", "forge"],
        },
        "paxi": {
            "id": "paxi",
            "project_type": "mod",
            "loaders": ["neoforge"],
        },
    }

    def fetch(url: str) -> object:
        parsed = urlparse(url)
        if parsed.path.endswith("/versions"):
            ids = json.loads(parse_qs(parsed.query)["ids"][0])
            return [versions[identity] for identity in ids]
        if parsed.path.endswith("/projects"):
            ids = json.loads(parse_qs(parsed.query)["ids"][0])
            return [projects[identity] for identity in ids]
        if parsed.path.endswith("/project/content/version"):
            return [versions["mod-version"], versions["data-version"]]
        raise AssertionError(f"Unexpected audit request: {url}")

    audit = audit_modrinth(
        tmp_path,
        fetch_json=fetch,
        fetch_bytes=lambda url: (
            _jar("META-INF/neoforge.mods.toml")
            if url == "https://cdn.example/content.jar"
            else pytest.fail(f"Unexpected archive request: {url}")
        ),
    )

    assert audit.distributions == 2
    assert audit.projects == 2
    assert audit.findings == (
        "content: Modrinth project content also publishes a Minecraft 1.21.1 mod: "
        "mod-version (content.jar)",
    )


def test_modrinth_audit_reports_exact_metadata_and_provider_discrepancies(
    tmp_path: Path,
) -> None:
    _fixture(tmp_path)

    def fetch(url: str) -> object:
        parsed = urlparse(url)
        if parsed.path.endswith("/versions"):
            return [
                {
                    "id": "data-version",
                    "project_id": "content",
                    "game_versions": ["1.21"],
                    "loaders": ["datapack"],
                    "files": [{"filename": "renamed.zip", "primary": True}],
                },
                {
                    "id": "paxi-version",
                    "project_id": "paxi",
                    "game_versions": ["1.21.1"],
                    "loaders": ["forge"],
                    "files": [
                        {
                            "filename": "paxi.jar",
                            "primary": True,
                            "url": "https://cdn.example/paxi.jar",
                        }
                    ],
                },
            ]
        if parsed.path.endswith("/projects"):
            return [
                {
                    "id": "content",
                    "project_type": "mod",
                    "loaders": ["datapack"],
                },
                {"id": "paxi", "project_type": "mod", "loaders": ["forge"]},
            ]
        if parsed.path.endswith("/project/content/version"):
            return []
        raise AssertionError(f"Unexpected audit request: {url}")

    audit = audit_modrinth(
        tmp_path,
        fetch_json=fetch,
        fetch_bytes=lambda url: (
            _jar("META-INF/neoforge.mods.toml")
            if url == "https://cdn.example/paxi.jar"
            else pytest.fail(f"Unexpected archive request: {url}")
        ),
    )

    assert audit.findings == (
        "content.modrinth-datapack: version data-version does not contain declared "
        "file 'content.zip'",
        "content.modrinth-datapack: version data-version does not advertise Minecraft "
        "1.21.1",
        "paxi.modrinth-mod: provider metadata discrepancy: version paxi-version "
        "advertises loaders ['forge'], but declared file 'paxi.jar' contains "
        "META-INF/neoforge.mods.toml",
    )


def test_check_reports_stale_lock_and_missing_required_artifact(tmp_path: Path) -> None:
    _fixture(tmp_path)
    _lock(tmp_path, "development")
    _lock(tmp_path, "release-curseforge")
    _lock(tmp_path, "release-modrinth")
    development = tmp_path / "deps/locks/development.lock.toml"
    contents = development.read_text(encoding="utf-8")
    contents = contents.replace(
        "required = []",
        'required = [{ artifact = "modrinth:missing:release" }]',
        1,
    )
    development.write_text(contents, encoding="utf-8")
    (tmp_path / "deps/components/content.toml").write_text(
        (tmp_path / "deps/components/content.toml").read_text(encoding="utf-8")
        + "\n# changed\n",
        encoding="utf-8",
    )

    errors = check_locks(tmp_path)

    assert any("stale inputs-hash" in error for error in errors)
    assert any("unresolved required edge" in error for error in errors)


def test_check_finds_consumers_when_workspace_parent_is_named_build(
    tmp_path: Path,
) -> None:
    root = tmp_path / "build" / "workspace"
    _fixture(root)
    for profile_id in ("development", "release-curseforge", "release-modrinth"):
        _lock(root, profile_id)
    refresh_locks(root)

    assert check_locks(root) == ()


def test_check_requires_owned_mod_runtime_dependency_to_be_direct(
    tmp_path: Path,
) -> None:
    _fixture(tmp_path)
    for profile_id in ("development", "release-curseforge", "release-modrinth"):
        _lock(tmp_path, profile_id)
        lock = tmp_path / f"deps/locks/{profile_id}.lock.toml"
        lock.write_text(
            lock.read_text(encoding="utf-8").replace(
                "provides = []", 'provides = ["content"]', 1
            ),
            encoding="utf-8",
        )
    _write(tmp_path / "mods/example/mod.properties", "mod_id=example")
    _write(
        tmp_path / "mods/example/src/main/templates/META-INF/neoforge.mods.toml",
        """
        [[dependencies."${mod_id}"]]
        modId = "content"
        type = "required"
        versionRange = "[1,)"
        ordering = "NONE"
        side = "BOTH"
        """,
    )
    build = tmp_path / "mods/example/build.gradle.kts"
    _write(
        build,
        """
        dependencies {
            compileOnly(deps.content)
        }
        """,
    )
    refresh_locks(tmp_path)

    errors = check_locks(tmp_path)

    assert any(
        "declare runtimeOnly(deps.content) directly" in error for error in errors
    )

    build.write_text(
        build.read_text(encoding="utf-8").replace("compileOnly", "runtimeOnly"),
        encoding="utf-8",
    )

    assert check_locks(tmp_path) == ()


def test_refresh_reuses_immutable_metadata_and_prunes_unreachable_artifacts(
    tmp_path: Path,
) -> None:
    _fixture(tmp_path)
    _lock(tmp_path, "development")
    _lock(tmp_path, "release-curseforge")
    _lock(tmp_path, "release-modrinth")
    release = tmp_path / "deps/locks/release-modrinth.lock.toml"
    release.write_text(
        release.read_text(encoding="utf-8")
        + _artifact(
            component="unused",
            provider="modrinth",
            kind="mod",
            module="unused",
            version="1",
        ),
        encoding="utf-8",
    )

    refresh_locks(tmp_path)

    assert check_locks(tmp_path) == ()
    assert "modrinth:unused:1" not in release.read_text(encoding="utf-8")


def test_refresh_reuses_archive_metadata_for_an_equivalent_distribution(
    tmp_path: Path,
) -> None:
    _fixture(tmp_path)
    content = tmp_path / "deps/components/content.toml"
    content.write_text(
        content.read_text(encoding="utf-8")
        + """

        [distributions.modrinth-mod]
        provider = "modrinth"
        kind = "mod"
        project-id = "content-mod"
        version-id = "mod-version"
        filename = "content.jar"
        """,
        encoding="utf-8",
    )
    _lock(tmp_path, "development")
    _lock(tmp_path, "release-curseforge")
    _lock(tmp_path, "release-modrinth")
    content.write_text(
        content.read_text(encoding="utf-8").replace("file-id = 20", "file-id = 21"),
        encoding="utf-8",
    )

    refresh_locks(tmp_path)

    assert check_locks(tmp_path) == ()
    release = tomllib.loads(
        (tmp_path / "deps/locks/release-curseforge.lock.toml").read_text(
            encoding="utf-8"
        )
    )
    selected = release["components"]["content"]["any"]
    assert selected == "curseforge:10:21"
    assert release["artifacts"][selected]["file-id"] == 21


def test_refresh_retargets_dependency_to_selected_equivalent_distribution(
    tmp_path: Path,
) -> None:
    _fixture(tmp_path)
    for profile_id in ("development", "release-curseforge", "release-modrinth"):
        _lock(tmp_path, profile_id)
        lock = tmp_path / f"deps/locks/{profile_id}.lock.toml"
        contents = lock.read_text(encoding="utf-8").replace(
            "required = []",
            'required = [{ artifact = "modrinth:library:1", mod-id = "library", origin = "archive" }]',
            1,
        )
        contents += _artifact(
            component="library",
            provider="modrinth",
            kind="mod",
            module="library",
            version="1",
        ).replace('component = "library"\n', "")
        lock.write_text(contents, encoding="utf-8")
    _write(
        tmp_path / "deps/components/library.toml",
        """
        [distributions.modrinth-mod]
        provider = "modrinth"
        kind = "mod"
        project-id = "library"
        version-id = "1"
        filename = "library.jar"

        [distributions.curseforge-mod]
        provider = "curseforge"
        kind = "mod"
        slug = "library"
        project-id = 30
        file-id = 40
        filename = "library.jar"
        """,
    )
    build = tmp_path / "pack/build.gradle.kts"
    build.write_text(
        build.read_text(encoding="utf-8").replace(
            "packComponents(deps.content)",
            "packComponents(deps.content)\n            packComponents(deps.library)",
        ),
        encoding="utf-8",
    )

    refresh_locks(tmp_path)

    assert check_locks(tmp_path) == ()
    release = tomllib.loads(
        (tmp_path / "deps/locks/release-curseforge.lock.toml").read_text(
            encoding="utf-8"
        )
    )
    assert release["components"]["library"]["any"] == "curseforge:30:40"
    content = release["artifacts"]["curseforge:10:20"]
    assert content["required"] == [
        {
            "artifact": "curseforge:30:40",
            "mod-id": "library",
            "origin": "archive",
        }
    ]
    assert "modrinth:library:1" not in release["artifacts"]


def test_refresh_regenerates_profile_native_pack_dependency(tmp_path: Path) -> None:
    _fixture(tmp_path)
    _lock(tmp_path, "development")
    _lock(tmp_path, "release-curseforge")
    _lock(tmp_path, "release-modrinth")

    refresh_locks(tmp_path)

    release = tmp_path / "deps/locks/release-modrinth.lock.toml"
    contents = release.read_text(encoding="utf-8")
    assert '"origin" = "profile:release-modrinth:native-packs.datapack"' in contents
    assert '"artifact" = "modrinth:paxi:paxi-version"' in contents

    contents = contents.replace(
        '"origin" = "profile:release-modrinth:native-packs.datapack"',
        '"origin" = "profile:obsolete:native-packs.datapack"',
    )
    release.write_text(contents, encoding="utf-8")

    refresh_locks(tmp_path)

    refreshed = release.read_text(encoding="utf-8")
    assert "profile:obsolete" not in refreshed
    assert '"origin" = "profile:release-modrinth:native-packs.datapack"' in refreshed


def test_refresh_records_additional_native_pack_installation_kinds(
    tmp_path: Path,
) -> None:
    _fixture(tmp_path)
    for profile_id in ("development", "release-curseforge", "release-modrinth"):
        _lock(tmp_path, profile_id)
    content = tmp_path / "deps/components/content.toml"
    content.write_text(
        content.read_text(encoding="utf-8").replace(
            'kind = "datapack"',
            'kind = "datapack"\nadditional-kinds = ["resourcepack"]',
            1,
        ),
        encoding="utf-8",
    )

    refresh_locks(tmp_path)

    release = tomllib.loads(
        (tmp_path / "deps/locks/release-modrinth.lock.toml").read_text(encoding="utf-8")
    )
    selected = release["components"]["content"]["any"]
    assert release["artifacts"][selected]["additional-kinds"] == ["resourcepack"]
    assert check_locks(tmp_path) == ()


@pytest.mark.parametrize("provides_field", ["provides", "bundled-provides"])
def test_refresh_applies_exact_required_dependency_correction(
    tmp_path: Path, provides_field: str
) -> None:
    _fixture(tmp_path)
    _lock(tmp_path, "development")
    _lock(tmp_path, "release-curseforge")
    _lock(tmp_path, "release-modrinth")
    _write(
        tmp_path / "deps/components/library.toml",
        """
        [distributions.modrinth-mod]
        provider = "modrinth"
        kind = "mod"
        project-id = "library"
        version-id = "1"
        filename = "library.jar"
        """,
    )
    content = tmp_path / "deps/components/content.toml"
    content.write_text(
        content.read_text(encoding="utf-8")
        + """

        [distributions.curseforge-mod.dependency-corrections.library-is-required]
        # The source archive loads this optional dependency unconditionally.
        action = "require"
        mod-id = "library"
        component = "library"
        version-range = "[1]"
        side = "both"
        applies-to = "curseforge:10:20"

        [distributions.curseforge-mod.dependency-corrections.library-api-is-required]
        action = "require"
        mod-id = "library_api"
        component = "library"
        version-range = "[1]"
        side = "both"
        applies-to = "curseforge:10:20"
        """,
        encoding="utf-8",
    )
    library_artifact = _artifact(
        component="library",
        provider="modrinth",
        kind="mod",
        module="library",
        version="1",
    ).replace(
        f"{provides_field} = []",
        f'{provides_field} = ["library", "library_api"]',
    )
    for profile_id in ("development", "release-curseforge", "release-modrinth"):
        lock = tmp_path / f"deps/locks/{profile_id}.lock.toml"
        lock.write_text(
            lock.read_text(encoding="utf-8") + library_artifact,
            encoding="utf-8",
        )

    refresh_locks(tmp_path)

    assert check_locks(tmp_path) == ()
    release = tomllib.loads(
        (tmp_path / "deps/locks/release-modrinth.lock.toml").read_text(encoding="utf-8")
    )
    source = release["artifacts"]["curseforge:10:20"]
    assert source["required"] == [
        {
            "artifact": "modrinth:library:1",
            "mod-id": "library",
            "version-range": "[1]",
            "side": "both",
            "origin": "correction:content:curseforge-mod:library-is-required",
        },
        {
            "artifact": "modrinth:library:1",
            "mod-id": "library_api",
            "version-range": "[1]",
            "side": "both",
            "origin": "correction:content:curseforge-mod:library-api-is-required",
        },
    ]


def test_refresh_resolves_provider_optional_project_and_records_component_relationship(
    tmp_path: Path,
) -> None:
    _fixture(tmp_path)
    for profile_id in ("development", "release-curseforge", "release-modrinth"):
        _lock(tmp_path, profile_id)
    _write(
        tmp_path / "deps/components/addon.toml",
        """
        [relationships.content]
        kind = "optional-addon-for"

        [distributions.modrinth-resourcepack]
        provider = "modrinth"
        kind = "resourcepack"
        side = "client"
        project-id = "addon-project"
        version-id = "addon-version"
        filename = "addon.zip"
        """,
    )
    build = tmp_path / "pack/build.gradle.kts"
    build.write_text(
        build.read_text(encoding="utf-8").replace(
            "packComponents(deps.content)",
            "packComponents(deps.content)\n            packComponents(deps.addon)",
        ),
        encoding="utf-8",
    )
    addon_artifact = _artifact(
        component="addon",
        provider="modrinth",
        kind="resourcepack",
        module="addon-project",
        version="addon-version",
    )
    for profile_id in ("development", "release-curseforge", "release-modrinth"):
        lock = tmp_path / f"deps/locks/{profile_id}.lock.toml"
        lock.write_text(
            lock.read_text(encoding="utf-8") + addon_artifact,
            encoding="utf-8",
        )
    development_lock = tmp_path / "deps/locks/development.lock.toml"
    development_lock.write_text(
        development_lock.read_text(encoding="utf-8").replace(
            "optional = []",
            'optional = [{ artifact = "modrinth:addon-project:addon-version", origin = "correction:obsolete" }]',
            1,
        ),
        encoding="utf-8",
    )
    release_modrinth = tmp_path / "deps/locks/release-modrinth.lock.toml"
    release_modrinth.write_text(
        release_modrinth.read_text(encoding="utf-8").replace(
            "optional = []",
            'optional = [{ missing = "modrinth:addon-project", origin = "provider" }]',
            1,
        ),
        encoding="utf-8",
    )

    refresh_locks(tmp_path)

    assert check_locks(tmp_path) == ()
    modrinth = tomllib.loads(release_modrinth.read_text(encoding="utf-8"))
    assert modrinth["relationships"] == [
        {
            "source": "addon",
            "target": "content",
            "kind": "optional-addon-for",
        }
    ]
    modrinth_content = modrinth["artifacts"]["modrinth:content:data-version"]
    assert modrinth_content["optional"] == [
        {"artifact": "modrinth:addon-project:addon-version", "origin": "provider"}
    ]
    development = tomllib.loads(
        (tmp_path / "deps/locks/development.lock.toml").read_text(encoding="utf-8")
    )
    development_content = development["artifacts"]["curseforge:10:20"]
    assert development_content["optional"] == []


def test_component_relationship_requires_a_known_distinct_target(
    tmp_path: Path,
) -> None:
    _fixture(tmp_path)
    component = tmp_path / "deps/components/content.toml"
    component.write_text(
        component.read_text(encoding="utf-8")
        + """

        [relationships.missing]
        kind = "optional-addon-for"
        """,
        encoding="utf-8",
    )

    with pytest.raises(RuntimeError, match="unknown component 'missing'"):
        load_inputs(tmp_path)


def test_input_hash_names_every_input_and_policy(tmp_path: Path) -> None:
    _fixture(tmp_path)
    inputs = load_inputs(tmp_path)
    digest = inputs_hash(inputs, inputs.profiles["development"])

    expected = hashlib.sha256()
    expected.update(LOCK_POLICY.encode())
    expected.update(b"\0")
    paths = [tmp_path / "deps/platform.toml"]
    paths.extend(sorted((tmp_path / "deps/components").glob("*.toml")))
    paths.append(tmp_path / "deps/profiles/development.toml")
    for path in paths:
        expected.update(path.relative_to(tmp_path).as_posix().encode())
        expected.update(b"\0")
        expected.update(path.read_bytes())
        expected.update(b"\0")

    assert digest == expected.hexdigest()


def test_component_requires_a_distribution(tmp_path: Path) -> None:
    _fixture(tmp_path)
    component = tmp_path / "deps/components/content.toml"
    component.write_text("# No distributions.\n", encoding="utf-8")

    with pytest.raises(RuntimeError, match="distributions"):
        load_inputs(tmp_path)


def test_component_rejects_mod_as_an_additional_installation_kind(
    tmp_path: Path,
) -> None:
    _fixture(tmp_path)
    component = tmp_path / "deps/components/content.toml"
    component.write_text(
        component.read_text(encoding="utf-8").replace(
            'kind = "datapack"',
            'kind = "datapack"\nadditional-kinds = ["mod"]',
            1,
        ),
        encoding="utf-8",
    )

    with pytest.raises(RuntimeError, match="additional mod"):
        load_inputs(tmp_path)
