from __future__ import annotations

import fnmatch
import json
import re
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Literal

import tomllib

WORKSPACE_FORMAT = "bertie-ci.workspace.v1"
COMPONENT_FORMAT = "bertie-ci.component.v1"
WORKSPACE_FILE = "bertie-ci.toml"
COMPONENT_FILE = "bertie-ci.toml"
SUBJECT_PATTERN = re.compile(r"[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")
VERSION_PATTERN = re.compile(r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)")
TAG_PATTERN = re.compile(
    r"(?P<subject>[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)/v(?P<version>"
    + VERSION_PATTERN.pattern
    + r")"
)

ComponentKind = Literal["neoforge-mod", "pack", "tool"]


def _document(path: Path) -> dict[str, Any]:
    try:
        data = tomllib.loads(path.read_text(encoding="utf-8"))
    except (OSError, tomllib.TOMLDecodeError) as error:
        raise RuntimeError(f"Cannot read {path}: {error}") from error
    if not isinstance(data, dict):
        raise RuntimeError(f"Invalid TOML document: {path}")
    return data


def _strings(value: object, label: str) -> tuple[str, ...]:
    if not isinstance(value, list) or not all(
        isinstance(item, str) and item for item in value
    ):
        raise RuntimeError(f"{label} must be an array of non-empty strings")
    return tuple(value)


def _reject_unknown(value: dict[str, Any], allowed: set[str], label: str) -> None:
    unknown = sorted(set(value) - allowed)
    if unknown:
        raise RuntimeError(f"Unknown {label} key(s): {', '.join(unknown)}")


def _relative(root: Path, value: object, label: str) -> Path:
    if not isinstance(value, str) or not value:
        raise RuntimeError(f"{label} must be a non-empty relative path")
    relative = Path(value)
    if relative.is_absolute() or ".." in relative.parts:
        raise RuntimeError(f"{label} must not escape its component: {value}")
    resolved = (root / relative).resolve()
    try:
        resolved.relative_to(root.resolve())
    except ValueError as error:
        raise RuntimeError(f"{label} escapes its component: {value}") from error
    return resolved


@dataclass(frozen=True)
class VersionSource:
    path: Path
    key: str

    def read(self) -> str:
        if not self.path.is_file():
            raise RuntimeError(f"Version source not found: {self.path}")
        if self.path.suffix == ".toml":
            value: object = _document(self.path)
            for segment in self.key.split("."):
                if not isinstance(value, dict) or segment not in value:
                    raise RuntimeError(
                        f"Version key {self.key!r} not found in {self.path}"
                    )
                value = value[segment]
        else:
            values: dict[str, str] = {}
            for raw_line in self.path.read_text(encoding="utf-8").splitlines():
                line = raw_line.strip()
                if not line or line.startswith(("#", "!")):
                    continue
                separator = next(
                    (index for index, char in enumerate(line) if char in "=:"), -1
                )
                if separator < 0:
                    continue
                values[line[:separator].strip()] = line[separator + 1 :].strip()
            value = values.get(self.key)
        if not isinstance(value, str) or not VERSION_PATTERN.fullmatch(value):
            raise RuntimeError(
                f"Version {value!r} from {self.path} is not an exact X.Y.Z version"
            )
        return value


@dataclass(frozen=True)
class Suite:
    id: str
    runner: str
    automatic: bool
    fixtures: tuple[str, ...] = ()
    instance_files: Path | None = None
    build_client_test_mod: bool = False
    required_logs: tuple[str, ...] = ()
    minimum_game_tests: int = 0
    command_test: Path | None = None
    timeout: int | None = None
    max_memory: str | None = None

    @classmethod
    def parse(cls, component_root: Path, value: object) -> Suite:
        if not isinstance(value, dict):
            raise RuntimeError("Each [[suite]] entry must be a TOML table")
        _reject_unknown(
            value,
            {
                "id",
                "runner",
                "automatic",
                "fixtures",
                "instance-files",
                "build-client-test-mod",
                "require-log",
                "minimum-game-tests",
                "command-test",
                "timeout",
                "max-memory",
            },
            "suite",
        )
        suite_id = value.get("id")
        runner = value.get("runner")
        if not isinstance(suite_id, str) or not SUBJECT_PATTERN.fullmatch(suite_id):
            raise RuntimeError(f"Invalid suite id: {suite_id!r}")
        allowed_runners = {"unit", "gametest", "client", "server", "validate"}
        if runner not in allowed_runners:
            raise RuntimeError(f"Invalid runner for suite {suite_id!r}: {runner!r}")
        automatic = value.get("automatic", True)
        if not isinstance(automatic, bool):
            raise RuntimeError(f"Suite {suite_id!r} automatic must be a boolean")
        fixtures = value.get("fixtures", [])
        parsed_fixtures = _strings(fixtures, f"Suite {suite_id!r} fixtures")
        instance_files_value = value.get("instance-files")
        instance_files = (
            _relative(
                component_root,
                instance_files_value,
                f"Suite {suite_id!r} instance-files",
            )
            if instance_files_value is not None
            else None
        )
        build_client_test_mod = value.get("build-client-test-mod", False)
        if not isinstance(build_client_test_mod, bool):
            raise RuntimeError(
                f"Suite {suite_id!r} build-client-test-mod must be a boolean"
            )
        if build_client_test_mod and runner != "client":
            raise RuntimeError(
                f"Suite {suite_id!r} can build a client test mod only for a client runner"
            )
        required_logs = _strings(
            value.get("require-log", []), f"Suite {suite_id!r} require-log"
        )
        minimum = value.get("minimum-game-tests", 0)
        if not isinstance(minimum, int) or isinstance(minimum, bool) or minimum < 0:
            raise RuntimeError(
                f"Suite {suite_id!r} minimum-game-tests must be non-negative"
            )
        timeout = value.get("timeout")
        if timeout is not None and (
            not isinstance(timeout, int) or isinstance(timeout, bool) or timeout <= 0
        ):
            raise RuntimeError(f"Suite {suite_id!r} timeout must be positive")
        max_memory = value.get("max-memory")
        if max_memory is not None and (
            not isinstance(max_memory, str)
            or re.fullmatch(r"[1-9][0-9]*[mMgG]", max_memory) is None
        ):
            raise RuntimeError(f"Suite {suite_id!r} max-memory is invalid")
        command = value.get("command-test")
        command_test = (
            _relative(component_root, command, f"Suite {suite_id!r} command-test")
            if command is not None
            else None
        )
        if parsed_fixtures and runner not in ("client", "server"):
            raise RuntimeError(
                f"Suite {suite_id!r} fixtures require a client or server runner"
            )
        if instance_files is not None and runner not in ("client", "server"):
            raise RuntimeError(
                f"Suite {suite_id!r} instance-files require a client or server runner"
            )
        if required_logs and runner not in ("client", "server"):
            raise RuntimeError(
                f"Suite {suite_id!r} require-log needs a client or server runner"
            )
        if minimum and runner != "client":
            raise RuntimeError(
                f"Suite {suite_id!r} minimum-game-tests requires a client runner"
            )
        if max_memory is not None and runner not in ("client", "server"):
            raise RuntimeError(
                f"Suite {suite_id!r} max-memory requires a client or server runner"
            )
        if timeout is not None and runner not in ("gametest", "client", "server"):
            raise RuntimeError(
                f"Suite {suite_id!r} timeout is unsupported for runner {runner!r}"
            )
        if command_test is not None and runner != "server":
            raise RuntimeError(
                f"Suite {suite_id!r} command-test requires a server runner"
            )
        if runner == "server" and command_test is None:
            raise RuntimeError(
                f"Server suite {suite_id!r} requires a project-owned command-test"
            )
        return cls(
            id=suite_id,
            runner=runner,
            automatic=automatic,
            fixtures=parsed_fixtures,
            instance_files=instance_files,
            build_client_test_mod=build_client_test_mod,
            required_logs=required_logs,
            minimum_game_tests=minimum,
            command_test=command_test,
            timeout=timeout,
            max_memory=max_memory.upper() if max_memory else None,
        )


@dataclass(frozen=True)
class Component:
    subject: str
    kind: ComponentKind
    path: Path
    relative_path: Path
    version_source: VersionSource
    gradle_project: str | None
    mod_id: str | None
    pack_metafile: Path | None
    dependencies: tuple[str, ...]
    suites: tuple[Suite, ...]

    def version(self) -> str:
        return self.version_source.read()

    def matrix_entry(self, suite: Suite | None = None) -> dict[str, object]:
        entry: dict[str, object] = {
            "subject": self.subject,
            "project": self.relative_path.as_posix(),
        }
        if self.gradle_project is not None:
            entry["gradle_project"] = self.gradle_project
        if suite is None:
            return entry
        entry["suite"] = suite.id
        if suite.runner == "gametest":
            entry["timeout"] = suite.timeout or 900
        elif suite.runner in ("client", "server"):
            entry.update(
                {
                    "fixture": ",".join(suite.fixtures),
                    "require_log": list(suite.required_logs),
                    "timeout": suite.timeout
                    or (1500 if suite.runner == "client" else 900),
                    "max_memory": suite.max_memory
                    or ("4G" if suite.runner == "client" else "3G"),
                }
            )
            if suite.instance_files is not None:
                entry["instance_files"] = suite.instance_files.relative_to(
                    self.path
                ).as_posix()
            if suite.runner == "client":
                entry["build_client_test_mod"] = suite.build_client_test_mod
                entry["minimum_game_tests"] = suite.minimum_game_tests
            if suite.command_test is not None:
                entry["command_test"] = suite.command_test.relative_to(
                    self.path
                ).as_posix()
        return entry


@dataclass(frozen=True)
class ReleasePlan:
    tag: str
    subject: str
    version: str
    kind: ComponentKind
    project: str

    def to_json(self) -> str:
        return json.dumps(
            {
                "tag": self.tag,
                "subject": self.subject,
                "version": self.version,
                "kind": self.kind,
                "project": self.project,
            },
            sort_keys=True,
        )


class Workspace:
    def __init__(
        self,
        root: Path,
        components: dict[str, Component],
        shared_mod_paths: tuple[str, ...],
        shared_all_paths: tuple[str, ...],
        ignored_paths: tuple[str, ...],
    ) -> None:
        self.root = root
        self.components = components
        self.shared_mod_paths = shared_mod_paths
        self.shared_all_paths = shared_all_paths
        self.ignored_paths = ignored_paths

    @classmethod
    def find(cls, start: Path) -> Workspace:
        candidate = start.resolve(strict=True)
        if candidate.is_file():
            candidate = candidate.parent
        standalone: tuple[Path, Path] | None = None
        for root in (candidate, *candidate.parents):
            config = root / WORKSPACE_FILE
            if config.is_file():
                data = _document(config)
                if data.get("format") == WORKSPACE_FORMAT:
                    return cls.load(root)
                if data.get("format") == COMPONENT_FORMAT and standalone is None:
                    standalone = (root, config)
        if standalone is not None:
            root, config = standalone
            component = cls._load_component(root, config)
            return cls(root, {component.subject: component}, (), (), ())
        raise RuntimeError(
            f"No {WORKSPACE_FORMAT} descriptor found from {start.resolve()}"
        )

    @classmethod
    def load(cls, root: Path) -> Workspace:
        root = root.resolve(strict=True)
        config = root / WORKSPACE_FILE
        data = _document(config)
        _reject_unknown(data, {"format", "workspace"}, "workspace descriptor")
        if data.get("format") != WORKSPACE_FORMAT:
            raise RuntimeError(f"Unsupported workspace descriptor format in {config}")
        workspace = data.get("workspace")
        if not isinstance(workspace, dict):
            raise RuntimeError(f"Missing [workspace] table in {config}")
        _reject_unknown(
            workspace,
            {
                "component-descriptors",
                "shared-mod-paths",
                "shared-all-paths",
                "ignored-paths",
            },
            "workspace",
        )
        patterns = _strings(
            workspace.get("component-descriptors"),
            "workspace.component-descriptors",
        )
        descriptor_paths = sorted(
            {
                path
                for pattern in patterns
                for path in root.glob(pattern)
                if path.is_file() and path.resolve() != config.resolve()
            }
        )
        components: dict[str, Component] = {}
        for path in descriptor_paths:
            component = cls._load_component(root, path)
            if component.subject in components:
                raise RuntimeError(f"Duplicate component subject: {component.subject}")
            components[component.subject] = component
        if not components:
            raise RuntimeError(f"Workspace has no component descriptors: {config}")
        for component in components.values():
            unknown = sorted(set(component.dependencies) - components.keys())
            if unknown:
                raise RuntimeError(
                    f"{component.subject} has unknown dependencies: {', '.join(unknown)}"
                )
        return cls(
            root=root,
            components=components,
            shared_mod_paths=_strings(
                workspace.get("shared-mod-paths", []),
                "workspace.shared-mod-paths",
            ),
            shared_all_paths=_strings(
                workspace.get("shared-all-paths", []),
                "workspace.shared-all-paths",
            ),
            ignored_paths=_strings(
                workspace.get("ignored-paths", []), "workspace.ignored-paths"
            ),
        )

    @staticmethod
    def _load_component(root: Path, descriptor: Path) -> Component:
        data = _document(descriptor)
        _reject_unknown(
            data,
            {
                "format",
                "subject",
                "kind",
                "gradle-project",
                "mod-id",
                "pack-metafile",
                "depends-on",
                "version",
                "suite",
            },
            "component descriptor",
        )
        if data.get("format") != COMPONENT_FORMAT:
            raise RuntimeError(
                f"Unsupported component descriptor format in {descriptor}"
            )
        subject = data.get("subject")
        kind = data.get("kind")
        if not isinstance(subject, str) or not SUBJECT_PATTERN.fullmatch(subject):
            raise RuntimeError(
                f"Invalid component subject in {descriptor}: {subject!r}"
            )
        if kind not in ("neoforge-mod", "pack", "tool"):
            raise RuntimeError(f"Invalid component kind in {descriptor}: {kind!r}")
        component_root = descriptor.parent.resolve()
        try:
            relative_path = component_root.relative_to(root)
        except ValueError as error:
            raise RuntimeError(
                f"Component descriptor escapes workspace: {descriptor}"
            ) from error
        version = data.get("version")
        if not isinstance(version, dict):
            raise RuntimeError(f"Missing [version] in {descriptor}")
        _reject_unknown(version, {"file", "key"}, "version")
        version_path = _relative(component_root, version.get("file"), "version.file")
        version_key = version.get("key")
        if not isinstance(version_key, str) or not version_key:
            raise RuntimeError(
                f"version.key must be a non-empty string in {descriptor}"
            )
        gradle_project = data.get("gradle-project")
        if gradle_project is not None and (
            not isinstance(gradle_project, str) or not gradle_project.startswith(":")
        ):
            raise RuntimeError(f"Invalid gradle-project in {descriptor}")
        if kind == "neoforge-mod" and gradle_project is None:
            raise RuntimeError(f"NeoForge component lacks gradle-project: {descriptor}")
        mod_id = data.get("mod-id")
        if mod_id is not None and (not isinstance(mod_id, str) or not mod_id):
            raise RuntimeError(f"Invalid mod-id in {descriptor}")
        if kind == "neoforge-mod" and mod_id is None:
            raise RuntimeError(f"NeoForge component lacks mod-id: {descriptor}")
        if kind == "neoforge-mod" and relative_path.parent == Path("mods"):
            expected_mod_id = relative_path.name.replace("-", "")
            if mod_id != expected_mod_id:
                raise RuntimeError(
                    "Owned mod id must equal its directory name without hyphens: "
                    f"{mod_id!r} != {expected_mod_id!r} in {descriptor}"
                )
        pack_metafile_value = data.get("pack-metafile")
        pack_metafile = (
            Path(pack_metafile_value)
            if isinstance(pack_metafile_value, str) and pack_metafile_value
            else None
        )
        if pack_metafile is not None and (
            pack_metafile.is_absolute() or ".." in pack_metafile.parts
        ):
            raise RuntimeError(f"Invalid pack-metafile in {descriptor}")
        dependencies = _strings(data.get("depends-on", []), "depends-on")
        suites_data = data.get("suite", [])
        if not isinstance(suites_data, list):
            raise RuntimeError(f"[[suite]] entries must be an array in {descriptor}")
        suites = tuple(Suite.parse(component_root, value) for value in suites_data)
        allowed_runners = {
            "neoforge-mod": {"unit", "gametest", "client", "server"},
            "pack": {"validate", "client", "server"},
            "tool": set(),
        }
        incompatible = sorted(
            suite.id for suite in suites if suite.runner not in allowed_runners[kind]
        )
        if incompatible:
            raise RuntimeError(
                f"Component kind {kind!r} cannot run suite(s): {', '.join(incompatible)}"
            )
        if kind != "neoforge-mod" and any(
            suite.instance_files is not None for suite in suites
        ):
            raise RuntimeError(
                "instance-files is supported only for NeoForge mod suites"
            )
        suite_ids = [suite.id for suite in suites]
        if len(set(suite_ids)) != len(suite_ids):
            raise RuntimeError(f"Duplicate suite id in {descriptor}")
        for runner in ("unit", "gametest", "validate"):
            matching = [suite.id for suite in suites if suite.runner == runner]
            if len(matching) > 1:
                raise RuntimeError(
                    f"Component has multiple {runner!r} suites in {descriptor}: "
                    f"{', '.join(matching)}"
                )
        return Component(
            subject=subject,
            kind=kind,
            path=component_root,
            relative_path=relative_path,
            version_source=VersionSource(version_path, version_key),
            gradle_project=gradle_project,
            mod_id=mod_id,
            pack_metafile=pack_metafile,
            dependencies=dependencies,
            suites=suites,
        )

    def component(self, subject: str) -> Component:
        try:
            return self.components[subject]
        except KeyError as error:
            expected = ", ".join(sorted(self.components))
            raise RuntimeError(
                f"Unknown component {subject!r}; expected one of: {expected}"
            ) from error

    def release_plan(self, tag: str) -> ReleasePlan:
        match = TAG_PATTERN.fullmatch(tag)
        if match is None:
            raise RuntimeError("Release tag must have the exact form subject/vX.Y.Z")
        component = self.component(match.group("subject"))
        requested = match.group("version")
        declared = component.version()
        if requested != declared:
            raise RuntimeError(
                f"Tag {tag} does not match {component.subject} version {declared}"
            )
        return ReleasePlan(
            tag=tag,
            subject=component.subject,
            version=requested,
            kind=component.kind,
            project=component.relative_path.as_posix(),
        )

    def _resolve_commit(self, revision: str) -> str | None:
        result = subprocess.run(
            [
                "git",
                "rev-parse",
                "--verify",
                "--quiet",
                "--end-of-options",
                f"{revision}^{{commit}}",
            ],
            cwd=self.root,
            capture_output=True,
            text=True,
        )
        if result.returncode == 0:
            return result.stdout.strip()
        if result.returncode == 1:
            return None
        detail = result.stderr.strip() or f"git rev-parse exited {result.returncode}"
        raise RuntimeError(f"Cannot resolve Git revision {revision!r}: {detail}")

    def changed_files(self, base: str | None, head: str) -> tuple[str, ...] | None:
        if base:
            head_commit = self._resolve_commit(head)
            if head_commit is None:
                raise RuntimeError(f"Head Git revision {head!r} is unavailable")
            base_commit = self._resolve_commit(base)
            if base_commit is None:
                return None
            result = subprocess.run(
                [
                    "git",
                    "diff",
                    "--name-only",
                    "-z",
                    base_commit,
                    head_commit,
                    "--",
                ],
                cwd=self.root,
                check=True,
                capture_output=True,
            )
            return tuple(
                item.decode("utf-8", errors="surrogateescape")
                for item in result.stdout.split(b"\0")
                if item
            )
        tracked = subprocess.run(
            ["git", "diff", "--name-only", "-z", "HEAD", "--"],
            cwd=self.root,
            check=True,
            capture_output=True,
        )
        untracked = subprocess.run(
            ["git", "ls-files", "--others", "--exclude-standard", "-z"],
            cwd=self.root,
            check=True,
            capture_output=True,
        )
        return tuple(
            item.decode("utf-8", errors="surrogateescape")
            for item in (tracked.stdout + untracked.stdout).split(b"\0")
            if item
        )

    @staticmethod
    def _matches(path: str, patterns: tuple[str, ...]) -> bool:
        for pattern in patterns:
            if pattern.endswith("/"):
                if path == pattern.rstrip("/") or path.startswith(pattern):
                    return True
            elif fnmatch.fnmatch(path, pattern):
                return True
        return False

    def affected(
        self,
        changed: tuple[str, ...] | None,
        selected: tuple[str, ...] = (),
    ) -> set[str]:
        affected = {self.component(subject).subject for subject in selected}
        if changed is None:
            affected.update(self.components)
            changed = ()
        for path in changed:
            normalized = Path(path).as_posix().removeprefix("./")
            if self._matches(normalized, self.ignored_paths):
                continue
            if self._matches(normalized, self.shared_all_paths):
                affected.update(self.components)
                continue
            if self._matches(normalized, self.shared_mod_paths):
                affected.update(
                    component.subject
                    for component in self.components.values()
                    if component.kind == "neoforge-mod"
                )
                continue
            matched = False
            for component in self.components.values():
                prefix = component.relative_path.as_posix().rstrip("/") + "/"
                if (
                    normalized == component.relative_path.as_posix()
                    or normalized.startswith(prefix)
                ):
                    affected.add(component.subject)
                    matched = True
            if not matched:
                # Unknown product paths are treated conservatively. New shared files must
                # not silently escape the build and test plan.
                affected.update(self.components)

        changed_set = True
        while changed_set:
            before = len(affected)
            affected.update(
                component.subject
                for component in self.components.values()
                if any(dependency in affected for dependency in component.dependencies)
            )
            changed_set = len(affected) != before
        return affected

    def plan(
        self,
        subjects: set[str],
        *,
        include_manual: bool = False,
    ) -> dict[str, list[dict[str, object]]]:
        result: dict[str, list[dict[str, object]]] = {
            "build": [],
            "unit": [],
            "gametest": [],
            "client": [],
            "server": [],
            "validate": [],
        }
        for subject in sorted(subjects):
            component = self.component(subject)
            if component.kind == "neoforge-mod":
                result["build"].append(component.matrix_entry())
            for suite in component.suites:
                if suite.automatic or include_manual:
                    result[suite.runner].append(component.matrix_entry(suite))
        return result


def plan_json(plan: dict[str, list[dict[str, object]]]) -> str:
    return json.dumps(plan, sort_keys=True, separators=(",", ":"))
