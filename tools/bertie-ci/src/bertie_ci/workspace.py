from __future__ import annotations

import fnmatch
import json
import re
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Literal

import tomllib

WORKSPACE_FORMAT = "bertie-ci.workspace.v2"
COMPONENT_FORMAT = "bertie-ci.component.v2"
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
        raise RuntimeError(f"{label} must not escape its base directory: {value}")
    resolved = (root / relative).resolve()
    try:
        resolved.relative_to(root.resolve())
    except ValueError as error:
        raise RuntimeError(f"{label} escapes its base directory: {value}") from error
    return resolved


def _task_path(gradle_project: str, task: str) -> str:
    return f"{gradle_project.rstrip(':')}:{task}"


def _has_sources(root: Path) -> bool:
    return root.is_dir() and any(path.is_file() for path in root.rglob("*"))


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
class Component:
    subject: str
    kind: ComponentKind
    path: Path
    relative_path: Path
    version_source: VersionSource
    gradle_project: str | None
    dependencies: tuple[str, ...]

    def version(self) -> str:
        return self.version_source.read()

    def plan_entry(self, task: str | None = None) -> dict[str, object]:
        entry: dict[str, object] = {
            "subject": self.subject,
            "project": self.relative_path.as_posix(),
        }
        if self.gradle_project is not None:
            entry["gradle_project"] = self.gradle_project
        if task is not None:
            if self.gradle_project is None:
                raise RuntimeError(f"{self.subject} is not a Gradle component")
            entry["task"] = _task_path(self.gradle_project, task)
        return entry

    def test_tasks(self) -> tuple[tuple[str, str], ...]:
        if self.gradle_project is None:
            return ()
        conventional = (
            ("unit", "test", self.path / "src" / "test"),
            ("gametest", "runGameTests", self.path / "src" / "gametest"),
            ("client", "runClientTests", self.path / "src" / "clienttest"),
        )
        return tuple(
            (runner, task) for runner, task, root in conventional if _has_sources(root)
        )


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
        shared_gradle_paths: tuple[str, ...],
        shared_all_paths: tuple[str, ...],
        ignored_paths: tuple[str, ...],
    ) -> None:
        self.root = root
        self.components = components
        self.shared_gradle_paths = shared_gradle_paths
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
                "shared-gradle-paths",
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
            shared_gradle_paths=_strings(
                workspace.get("shared-gradle-paths", []),
                "workspace.shared-gradle-paths",
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
                "depends-on",
                "version",
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
        version_file = version.get("file")
        if not isinstance(version_file, str) or not version_file:
            raise RuntimeError(
                f"version.file must be a non-empty string in {descriptor}"
            )
        version_path = _relative(component_root, version_file, "version.file")
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
        if kind in ("neoforge-mod", "pack") and gradle_project is None:
            raise RuntimeError(f"Gradle component lacks gradle-project: {descriptor}")
        dependencies = _strings(data.get("depends-on", []), "depends-on")
        return Component(
            subject=subject,
            kind=kind,
            path=component_root,
            relative_path=relative_path,
            version_source=VersionSource(version_path, version_key),
            gradle_project=gradle_project,
            dependencies=dependencies,
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
            if self._matches(normalized, self.shared_gradle_paths):
                affected.update(
                    component.subject
                    for component in self.components.values()
                    if component.gradle_project is not None
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

        if any(self.component(subject).kind == "neoforge-mod" for subject in affected):
            affected.update(
                component.subject
                for component in self.components.values()
                if component.kind == "pack"
            )
        return affected

    def plan(self, subjects: set[str]) -> dict[str, list[dict[str, object]]]:
        result: dict[str, list[dict[str, object]]] = {
            "build": [],
            "unit": [],
            "gametest": [],
            "client": [],
            "validate": [],
        }
        for subject in sorted(subjects):
            component = self.component(subject)
            if component.kind == "neoforge-mod":
                result["build"].append(component.plan_entry("assemble"))
            if component.kind == "pack":
                result["validate"].append(component.plan_entry())
            for runner, task in component.test_tasks():
                entry = component.plan_entry(task)
                result[runner].append(entry)
        return result


def plan_json(plan: dict[str, list[dict[str, object]]]) -> str:
    return json.dumps(plan, sort_keys=True, separators=(",", ":"))
