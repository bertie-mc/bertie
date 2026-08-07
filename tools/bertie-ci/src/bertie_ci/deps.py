from __future__ import annotations

import hashlib
import os
import re
from copy import deepcopy
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable, Mapping

import tomllib

LOCK_POLICY = "bertie-deps-lock-v1"
KINDS = ("mod", "datapack", "resourcepack", "shaderpack")
PROVIDERS = ("maven", "modrinth", "curseforge")
SIDES = ("both", "client", "server")
RELATIONSHIP_KINDS = ("optional-addon-for",)


@dataclass(frozen=True)
class Distribution:
    id: str
    provider: str
    kind: str
    side: str
    values: Mapping[str, Any]

    @property
    def identity(self) -> str:
        if self.provider == "maven":
            return f"maven:{self.values['module']}:{self.values['version']}"
        if self.provider == "modrinth":
            return f"modrinth:{self.values['project-id']}:{self.values['version-id']}"
        return f"curseforge:{self.values['project-id']}:{self.values['file-id']}"


@dataclass(frozen=True)
class ComponentRelationship:
    target: str
    kind: str


@dataclass(frozen=True)
class Component:
    id: str
    distributions: tuple[Distribution, ...]
    relationships: tuple[ComponentRelationship, ...]


@dataclass(frozen=True)
class Profile:
    id: str
    selection_priority: tuple[str, ...]
    representation_preference: tuple[str, ...]
    provider_preference: tuple[str, ...]
    native_packs: Mapping[str, str]


@dataclass(frozen=True)
class DependencyInputs:
    root: Path
    components: Mapping[str, Component]
    profiles: Mapping[str, Profile]


def load_inputs(root: Path) -> DependencyInputs:
    deps = root / "deps"
    component_paths = sorted((deps / "components").glob("*.toml"))
    profile_paths = sorted((deps / "profiles").glob("*.toml"))
    if not component_paths:
        raise RuntimeError(f"No component manifests found under {deps / 'components'}")
    if not profile_paths:
        raise RuntimeError(f"No dependency profiles found under {deps / 'profiles'}")
    components = {path.stem: _load_component(path) for path in component_paths}
    profiles = {path.stem: _load_profile(path) for path in profile_paths}
    inputs = DependencyInputs(root.resolve(), components, profiles)
    _validate_relationships(inputs)
    _validate_corrections(inputs)
    return inputs


def _load_component(path: Path) -> Component:
    data = _load_toml(path)
    relationships: list[ComponentRelationship] = []
    raw_relationships = data.get("relationships", {})
    if not isinstance(raw_relationships, dict):
        raise RuntimeError(f"{path}: 'relationships' must be a table")
    for target, raw in sorted(raw_relationships.items()):
        if not isinstance(raw, dict):
            raise RuntimeError(f"{path}: relationship {target!r} must be a table")
        relationships.append(
            ComponentRelationship(
                target=target,
                kind=_choice(
                    raw.get("kind"),
                    RELATIONSHIP_KINDS,
                    path,
                    f"relationships.{target}.kind",
                ),
            )
        )
    raw_distributions = data.get("distributions")
    if not isinstance(raw_distributions, dict) or not raw_distributions:
        raise RuntimeError(f"{path}: 'distributions' must be a non-empty table")
    distributions: list[Distribution] = []
    for distribution_id, raw in sorted(raw_distributions.items()):
        if not isinstance(raw, dict):
            raise RuntimeError(
                f"{path}: distribution {distribution_id!r} must be a table"
            )
        provider = _choice(raw.get("provider"), PROVIDERS, path, "provider")
        kind = _choice(raw.get("kind"), KINDS, path, "kind")
        side = _choice(raw.get("side", "both"), SIDES, path, "side")
        additional_kinds = _optional_choice_list(
            raw.get("additional-kinds", []),
            KINDS,
            path,
            "additional-kinds",
        )
        if kind in additional_kinds:
            raise RuntimeError(
                f"{path}: distribution {distribution_id!r} repeats its primary kind "
                "in additional-kinds"
            )
        if "mod" in additional_kinds:
            raise RuntimeError(
                f"{path}: distribution {distribution_id!r} cannot install a native "
                "pack as an additional mod"
            )
        _validate_distribution(path, distribution_id, provider, raw)
        distributions.append(
            Distribution(distribution_id, provider, kind, side, dict(raw))
        )
    return Component(path.stem, tuple(distributions), tuple(relationships))


def _validate_relationships(inputs: DependencyInputs) -> None:
    for component in inputs.components.values():
        for relationship in component.relationships:
            if relationship.target == component.id:
                raise RuntimeError(
                    f"Component {component.id!r} cannot relate to itself"
                )
            if relationship.target not in inputs.components:
                raise RuntimeError(
                    f"Component {component.id!r} relationship names unknown component "
                    f"{relationship.target!r}"
                )


def _validate_distribution(
    path: Path, distribution_id: str, provider: str, values: Mapping[str, Any]
) -> None:
    required: dict[str, tuple[str, ...]] = {
        "maven": ("module", "version", "filename"),
        "modrinth": ("project-id", "version-id", "filename"),
        "curseforge": ("slug", "project-id", "file-id", "filename"),
    }
    for field in required[provider]:
        value = values.get(field)
        if field in {"project-id", "file-id"} and provider == "curseforge":
            valid = isinstance(value, int) and not isinstance(value, bool) and value > 0
        else:
            valid = isinstance(value, str) and bool(value.strip())
        if not valid:
            raise RuntimeError(
                f"{path}: distribution {distribution_id!r} has invalid {field!r}"
            )


def _validate_corrections(inputs: DependencyInputs) -> None:
    for component in inputs.components.values():
        for distribution in component.distributions:
            corrections = distribution.values.get("dependency-corrections", {})
            if not isinstance(corrections, dict):
                raise RuntimeError(
                    f"Component {component.id!r} distribution {distribution.id!r}: "
                    "dependency-corrections must be a table"
                )
            for correction_id, correction in corrections.items():
                owner = (
                    f"Component {component.id!r} distribution {distribution.id!r} "
                    f"correction {correction_id!r}"
                )
                if not isinstance(correction, dict):
                    raise RuntimeError(f"{owner} must be a table")
                if correction.get("action") != "require":
                    raise RuntimeError(
                        f"{owner} action must be 'require'; other correction actions "
                        "are not implemented yet"
                    )
                if correction.get("applies-to") != distribution.identity:
                    raise RuntimeError(
                        f"{owner} applies-to must equal {distribution.identity!r}"
                    )
                for field in ("mod-id", "component", "version-range"):
                    value = correction.get(field)
                    if not isinstance(value, str) or not value.strip():
                        raise RuntimeError(f"{owner} field {field!r} must be non-empty")
                if correction["component"] not in inputs.components:
                    raise RuntimeError(
                        f"{owner} names unknown component {correction['component']!r}"
                    )
                if correction.get("side") not in SIDES:
                    raise RuntimeError(
                        f"{owner} side must be one of {', '.join(SIDES)}"
                    )


def _load_profile(path: Path) -> Profile:
    data = _load_toml(path)
    priority = _string_list(data.get("selection-priority"), path, "selection-priority")
    if sorted(priority) != ["provider", "representation"]:
        raise RuntimeError(
            f"{path}: selection-priority must contain provider and representation once"
        )
    representations = _choice_list(
        data.get("representation-preference"), KINDS, path, "representation-preference"
    )
    providers = _choice_list(
        data.get("provider-preference"), PROVIDERS, path, "provider-preference"
    )
    native_packs: dict[str, str] = {}
    raw_native = data.get("native-packs", {})
    if not isinstance(raw_native, dict):
        raise RuntimeError(f"{path}: native-packs must be a table")
    for kind, rule in sorted(raw_native.items()):
        if kind not in {"datapack", "resourcepack"} or not isinstance(rule, dict):
            raise RuntimeError(f"{path}: invalid native-packs entry {kind!r}")
        if rule.get("mode") != "loader":
            raise RuntimeError(f"{path}: native-packs.{kind}.mode must be 'loader'")
        native_packs[kind] = _nonempty_string(
            rule.get("component"), path, f"native-packs.{kind}.component"
        )
    return Profile(path.stem, priority, representations, providers, native_packs)


def select_distribution(
    component: Component, profile: Profile, *, kind: str | None = None
) -> Distribution:
    candidates = [
        distribution
        for distribution in component.distributions
        if kind is None or distribution.kind == kind
    ]
    if not candidates:
        suffix = "" if kind is None else f" with representation {kind!r}"
        raise RuntimeError(
            f"Component {component.id!r} has no distribution for profile "
            f"{profile.id!r}{suffix}"
        )
    representation_rank = {
        value: index for index, value in enumerate(profile.representation_preference)
    }
    provider_rank = {
        value: index for index, value in enumerate(profile.provider_preference)
    }

    def score(distribution: Distribution) -> tuple[int, ...]:
        ranks = {
            "representation": representation_rank.get(
                distribution.kind, len(representation_rank)
            ),
            "provider": provider_rank.get(distribution.provider, len(provider_rank)),
        }
        return tuple(ranks[item] for item in profile.selection_priority)

    selected_score = min(map(score, candidates))
    selected = [item for item in candidates if score(item) == selected_score]
    if len(selected) != 1:
        ids = ", ".join(item.id for item in selected)
        raise RuntimeError(
            f"Component {component.id!r} is ambiguous in profile {profile.id!r}: {ids}"
        )
    return selected[0]


def inputs_hash(inputs: DependencyInputs, profile: Profile) -> str:
    digest = hashlib.sha256()
    digest.update(LOCK_POLICY.encode())
    digest.update(b"\0")
    paths = [inputs.root / "deps" / "platform.toml"]
    paths.extend(sorted((inputs.root / "deps" / "components").glob("*.toml")))
    paths.append(inputs.root / "deps" / "profiles" / f"{profile.id}.toml")
    for path in paths:
        relative = path.relative_to(inputs.root).as_posix()
        digest.update(relative.encode())
        digest.update(b"\0")
        digest.update(path.read_bytes())
        digest.update(b"\0")
    return digest.hexdigest()


def check_locks(root: Path) -> tuple[str, ...]:
    inputs = load_inputs(root)
    errors: list[str] = []
    errors.extend(_validate_component_consumers(inputs))
    for profile_id, profile in sorted(inputs.profiles.items()):
        path = root / "deps" / "locks" / f"{profile_id}.lock.toml"
        if not path.is_file():
            errors.append(f"Missing lock for profile {profile_id!r}: {path}")
            continue
        lock = _load_toml(path)
        if lock.get("profile") != profile_id:
            errors.append(f"{path}: profile does not match {profile_id!r}")
        expected_hash = inputs_hash(inputs, profile)
        if lock.get("inputs-hash") != expected_hash:
            errors.append(f"{path}: stale inputs-hash; run 'bertie-ci deps-lock'")
        raw_components = lock.get("components")
        raw_artifacts = lock.get("artifacts")
        raw_relationships = lock.get("relationships")
        if (
            not isinstance(raw_components, dict)
            or not isinstance(raw_artifacts, dict)
            or not isinstance(raw_relationships, list)
        ):
            errors.append(
                f"{path}: relationships must be an array; components and artifacts "
                "must be tables"
            )
            continue
        expected_relationships = _locked_relationships(inputs)
        if raw_relationships != expected_relationships:
            errors.append(f"{path}: component relationships do not match the manifest")
        for component_id, component in sorted(inputs.components.items()):
            selected = raw_components.get(component_id)
            if not isinstance(selected, dict):
                errors.append(f"{path}: missing component selection {component_id!r}")
                continue
            expected = select_distribution(component, profile).identity
            if selected.get("any") != expected:
                errors.append(
                    f"{path}: {component_id!r} selects {selected.get('any')!r}, "
                    f"expected {expected!r}"
                )
            for constraint in ("any", "mod"):
                identity = selected.get(constraint)
                if identity is not None and identity not in raw_artifacts:
                    errors.append(
                        f"{path}: {component_id!r}.{constraint} references missing "
                        f"artifact {identity!r}"
                    )
        errors.extend(
            _validate_artifact_graph(
                path, raw_artifacts, inputs, profile, raw_components
            )
        )
        if profile_id == "development":
            errors.extend(_validate_owned_runtime_dependencies(inputs, raw_artifacts))
        errors.extend(
            _validate_locked_corrections(path, inputs, raw_components, raw_artifacts)
        )
    return tuple(errors)


def _validate_component_consumers(inputs: DependencyInputs) -> Iterable[str]:
    aliases = {
        _catalog_alias(component_id): component_id for component_id in inputs.components
    }
    referenced: set[str] = set()
    dependency_pattern = re.compile(r"\bdeps\.([A-Za-z][A-Za-z0-9]*)")
    build_files = [
        path
        for path in inputs.root.rglob("*.gradle.kts")
        if not any(
            part in {".git", ".gradle", "build"}
            for part in path.relative_to(inputs.root).parts
        )
    ]
    for path in build_files:
        for alias in dependency_pattern.findall(path.read_text(encoding="utf-8")):
            component_id = aliases.get(alias)
            if component_id is None:
                yield f"{path}: deps.{alias} does not name a component manifest"
            else:
                referenced.add(component_id)
    referenced.update(
        component_id
        for profile in inputs.profiles.values()
        for component_id in profile.native_packs.values()
    )
    referenced.update(
        correction["component"]
        for component in inputs.components.values()
        for distribution in component.distributions
        for correction in distribution.values.get("dependency-corrections", {}).values()
    )
    for component_id in sorted(inputs.components.keys() - referenced):
        yield f"Component {component_id!r} has no Gradle or profile-infrastructure consumer"


def _validate_owned_runtime_dependencies(
    inputs: DependencyInputs, artifacts: Mapping[str, Any]
) -> Iterable[str]:
    component_by_mod_id: dict[str, set[str]] = {}
    for raw in artifacts.values():
        if not isinstance(raw, dict) or not isinstance(raw.get("component"), str):
            continue
        for mod_id in raw.get("provides", []) + raw.get("bundled-provides", []):
            component_by_mod_id.setdefault(mod_id, set()).add(raw["component"])

    mods_directory = inputs.root / "mods"
    owned_mod_ids = {
        match.group(1)
        for properties in mods_directory.glob("*/mod.properties")
        if (
            match := re.search(
                r"(?m)^\s*mod_id\s*=\s*([^\s#]+)",
                properties.read_text(encoding="utf-8"),
            )
        )
    }
    runtime_configurations = ("runtimeOnly", "implementation", "api")
    for metadata in sorted(mods_directory.glob("*/src/main/**/neoforge.mods.toml")):
        project = metadata.relative_to(mods_directory).parts[0]
        build_file = mods_directory / project / "build.gradle.kts"
        build = build_file.read_text(encoding="utf-8")
        contents = metadata.read_text(encoding="utf-8")
        for block in re.split(r"(?=\[\[dependencies\.)", contents):
            if not block.startswith("[[dependencies."):
                continue
            mod_id_match = re.search(r'modId\s*=\s*"([^"]+)"', block)
            type_match = re.search(r'type\s*=\s*"([^"]+)"', block)
            if (
                mod_id_match is None
                or type_match is None
                or type_match.group(1) != "required"
            ):
                continue
            mod_id = mod_id_match.group(1)
            if mod_id in {"minecraft", "neoforge"} or mod_id in owned_mod_ids:
                continue
            components = component_by_mod_id.get(mod_id, set())
            if len(components) != 1:
                description = (
                    "no component" if not components else "multiple components"
                )
                yield (
                    f"{metadata}: required external mod ID {mod_id!r} has "
                    f"{description} in the development lock"
                )
                continue
            component = next(iter(components))
            alias = _catalog_alias(component)
            if not any(
                re.search(
                    rf"\b{configuration}\s*\(\s*(?:variantOf\s*\(\s*)?"
                    rf"deps\.{re.escape(alias)}\b",
                    build,
                )
                for configuration in runtime_configurations
            ):
                yield (
                    f"{build_file}: owned mod {project!r} requires {mod_id!r}; "
                    f"declare runtimeOnly(deps.{alias}) directly"
                )


def _catalog_alias(component_id: str) -> str:
    words = re.split("[-_]", component_id)
    return words[0] + "".join(word[:1].upper() + word[1:] for word in words[1:])


def _validate_artifact_graph(
    path: Path,
    artifacts: Mapping[str, Any],
    inputs: DependencyInputs,
    profile: Profile,
    selections: Mapping[str, Any],
) -> Iterable[str]:
    for identity, raw in sorted(artifacts.items()):
        if not isinstance(raw, dict):
            yield f"{path}: artifact {identity!r} must be a table"
            continue
        for field, choices in (("kind", KINDS), ("side", SIDES)):
            if raw.get(field) not in choices:
                yield f"{path}: artifact {identity!r} has invalid {field}"
        additional_kinds = raw.get("additional-kinds", [])
        if (
            not isinstance(additional_kinds, list)
            or not all(isinstance(kind, str) for kind in additional_kinds)
            or len(set(additional_kinds)) != len(additional_kinds)
            or any(kind not in KINDS or kind == "mod" for kind in additional_kinds)
            or raw.get("kind") in additional_kinds
        ):
            yield f"{path}: artifact {identity!r} has invalid additional-kinds"
        component = raw.get("component")
        if component is not None and component not in inputs.components:
            yield f"{path}: artifact {identity!r} names unknown component {component!r}"
        for edge in raw.get("required", []):
            if not isinstance(edge, dict) or edge.get("artifact") not in artifacts:
                yield f"{path}: artifact {identity!r} has an unresolved required edge"
        loader = profile.native_packs.get(raw.get("kind"))
        if loader is None:
            continue
        loader_selection = selections.get(loader)
        if not isinstance(loader_selection, dict):
            yield f"{path}: profile loader component {loader!r} has no selection"
            continue
        loader_identity = loader_selection.get("mod", loader_selection.get("any"))
        if not any(
            isinstance(edge, dict) and edge.get("artifact") == loader_identity
            for edge in raw.get("required", [])
        ):
            yield (
                f"{path}: native artifact {identity!r} is missing its "
                f"profile loader edge to {loader!r}"
            )


def _validate_locked_corrections(
    path: Path,
    inputs: DependencyInputs,
    selections: Mapping[str, Any],
    artifacts: Mapping[str, Any],
) -> Iterable[str]:
    for component in inputs.components.values():
        selected = selections.get(component.id)
        if not isinstance(selected, dict):
            continue
        selected_identities = {selected["any"], selected.get("mod")}
        for distribution in component.distributions:
            if distribution.identity not in selected_identities:
                continue
            source = artifacts.get(distribution.identity)
            if not isinstance(source, dict):
                continue
            for correction_id, correction in distribution.values.get(
                "dependency-corrections", {}
            ).items():
                target_selection = selections.get(correction["component"])
                if not isinstance(target_selection, dict):
                    continue
                target_identity = target_selection.get("mod")
                if target_identity is None:
                    yield (
                        f"{path}: correction {component.id}.{correction_id} target "
                        f"{correction['component']!r} has no mod selection"
                    )
                    continue
                expected_origin = (
                    f"correction:{component.id}:{distribution.id}:{correction_id}"
                )
                if not any(
                    isinstance(edge, dict)
                    and edge.get("artifact") == target_identity
                    and edge.get("mod-id") == correction.get("mod-id")
                    and edge.get("origin") == expected_origin
                    for edge in source.get("required", [])
                ):
                    yield (
                        f"{path}: correction {component.id}.{correction_id} is not "
                        f"present on locked artifact {distribution.identity!r}"
                    )


def refresh_locks(root: Path) -> None:
    """Refresh selections while reusing verified metadata for unchanged artifacts.

    Provider/archive discovery is intentionally incremental: unchanged immutable
    distributions retain their locked evidence. An equivalent distribution of the
    same component and representation may reuse archive metadata from an existing
    lock; its provider coordinates still come from the component manifest.
    """
    inputs = load_inputs(root)
    lock_dir = root / "deps" / "locks"
    existing = {
        path.stem.removesuffix(".lock"): _load_toml(path)
        for path in sorted(lock_dir.glob("*.lock.toml"))
    }
    known_artifacts: dict[str, Any] = {}
    for lock in existing.values():
        artifacts = lock.get("artifacts", {})
        if isinstance(artifacts, dict):
            known_artifacts.update(artifacts)

    rendered: dict[Path, str] = {}
    for profile_id, profile in sorted(inputs.profiles.items()):
        previous = existing.get(profile_id, {})
        previous_artifacts = previous.get("artifacts", {})
        artifacts = (
            deepcopy(previous_artifacts) if isinstance(previous_artifacts, dict) else {}
        )
        selections: dict[str, dict[str, str]] = {}
        for component_id, component in sorted(inputs.components.items()):
            any_distribution = select_distribution(component, profile)
            selection = {"any": any_distribution.identity}
            mod_candidates = [
                item for item in component.distributions if item.kind == "mod"
            ]
            if mod_candidates:
                selection["mod"] = select_distribution(
                    component, profile, kind="mod"
                ).identity
            selections[component_id] = selection
            for distribution in (any_distribution,) + tuple(
                item
                for item in component.distributions
                if item.identity == selection.get("mod")
            ):
                metadata = artifacts.get(distribution.identity)
                if not isinstance(metadata, dict):
                    metadata = known_artifacts.get(distribution.identity)
                    if not isinstance(metadata, dict):
                        metadata = _equivalent_locked_metadata(
                            component, distribution, known_artifacts
                        )
                artifacts[distribution.identity] = _with_distribution_source(
                    metadata, distribution
                )
                artifacts[distribution.identity]["component"] = component_id
        _populate_locked_dependencies(artifacts, known_artifacts)
        _select_component_dependency_distributions(
            artifacts, known_artifacts, selections, inputs
        )
        for artifact in artifacts.values():
            if not isinstance(artifact, dict):
                continue
            if artifact.get("component") not in inputs.components:
                artifact.pop("component", None)
            for field in ("required", "optional"):
                artifact[field] = [
                    edge
                    for edge in artifact.get(field, [])
                    if not (
                        isinstance(edge, dict)
                        and str(edge.get("origin", "")).startswith(
                            ("profile:", "correction:")
                        )
                    )
                ]
        _apply_corrections(inputs, profile, selections, artifacts)
        reachable = _reachable_artifacts(selections, artifacts, profile)
        _add_profile_dependencies(selections, artifacts, profile, reachable)
        retained = {key: artifacts[key] for key in sorted(reachable)}
        rendered[lock_dir / f"{profile_id}.lock.toml"] = _render_lock(
            profile_id,
            inputs_hash(inputs, profile),
            _locked_relationships(inputs),
            selections,
            retained,
        )

    lock_dir.mkdir(parents=True, exist_ok=True)
    temporary: list[tuple[Path, Path]] = []
    try:
        for path, contents in rendered.items():
            temp = path.with_name(f".{path.name}.{os.getpid()}.tmp")
            temp.write_text(contents, encoding="utf-8", newline="\n")
            temporary.append((temp, path))
        for temp, path in temporary:
            temp.replace(path)
    finally:
        for temp, _ in temporary:
            temp.unlink(missing_ok=True)


def _equivalent_locked_metadata(
    component: Component,
    distribution: Distribution,
    known_artifacts: Mapping[str, Any],
) -> Mapping[str, Any]:
    for candidate in component.distributions:
        known = known_artifacts.get(candidate.identity)
        if candidate.kind == distribution.kind and isinstance(known, dict):
            return known
    raise RuntimeError(
        f"No locked archive metadata for distribution {distribution.identity!r} or "
        f"an equivalent {distribution.kind} distribution of component {component.id!r}; "
        "dependency discovery for new artifacts is not implemented yet"
    )


def _with_distribution_source(
    metadata: Mapping[str, Any], distribution: Distribution
) -> dict[str, Any]:
    result = deepcopy(metadata)
    for field in (
        "group",
        "module",
        "version",
        "project-id",
        "version-id",
        "slug",
        "file-id",
    ):
        result.pop(field, None)
    result.update(
        {
            "provider": distribution.provider,
            "kind": distribution.kind,
            "side": distribution.side,
            "filename": distribution.values["filename"],
        }
    )
    result.pop("additional-kinds", None)
    additional_kinds = distribution.values.get("additional-kinds", [])
    if additional_kinds:
        result["additional-kinds"] = list(additional_kinds)
    if distribution.provider == "maven":
        group, module = str(distribution.values["module"]).split(":", 1)
        result.update(
            {
                "group": group,
                "module": module,
                "version": distribution.values["version"],
            }
        )
    elif distribution.provider == "modrinth":
        result.update(
            {
                "group": "maven.modrinth",
                "module": distribution.values["project-id"],
                "version": distribution.values["version-id"],
                "project-id": distribution.values["project-id"],
                "version-id": distribution.values["version-id"],
            }
        )
    else:
        result.update(
            {
                "group": "curse.maven",
                "module": (
                    f"{distribution.values['slug']}-{distribution.values['project-id']}"
                ),
                "version": str(distribution.values["file-id"]),
                "slug": distribution.values["slug"],
                "project-id": distribution.values["project-id"],
                "file-id": distribution.values["file-id"],
            }
        )
    return result


def _populate_locked_dependencies(
    artifacts: dict[str, Any], known_artifacts: Mapping[str, Any]
) -> None:
    pending = list(artifacts)
    while pending:
        identity = pending.pop()
        raw = artifacts.get(identity)
        if not isinstance(raw, dict):
            continue
        for edge in raw.get("required", []):
            target = edge.get("artifact") if isinstance(edge, dict) else None
            if not isinstance(target, str) or target in artifacts:
                continue
            known = known_artifacts.get(target)
            if not isinstance(known, dict):
                raise RuntimeError(
                    f"Locked archive metadata for {identity!r} requires unknown "
                    f"artifact {target!r}"
                )
            artifacts[target] = deepcopy(known)
            pending.append(target)


def _select_component_dependency_distributions(
    artifacts: Mapping[str, Any],
    known_artifacts: Mapping[str, Any],
    selections: Mapping[str, Mapping[str, str]],
    inputs: DependencyInputs,
) -> None:
    component_by_identity = {
        distribution.identity: component.id
        for component in inputs.components.values()
        for distribution in component.distributions
    }
    component_by_identity.update(
        {
            identity: raw["component"]
            for identity, raw in known_artifacts.items()
            if isinstance(raw, dict) and isinstance(raw.get("component"), str)
        }
    )
    component_by_identity.update(
        {
            identity: raw["component"]
            for identity, raw in artifacts.items()
            if isinstance(raw, dict) and isinstance(raw.get("component"), str)
        }
    )
    component_by_provider_project: dict[str, str] = {}
    for identity, component in component_by_identity.items():
        provider_project = _provider_project_identity(identity)
        if provider_project is not None:
            component_by_provider_project[provider_project] = component
    for raw in artifacts.values():
        if not isinstance(raw, dict):
            continue
        for field in ("required", "optional", "incompatible", "integrations"):
            for edge in raw.get(field, []):
                if not isinstance(edge, dict):
                    continue
                target = edge.get("artifact")
                component = component_by_identity.get(target)
                if component is None:
                    component = component_by_provider_project.get(edge.get("missing"))
                selection = selections.get(component) if component is not None else None
                if selection is not None:
                    edge["artifact"] = selection.get("mod", selection["any"])
                    edge.pop("missing", None)


def _provider_project_identity(identity: str) -> str | None:
    parts = identity.split(":")
    if len(parts) == 3 and parts[0] in {"modrinth", "curseforge"}:
        return ":".join(parts[:2])
    return None


def _reachable_artifacts(
    selections: Mapping[str, Mapping[str, str]],
    artifacts: Mapping[str, Any],
    profile: Profile,
) -> set[str]:
    pending = [identity for value in selections.values() for identity in value.values()]
    reachable: set[str] = set()
    while pending:
        identity = pending.pop()
        if identity in reachable:
            continue
        raw = artifacts.get(identity)
        if not isinstance(raw, dict):
            raise RuntimeError(f"Lock metadata is missing artifact {identity!r}")
        reachable.add(identity)
        pending.extend(
            edge["artifact"]
            for edge in raw.get("required", [])
            if isinstance(edge, dict) and isinstance(edge.get("artifact"), str)
        )
        loader = profile.native_packs.get(raw.get("kind"))
        if loader is not None:
            loader_selection = selections.get(loader)
            if loader_selection is None:
                raise RuntimeError(
                    f"Profile {profile.id!r} names unknown loader component {loader!r}"
                )
            pending.append(loader_selection.get("mod", loader_selection["any"]))
    return reachable


def _add_profile_dependencies(
    selections: Mapping[str, Mapping[str, str]],
    artifacts: Mapping[str, Any],
    profile: Profile,
    reachable: set[str],
) -> None:
    for identity in sorted(reachable):
        artifact = artifacts[identity]
        loader = profile.native_packs.get(artifact.get("kind"))
        if loader is None:
            continue
        loader_selection = selections[loader]
        loader_identity = loader_selection.get("mod", loader_selection["any"])
        required = artifact.setdefault("required", [])
        if any(
            isinstance(edge, dict) and edge.get("artifact") == loader_identity
            for edge in required
        ):
            continue
        required.append(
            {
                "artifact": loader_identity,
                "mod-id": loader,
                "side": "both",
                "origin": f"profile:{profile.id}:native-packs.{artifact['kind']}",
            }
        )


def _apply_corrections(
    inputs: DependencyInputs,
    profile: Profile,
    selections: Mapping[str, Mapping[str, str]],
    artifacts: Mapping[str, Any],
) -> None:
    for component in inputs.components.values():
        selected_identities = set(selections[component.id].values())
        for distribution in component.distributions:
            if distribution.identity not in selected_identities:
                continue
            source = artifacts[distribution.identity]
            for correction_id, correction in distribution.values.get(
                "dependency-corrections", {}
            ).items():
                target_selection = selections[correction["component"]]
                target_identity = target_selection.get("mod")
                if target_identity is None:
                    raise RuntimeError(
                        f"Correction {component.id}.{correction_id} requires component "
                        f"{correction['component']!r}, which has no mod distribution in "
                        f"profile {profile.id!r}"
                    )
                target = artifacts[target_identity]
                mod_id = correction["mod-id"]
                provided_mod_ids = target.get("provides", []) + target.get(
                    "bundled-provides", []
                )
                if mod_id not in provided_mod_ids:
                    raise RuntimeError(
                        f"Correction {component.id}.{correction_id} target "
                        f"{target_identity!r} does not provide mod ID {mod_id!r}"
                    )
                origin = f"correction:{component.id}:{distribution.id}:{correction_id}"
                source["required"] = [
                    edge
                    for edge in source.get("required", [])
                    if not (isinstance(edge, dict) and edge.get("origin") == origin)
                ]
                edge = {
                    "artifact": target_identity,
                    "mod-id": mod_id,
                    "version-range": correction["version-range"],
                    "side": correction["side"],
                    "origin": origin,
                }
                source["required"].append(edge)


def _locked_relationships(inputs: DependencyInputs) -> list[dict[str, str]]:
    return [
        {
            "source": component.id,
            "target": relationship.target,
            "kind": relationship.kind,
        }
        for component in sorted(inputs.components.values(), key=lambda item: item.id)
        for relationship in component.relationships
    ]


def _render_lock(
    profile_id: str,
    digest: str,
    relationships: list[dict[str, str]],
    selections: Mapping[str, Mapping[str, str]],
    artifacts: Mapping[str, Any],
) -> str:
    lines = [
        f"profile = {_toml_string(profile_id)}",
        f"inputs-hash = {_toml_string(digest)}",
        f"relationships = {_toml_value(relationships)}",
    ]
    for component_id, selection in sorted(selections.items()):
        lines.append("")
        lines.append(f"[components.{_toml_key(component_id)}]")
        for constraint, identity in selection.items():
            lines.append(f"{constraint} = {_toml_string(identity)}")
    for identity, artifact in sorted(artifacts.items()):
        lines.append("")
        lines.append(f"[artifacts.{_toml_key(identity)}]")
        scalar_order = (
            "name",
            "component",
            "provider",
            "kind",
            "side",
            "group",
            "module",
            "version",
            "filename",
            "project-id",
            "version-id",
            "slug",
            "file-id",
        )
        for field in scalar_order:
            if field in artifact:
                lines.append(f"{field} = {_toml_value(artifact[field])}")
        if artifact.get("additional-kinds"):
            lines.append(
                f"additional-kinds = {_toml_value(artifact['additional-kinds'])}"
            )
        for field in ("provides", "bundled-provides"):
            lines.append(f"{field} = {_toml_value(artifact.get(field, []))}")
        for field in (
            "required",
            "optional",
            "incompatible",
            "bundled",
            "integrations",
        ):
            values = artifact.get(field, [])
            lines.append(f"{field} = {_toml_value(values)}")
    lines.append("")
    return "\n".join(lines)


def _toml_value(value: Any) -> str:
    if isinstance(value, str):
        return _toml_string(value)
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, int):
        return str(value)
    if isinstance(value, list):
        return "[" + ", ".join(_toml_value(item) for item in value) + "]"
    if isinstance(value, dict):
        return (
            "{ "
            + ", ".join(
                f"{_toml_key(key)} = {_toml_value(item)}" for key, item in value.items()
            )
            + " }"
        )
    raise TypeError(f"Cannot encode TOML value {value!r}")


def _toml_string(value: str) -> str:
    escaped = (
        value.replace("\\", "\\\\")
        .replace('"', '\\"')
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    )
    return f'"{escaped}"'


def _toml_key(value: str) -> str:
    return _toml_string(value)


def _load_toml(path: Path) -> dict[str, Any]:
    try:
        with path.open("rb") as source:
            return tomllib.load(source)
    except tomllib.TOMLDecodeError as error:
        raise RuntimeError(f"Invalid TOML in {path}: {error}") from error


def _nonempty_string(value: Any, path: Path, field: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise RuntimeError(f"{path}: {field!r} must be a non-empty string")
    return value


def _string_list(value: Any, path: Path, field: str) -> tuple[str, ...]:
    if (
        not isinstance(value, list)
        or not value
        or not all(isinstance(item, str) for item in value)
    ):
        raise RuntimeError(f"{path}: {field!r} must be a non-empty string array")
    return tuple(value)


def _choice(value: Any, choices: tuple[str, ...], path: Path, field: str) -> str:
    if value not in choices:
        raise RuntimeError(f"{path}: {field!r} must be one of {', '.join(choices)}")
    return value


def _choice_list(
    value: Any, choices: tuple[str, ...], path: Path, field: str
) -> tuple[str, ...]:
    result = _string_list(value, path, field)
    if len(set(result)) != len(result) or any(item not in choices for item in result):
        raise RuntimeError(
            f"{path}: {field!r} must contain unique values from {', '.join(choices)}"
        )
    return result


def _optional_choice_list(
    value: Any, choices: tuple[str, ...], path: Path, field: str
) -> tuple[str, ...]:
    if not isinstance(value, list) or not all(isinstance(item, str) for item in value):
        raise RuntimeError(f"{path}: {field!r} must be a string array")
    result = tuple(value)
    if len(set(result)) != len(result) or any(item not in choices for item in result):
        raise RuntimeError(
            f"{path}: {field!r} must contain unique values from {', '.join(choices)}"
        )
    return result
