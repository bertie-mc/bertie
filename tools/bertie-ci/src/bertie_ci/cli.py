from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

from .artifact import find_artifact, stage_artifact
from .config import (
    load_client_runtime_tools,
    load_fixture_tools,
    load_java,
    load_pack_tools,
    load_packwiz,
    load_packwiz_installer,
    load_server_runtime_tools,
    load_versions,
)
from .gradle import (
    assemble_client_test_mod,
    assemble_mod,
    run_gametests,
    run_gradle,
    run_unit_tests,
    task_path,
)
from .instance import (
    install_mod,
    load_instance,
    prepare_mod_instance,
    prepare_pack_instance,
)
from .pack import (
    PackMod,
    export_client_pack,
    export_server_pack,
    read_pack_mod,
    validate_pack,
)
from .runtime import RuntimeContext, run_client_test, run_server_test
from .workspace import Component, Workspace, plan_json


def _add_project(
    parser: argparse.ArgumentParser, help_text: str = "project checkout"
) -> None:
    parser.add_argument("--project", type=Path, default=Path.cwd(), help=help_text)


def _add_component_target(
    parser: argparse.ArgumentParser,
    *,
    many: bool = False,
    allow_all_mods: bool = False,
) -> None:
    parser.add_argument(
        "--workspace",
        type=Path,
        help="workspace root; discovered from the current directory when omitted",
    )
    parser.add_argument(
        "--component",
        action="append" if many else "store",
        default=[] if many else None,
        help="workspace component subject",
    )
    if allow_all_mods:
        parser.add_argument(
            "--all-mods",
            action="store_true",
            help="target every NeoForge mod component in the workspace",
        )


def _add_fixture(parser: argparse.ArgumentParser) -> None:
    parser.add_argument(
        "--fixture",
        action="append",
        default=[],
        help=(
            "comma-separated canonical mod names or aggregate fixture profiles; "
            "may be repeated"
        ),
    )


def _memory(value: str) -> str:
    if not re.fullmatch(r"[1-9][0-9]*[mMgG]", value):
        raise argparse.ArgumentTypeError("memory must look like 4G or 1024M")
    return value.upper()


def _nonnegative_int(value: str) -> int:
    parsed = int(value)
    if parsed < 0:
        raise argparse.ArgumentTypeError("value cannot be negative")
    return parsed


def _positive_int(value: str) -> int:
    parsed = int(value)
    if parsed <= 0:
        raise argparse.ArgumentTypeError("value must be positive")
    return parsed


def _optional_path(value: str) -> Path | None:
    """Translate an empty adapter input into an omitted optional path."""
    return Path(value) if value else None


def _add_runtime(
    parser: argparse.ArgumentParser, default_timeout: int, default_memory: str
) -> None:
    parser.add_argument("--instance", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path)
    parser.add_argument("--cache-dir", type=Path)
    parser.add_argument(
        "--timeout", type=_positive_int, default=default_timeout, metavar="SECONDS"
    )
    parser.add_argument("--max-memory", type=_memory, default=default_memory)


def _add_test_extensions(parser: argparse.ArgumentParser) -> None:
    parser.add_argument(
        "--test-mod",
        action="append",
        default=[],
        type=_optional_path,
        metavar="JAR",
        help=(
            "optional test-only mod JAR or directory containing one JAR; "
            "may be repeated"
        ),
    )
    parser.add_argument(
        "--require-log",
        action="append",
        default=[],
        metavar="TEXT",
        help="fail unless the runtime log contains this exact text; may be repeated",
    )


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="bertie-ci",
        description="Composable checks for bertie-mc projects",
    )
    subcommands = parser.add_subparsers(dest="command", required=True)

    build = subcommands.add_parser("build", help="build a NeoForge mod JAR")
    _add_project(build, "mod checkout")
    _add_component_target(build, many=True, allow_all_mods=True)
    build.add_argument(
        "--output-dir",
        type=Path,
        help="copy the single releaseable JAR into this artifact directory",
    )

    build_client_test_mod = subcommands.add_parser(
        "build-client-test-mod",
        help="build and stage the test-only mod from the clientTest source set",
    )
    _add_project(build_client_test_mod, "mod checkout")
    _add_component_target(build_client_test_mod)
    build_client_test_mod.add_argument(
        "--output-dir",
        type=Path,
        default=Path(".bertie-ci/client-test-mod"),
        help="directory for the staged client-test-mod.jar",
    )

    unit_test = subcommands.add_parser(
        "unit-test", help="run the mod's ordinary JVM unit tests"
    )
    _add_project(unit_test, "mod checkout")
    _add_component_target(unit_test, many=True, allow_all_mods=True)

    gametest = subcommands.add_parser(
        "gametest", help="run NeoForge GameTests in the Gradle development runtime"
    )
    _add_project(gametest, "mod checkout")
    _add_component_target(gametest)
    gametest.add_argument("--work-dir", type=Path)
    gametest.add_argument(
        "--timeout", type=_positive_int, default=15 * 60, metavar="SECONDS"
    )

    prepare_mod = subcommands.add_parser(
        "prepare-mod-instance",
        help="assemble a side-specific instance around a built mod",
    )
    _add_project(prepare_mod, "mod checkout")
    _add_component_target(prepare_mod)
    prepare_mod.add_argument("--artifact", type=Path)
    _add_fixture(prepare_mod)
    prepare_mod.add_argument("--side", choices=("client", "server"), required=True)
    prepare_mod.add_argument("--output-dir", type=Path, required=True)

    prepare_pack = subcommands.add_parser(
        "prepare-pack-instance", help="install one side of a canonical packwiz pack"
    )
    _add_project(prepare_pack, "packwiz checkout")
    _add_component_target(prepare_pack)
    prepare_pack.add_argument("--side", choices=("client", "server"), required=True)
    prepare_pack.add_argument("--output-dir", type=Path, required=True)

    client_test = subcommands.add_parser(
        "client-test",
        help="run the world-join scenario and optional extensions in a prepared client",
    )
    _add_runtime(client_test, 25 * 60, "4G")
    _add_test_extensions(client_test)
    client_test.add_argument(
        "--minimum-game-tests",
        type=_nonnegative_int,
        default=0,
        metavar="COUNT",
        help="fail unless mc-runtime-test discovers at least this many GameTests",
    )

    server_test = subcommands.add_parser(
        "server-test",
        help="run readiness or a project-owned scenario in a prepared server",
    )
    _add_runtime(server_test, 15 * 60, "3G")
    _add_test_extensions(server_test)
    server_test.add_argument(
        "--command-test",
        type=Path,
        required=True,
        metavar="JSON",
        help="project-owned HeadlessMC command-test specification",
    )

    pack_validate = subcommands.add_parser(
        "pack-validate", help="validate a packwiz checkout without modifying it"
    )
    _add_project(pack_validate, "packwiz checkout")
    _add_component_target(pack_validate)

    pack_export_client = subcommands.add_parser(
        "pack-export-client", help="export a Modrinth client pack"
    )
    _add_project(pack_export_client, "packwiz checkout")
    _add_component_target(pack_export_client)
    pack_export_client.add_argument("--output", type=Path, required=True)

    pack_export_server = subcommands.add_parser(
        "pack-export-server", help="export a no-mod-JAR server installer archive"
    )
    _add_project(pack_export_server, "packwiz checkout")
    _add_component_target(pack_export_server)
    pack_export_server.add_argument("--output", type=Path, required=True)

    overlay_mod = subcommands.add_parser(
        "overlay-mod", help="install a built mod into a prepared instance"
    )
    overlay_mod.add_argument("--instance", type=Path, required=True)
    overlay_mod.add_argument("--artifact", type=Path, required=True)
    overlay_mod.add_argument(
        "--replace",
        metavar="FILENAME",
        help="explicit installed mod filename to replace",
    )

    overlay_components = subcommands.add_parser(
        "overlay-components",
        help="overlay workspace component artifacts onto a prepared pack instance",
    )
    _add_component_target(overlay_components, many=True, allow_all_mods=True)
    overlay_components.add_argument("--instance", type=Path, required=True)
    overlay_components.add_argument("--artifact-dir", type=Path, required=True)
    overlay_components.add_argument(
        "--pack-component",
        default="pack",
        help="pack component whose metafiles identify published JARs",
    )

    plan = subcommands.add_parser(
        "plan", help="emit a provider-neutral JSON plan for affected components"
    )
    plan.add_argument("--workspace", type=Path, default=Path.cwd())
    plan.add_argument("--base", help="base Git revision; omit to inspect the worktree")
    plan.add_argument("--head", default="HEAD", help="head Git revision")
    plan.add_argument("--component", action="append", default=[])
    plan.add_argument("--all", action="store_true", help="select every component")
    plan.add_argument(
        "--include-manual",
        action="store_true",
        help="include suites such as scheduled full-pack runtime checks",
    )

    release_plan = subcommands.add_parser(
        "release-plan", help="validate a subject/vX.Y.Z tag and emit component JSON"
    )
    release_plan.add_argument("--workspace", type=Path, default=Path.cwd())
    release_plan.add_argument("--tag", required=True)

    return parser


def _project(args: argparse.Namespace) -> Path:
    return args.project.resolve(strict=True)


def _workspace(args: argparse.Namespace) -> Workspace:
    return Workspace.find(args.workspace or Path.cwd())


def _selected_mods(
    args: argparse.Namespace,
) -> tuple[Workspace, tuple[Component, ...]] | None:
    requested = tuple(args.component or ())
    if not requested and not getattr(args, "all_mods", False):
        return None
    workspace = _workspace(args)
    if getattr(args, "all_mods", False):
        if requested:
            raise RuntimeError("--all-mods cannot be combined with --component")
        components = tuple(
            component
            for component in workspace.components.values()
            if component.kind == "neoforge-mod"
        )
    else:
        components = tuple(workspace.component(subject) for subject in requested)
    non_mods = [
        component.subject
        for component in components
        if component.kind != "neoforge-mod"
    ]
    if non_mods:
        raise RuntimeError(
            f"Expected NeoForge mod component(s), got: {', '.join(non_mods)}"
        )
    return workspace, tuple(sorted(components, key=lambda item: item.subject))


def _single_component(
    args: argparse.Namespace, expected_kind: str
) -> tuple[Workspace, Component] | None:
    subject = args.component
    if subject is None:
        return None
    workspace = _workspace(args)
    component = workspace.component(subject)
    if component.kind != expected_kind:
        raise RuntimeError(
            f"Component {subject!r} is {component.kind}, expected {expected_kind}"
        )
    return workspace, component


def _component_or_project(
    args: argparse.Namespace, expected_kind: str
) -> tuple[Path, Path, str | None]:
    selected = _single_component(args, expected_kind)
    if selected is None:
        project = _project(args)
        return project, project, None
    workspace, component = selected
    return component.path, workspace.root, component.gradle_project


def _path_base(args: argparse.Namespace, project: Path) -> Path:
    return _workspace(args).root if getattr(args, "component", None) else project


def _under_project(project: Path, path: Path) -> Path:
    return path.resolve() if path.is_absolute() else (project / path).resolve()


def _cache(path: Path | None) -> Path:
    default = (
        Path(os.environ.get("XDG_CACHE_HOME", Path.home() / ".cache")) / "bertie-ci"
    )
    if path is None:
        return default.resolve()
    return path.resolve()


def _fixture_profiles(values: list[str]) -> list[str]:
    return [
        name.strip() for value in values for name in value.split(",") if name.strip()
    ]


def _run_build(args: argparse.Namespace) -> None:
    java = load_java()
    selected = _selected_mods(args)
    if selected is None:
        project = _project(args)
        assemble_mod(project, java.parent.parent)
        artifact = find_artifact(project, None)
        if args.output_dir is not None:
            artifact = stage_artifact(
                artifact, _under_project(project, args.output_dir)
            )
        print(f"Built artifact: {artifact}", flush=True)
        return

    workspace, components = selected
    run_gradle(
        workspace.root,
        java.parent.parent,
        [task_path(component.gradle_project, "assemble") for component in components],
    )
    for component in components:
        artifact = find_artifact(component.path, None)
        if args.output_dir is not None:
            root = _under_project(workspace.root, args.output_dir)
            artifact = stage_artifact(artifact, root / component.subject)
        print(f"Built {component.subject}: {artifact}", flush=True)


def _run_build_client_test_mod(args: argparse.Namespace) -> None:
    project, gradle_root, gradle_project = _component_or_project(args, "neoforge-mod")
    base = _path_base(args, project)
    java = load_java()
    artifact = assemble_client_test_mod(
        project,
        java.parent.parent,
        _under_project(base, args.output_dir),
        gradle_root=gradle_root,
        gradle_project=gradle_project,
    )
    print(f"Built client test mod: {artifact}", flush=True)


def _run_gametest(args: argparse.Namespace) -> None:
    project, gradle_root, gradle_project = _component_or_project(args, "neoforge-mod")
    work = _under_project(
        _path_base(args, project), args.work_dir or Path(".bertie-ci")
    )
    java = load_java()
    count = run_gametests(
        project,
        java.parent.parent,
        work,
        args.timeout,
        gradle_root=gradle_root,
        gradle_project=gradle_project,
    )
    print(
        f"NeoForge GameTests passed: {count} test(s). Logs: {work / 'gametest.log'}",
        flush=True,
    )


def _run_unit_test(args: argparse.Namespace) -> None:
    java = load_java()
    selected = _selected_mods(args)
    if selected is None:
        project = _project(args)
        run_unit_tests(project, java.parent.parent)
        print("JVM unit tests passed.", flush=True)
        return
    workspace, components = selected
    run_gradle(
        workspace.root,
        java.parent.parent,
        [task_path(component.gradle_project, "test") for component in components],
    )
    subjects = ", ".join(component.subject for component in components)
    print(f"JVM unit tests passed: {subjects}.", flush=True)


def _runtime_context(
    descriptor: Path, work: Path | None, cache: Path | None, side: str
) -> RuntimeContext:
    descriptor = descriptor.resolve(strict=True)
    instance = load_instance(descriptor)
    if instance.side != side:
        raise RuntimeError(
            f"{side} test cannot consume a {instance.side} prepared instance"
        )
    runtime_work = (work or descriptor.parent).resolve()
    runtime_cache = _cache(cache)
    runtime_cache.mkdir(parents=True, exist_ok=True)
    tools = (
        load_client_runtime_tools() if side == "client" else load_server_runtime_tools()
    )
    return RuntimeContext(runtime_work, runtime_cache, instance, load_versions(), tools)


def _run_test(args: argparse.Namespace, side: str) -> None:
    context = _runtime_context(args.instance, args.work_dir, args.cache_dir, side)
    test_mods = tuple(path.resolve() for path in args.test_mod if path is not None)
    required_log_markers = tuple(marker for marker in args.require_log if marker)
    if side == "client":
        run_client_test(
            context,
            args.timeout,
            args.max_memory,
            minimum_game_tests=args.minimum_game_tests,
            test_mods=test_mods,
            required_log_markers=required_log_markers,
        )
    else:
        run_server_test(
            context,
            args.timeout,
            args.max_memory,
            command_test=args.command_test,
            test_mods=test_mods,
            required_log_markers=required_log_markers,
        )


def _pack_mod(pack: Component, mod: Component) -> PackMod:
    if mod.pack_metafile is None:
        raise RuntimeError(f"{mod.subject} has no pack-metafile mapping")
    return read_pack_mod(pack.path, mod.pack_metafile)


def _run_overlay_components(args: argparse.Namespace) -> None:
    selected = _selected_mods(args)
    if selected is None:
        raise RuntimeError("overlay-components requires --component or --all-mods")
    workspace, components = selected
    pack = workspace.component(args.pack_component)
    if pack.kind != "pack":
        raise RuntimeError(f"{args.pack_component!r} is not a pack component")
    artifact_root = _under_project(workspace.root, args.artifact_dir)
    descriptor = args.instance.resolve(strict=True)
    instance = load_instance(descriptor)
    for component in components:
        metadata = _pack_mod(pack, component)
        if not metadata.applies_to(instance.side):
            print(
                f"Skipped {component.subject}: pack side is {metadata.side}",
                flush=True,
            )
            continue
        artifact = artifact_root / component.subject
        installed = install_mod(
            descriptor,
            artifact,
            replace_filename=metadata.filename,
        )
        print(f"Overlaid {component.subject}: {installed}", flush=True)


def _run_plan(args: argparse.Namespace) -> None:
    workspace = Workspace.find(args.workspace)
    if args.all and args.component:
        raise RuntimeError("--all cannot be combined with --component")
    selected = tuple(workspace.components) if args.all else tuple(args.component)
    changed = () if selected else workspace.changed_files(args.base, args.head)
    if changed is None:
        print(
            f"bertie-ci: base Git revision {args.base!r} is unavailable; "
            "planning all components",
            file=sys.stderr,
            flush=True,
        )
    subjects = workspace.affected(changed, selected)
    print(
        plan_json(workspace.plan(subjects, include_manual=args.include_manual)),
        flush=True,
    )


def _run_release_plan(args: argparse.Namespace) -> None:
    workspace = Workspace.find(args.workspace)
    print(workspace.release_plan(args.tag).to_json(), flush=True)


def tolerate_unencodable_output() -> None:
    """Never let mirrored subprocess output kill a run.

    Minecraft logs carry characters the Windows ANSI code page cannot encode, and
    a redirected stdout on Windows uses that code page rather than UTF-8. The
    unabridged text is always kept in the run's log file, which is UTF-8.
    """
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure is not None:
            reconfigure(errors="replace")


def main() -> None:
    tolerate_unencodable_output()
    parser = _parser()
    args = parser.parse_args()
    try:
        match args.command:
            case "build":
                _run_build(args)
            case "build-client-test-mod":
                _run_build_client_test_mod(args)
            case "unit-test":
                _run_unit_test(args)
            case "gametest":
                _run_gametest(args)
            case "prepare-mod-instance":
                project, _, _ = _component_or_project(args, "neoforge-mod")
                base = _path_base(args, project)
                artifact = args.artifact
                if artifact is not None and not artifact.is_absolute():
                    artifact = base / artifact
                descriptor = prepare_mod_instance(
                    project,
                    artifact,
                    _fixture_profiles(args.fixture),
                    args.side,
                    _under_project(base, args.output_dir),
                    load_versions(),
                    load_fixture_tools(),
                )
                print(f"Prepared instance: {descriptor}", flush=True)
            case "prepare-pack-instance":
                project, _, _ = _component_or_project(args, "pack")
                base = _path_base(args, project)
                descriptor = prepare_pack_instance(
                    project,
                    args.side,
                    _under_project(base, args.output_dir),
                    load_pack_tools(),
                )
                print(f"Prepared instance: {descriptor}", flush=True)
            case "client-test" | "server-test":
                _run_test(args, args.command.removesuffix("-test"))
            case "pack-validate":
                project, _, _ = _component_or_project(args, "pack")
                summary = validate_pack(project, load_packwiz())
                print(
                    "Pack valid: "
                    f"{summary.metafiles} metafiles "
                    f"({summary.client} client, {summary.server} server, {summary.both} both), "
                    f"{summary.config_files} config files",
                    flush=True,
                )
            case "pack-export-client":
                project, _, _ = _component_or_project(args, "pack")
                output = export_client_pack(
                    project,
                    _under_project(_path_base(args, project), args.output),
                    load_packwiz(),
                )
                print(f"Exported client pack: {output}", flush=True)
            case "pack-export-server":
                project, _, _ = _component_or_project(args, "pack")
                output = export_server_pack(
                    project,
                    _under_project(_path_base(args, project), args.output),
                    load_packwiz_installer(),
                )
                print(f"Exported server pack: {output}", flush=True)
            case "overlay-mod":
                installed = install_mod(
                    args.instance.resolve(strict=True),
                    args.artifact.resolve(strict=True),
                    replace_filename=args.replace,
                )
                print(f"Installed mod: {installed}", flush=True)
            case "overlay-components":
                _run_overlay_components(args)
            case "plan":
                _run_plan(args)
            case "release-plan":
                _run_release_plan(args)
    except (OSError, RuntimeError, subprocess.SubprocessError) as error:
        parser.exit(2, f"bertie-ci: {error}\n")


if __name__ == "__main__":
    main()
