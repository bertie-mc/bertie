from __future__ import annotations

import io
import json
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from collections import defaultdict
from collections.abc import Callable, Mapping
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import tomllib

from .deps import Distribution, load_inputs

MODRINTH_API = "https://api.modrinth.com/v2"
JsonFetcher = Callable[[str], Any]
BytesFetcher = Callable[[str], bytes]

LOADER_DESCRIPTORS = {
    "fabric": "fabric.mod.json",
    "forge": "META-INF/mods.toml",
    "neoforge": "META-INF/neoforge.mods.toml",
    "quilt": "quilt.mod.json",
}


@dataclass(frozen=True)
class DependencyAudit:
    distributions: int
    projects: int
    findings: tuple[str, ...]


def audit_modrinth(
    root: Path,
    *,
    fetch_json: JsonFetcher | None = None,
    fetch_bytes: BytesFetcher | None = None,
) -> DependencyAudit:
    """Compare declared Modrinth distributions with current provider metadata.

    This is deliberately advisory and networked. Deterministic dependency validation
    remains in deps-check.
    """
    inputs = load_inputs(root)
    platform = _load_platform(inputs.root / "deps" / "platform.toml")
    fetch = fetch_json or _fetch_json
    download = fetch_bytes or _fetch_bytes
    archive_loaders: dict[str, frozenset[str]] = {}
    distributions = [
        distribution
        for component in inputs.components.values()
        for distribution in component.distributions
        if distribution.provider == "modrinth"
    ]
    component_by_distribution = {
        distribution.identity: component.id
        for component in inputs.components.values()
        for distribution in component.distributions
        if distribution.provider == "modrinth"
    }
    version_ids = sorted(
        {str(distribution.values["version-id"]) for distribution in distributions}
    )
    project_ids = sorted(
        {str(distribution.values["project-id"]) for distribution in distributions}
    )
    versions = _fetch_many(fetch, "versions", version_ids)
    projects = _fetch_many(fetch, "projects", project_ids)

    findings: list[str] = []
    for distribution in sorted(distributions, key=lambda item: item.identity):
        component = component_by_distribution[distribution.identity]
        version_id = str(distribution.values["version-id"])
        project_id = str(distribution.values["project-id"])
        version = versions.get(version_id)
        if version is None:
            findings.append(
                f"{component}.{distribution.id}: Modrinth version {version_id} "
                "does not exist"
            )
            continue
        if version.get("project_id") != project_id:
            findings.append(
                f"{component}.{distribution.id}: version {version_id} belongs to "
                f"project {version.get('project_id')!r}, not {project_id!r}"
            )
        filenames = {
            item.get("filename")
            for item in version.get("files", [])
            if isinstance(item, dict)
        }
        filename = distribution.values["filename"]
        if filename not in filenames:
            findings.append(
                f"{component}.{distribution.id}: version {version_id} does not "
                f"contain declared file {filename!r}"
            )
        game_versions = version.get("game_versions", [])
        if platform["minecraft"] not in game_versions:
            findings.append(
                f"{component}.{distribution.id}: version {version_id} does not "
                f"advertise Minecraft {platform['minecraft']}"
            )
        project = projects.get(project_id, {})
        if distribution.kind == "mod" and not _version_matches_kind(
            version, project, distribution.kind, platform["loader"]
        ):
            loaders = _loaders_from_archive(
                _declared_file(version, str(filename)), download, archive_loaders
            )
            if platform["loader"] in loaders:
                findings.append(
                    f"{component}.{distribution.id}: provider metadata discrepancy: "
                    f"version {version_id} advertises loaders "
                    f"{sorted(_version_loaders(version))!r}, but declared file "
                    f"{filename!r} contains {_loader_descriptor(platform['loader'])}"
                )
            elif loaders:
                findings.append(
                    f"{component}.{distribution.id}: provider metadata does not "
                    f"advertise {platform['loader']}; declared file {filename!r} "
                    f"contains descriptors for {sorted(loaders)!r}"
                )

    distributions_by_project: dict[str, list[tuple[str, Distribution]]] = defaultdict(
        list
    )
    for distribution in distributions:
        distributions_by_project[str(distribution.values["project-id"])].append(
            (component_by_distribution[distribution.identity], distribution)
        )
    for project_id, declared in sorted(distributions_by_project.items()):
        project = projects.get(project_id)
        if project is None:
            findings.append(f"Modrinth project {project_id} does not exist")
            continue
        declared_kinds = {distribution.kind for _, distribution in declared}
        advertised_kinds = _advertised_project_kinds(project, platform["loader"])
        if not advertised_kinds - declared_kinds:
            continue
        query = urllib.parse.urlencode(
            {
                "game_versions": json.dumps(
                    [platform["minecraft"]], separators=(",", ":")
                )
            }
        )
        raw_versions = _expect_list(
            fetch(f"{MODRINTH_API}/project/{project_id}/version?{query}"),
            f"versions for Modrinth project {project_id}",
        )
        candidates: dict[str, Mapping[str, Any]] = {}
        for version in raw_versions:
            if not isinstance(version, dict):
                continue
            for kind in advertised_kinds - declared_kinds:
                matches = _version_matches_kind(
                    version, project, kind, platform["loader"]
                )
                if not matches and kind == "mod":
                    matches = platform["loader"] in _loaders_from_archive(
                        _primary_file(version), download, archive_loaders
                    )
                if matches:
                    candidates.setdefault(kind, version)
        components = ", ".join(sorted({component for component, _ in declared}))
        for kind, version in sorted(candidates.items()):
            primary = _primary_file(version)
            findings.append(
                f"{components}: Modrinth project {project_id} also publishes a "
                f"Minecraft {platform['minecraft']} {kind}: {version.get('id')} "
                f"({primary.get('filename', 'unknown file')})"
            )

    return DependencyAudit(
        distributions=len(distributions),
        projects=len(project_ids),
        findings=tuple(findings),
    )


def _load_platform(path: Path) -> dict[str, str]:
    with path.open("rb") as source:
        raw = tomllib.load(source)
    result: dict[str, str] = {}
    for field in ("minecraft", "loader"):
        value = raw.get(field)
        if not isinstance(value, str) or not value:
            raise RuntimeError(f"{path}: {field!r} must be a non-empty string")
        result[field] = value
    return result


def _fetch_many(
    fetch: JsonFetcher, endpoint: str, identities: list[str]
) -> dict[str, Mapping[str, Any]]:
    result: dict[str, Mapping[str, Any]] = {}
    for offset in range(0, len(identities), 80):
        batch = identities[offset : offset + 80]
        query = urllib.parse.urlencode(
            {"ids": json.dumps(batch, separators=(",", ":"))}
        )
        values = _expect_list(
            fetch(f"{MODRINTH_API}/{endpoint}?{query}"),
            f"Modrinth {endpoint}",
        )
        for value in values:
            if isinstance(value, dict) and isinstance(value.get("id"), str):
                result[value["id"]] = value
    return result


def _expect_list(value: Any, description: str) -> list[Any]:
    if not isinstance(value, list):
        raise RuntimeError(f"{description} response must be a JSON array")
    return value


def _version_matches_kind(
    version: Mapping[str, Any],
    project: Mapping[str, Any],
    kind: str,
    loader: str,
) -> bool:
    loaders = _version_loaders(version)
    if kind == "mod":
        return loader in loaders
    if kind == "datapack":
        return "datapack" in loaders
    if kind == "resourcepack":
        return "resourcepack" in loaders or (
            project.get("project_type") == "resourcepack" and "minecraft" in loaders
        )
    return project.get("project_type") == "shader"


def _advertised_project_kinds(project: Mapping[str, Any], loader: str) -> set[str]:
    loaders = set(project.get("loaders", []))
    kinds: set[str] = set()
    # Project/version loader tags are useful discovery hints, but are not
    # authoritative. Some Modrinth-generated wrappers contain a NeoForge descriptor
    # even though the API only labels their version as Forge.
    if loader in loaders or project.get("project_type") == "mod":
        kinds.add("mod")
    if "datapack" in loaders:
        kinds.add("datapack")
    if "resourcepack" in loaders or project.get("project_type") == "resourcepack":
        kinds.add("resourcepack")
    if project.get("project_type") == "shader":
        kinds.add("shaderpack")
    return kinds


def _primary_file(version: Mapping[str, Any]) -> Mapping[str, Any]:
    files = [item for item in version.get("files", []) if isinstance(item, dict)]
    return next(
        (item for item in files if item.get("primary")), files[0] if files else {}
    )


def _declared_file(version: Mapping[str, Any], filename: str) -> Mapping[str, Any]:
    return next(
        (
            item
            for item in version.get("files", [])
            if isinstance(item, dict) and item.get("filename") == filename
        ),
        {},
    )


def _version_loaders(version: Mapping[str, Any]) -> set[str]:
    loaders = version.get("loaders", [])
    if not isinstance(loaders, list):
        return set()
    return {loader for loader in loaders if isinstance(loader, str)}


def _loader_descriptor(loader: str) -> str:
    return LOADER_DESCRIPTORS.get(loader, f"a {loader} loader descriptor")


def _loaders_from_archive(
    file: Mapping[str, Any],
    fetch: BytesFetcher,
    cache: dict[str, frozenset[str]],
) -> frozenset[str]:
    url = file.get("url")
    if not isinstance(url, str) or not url:
        return frozenset()
    cached = cache.get(url)
    if cached is not None:
        return cached
    try:
        with zipfile.ZipFile(io.BytesIO(fetch(url))) as archive:
            entries = set(archive.namelist())
    except zipfile.BadZipFile:
        result = frozenset()
    else:
        result = frozenset(
            loader
            for loader, descriptor in LOADER_DESCRIPTORS.items()
            if descriptor in entries
        )
    cache[url] = result
    return result


def _fetch_json(url: str) -> Any:
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "bertie-ci dependency audit"},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError) as error:
        raise RuntimeError(f"Modrinth request failed for {url}: {error}") from error


def _fetch_bytes(url: str) -> bytes:
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "bertie-ci dependency audit"},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.read()
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError) as error:
        raise RuntimeError(
            f"Modrinth file request failed for {url}: {error}"
        ) from error
