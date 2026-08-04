from __future__ import annotations

import argparse
import signal
import subprocess
import sys
from contextlib import nullcontext
from pathlib import Path

from .artifact import find_artifact, stage_artifact
from .config import load_java, load_packwiz, load_packwiz_installer, load_wayland_tools
from .gradle import run_gradle, task_path
from .pack import export_client_pack, export_server_pack, validate_pack
from .process import TerminationRequested, unwind_on_sigterm
from .wayland import wayland_session
from .workspace import Component, Workspace, plan_json


def _positive_int(value: str) -> int:
    parsed = int(value)
    if parsed <= 0:
        raise argparse.ArgumentTypeError("value must be positive")
    return parsed


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


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="bertie-ci",
        description="Plan and supervise Bertie's Gradle checks and release packaging",
    )
    subcommands = parser.add_subparsers(dest="command", required=True)

    build = subcommands.add_parser("build", help="build and stage NeoForge mod JARs")
    _add_project(build, "mod checkout")
    _add_component_target(build, many=True, allow_all_mods=True)
    build.add_argument(
        "--output-dir",
        type=Path,
        help="copy releaseable JARs into this artifact directory",
    )

    gradle_task = subcommands.add_parser(
        "gradle-task", help="run one or more exact Gradle tasks"
    )
    gradle_task.add_argument("--workspace", type=Path, default=Path.cwd())
    gradle_task.add_argument("--task", action="append", required=True)
    gradle_task.add_argument("--work-dir", type=Path, default=Path(".bertie-ci/gradle"))
    gradle_task.add_argument(
        "--timeout", type=_positive_int, default=60 * 60, metavar="SECONDS"
    )
    gradle_task.add_argument(
        "--wayland",
        action="store_true",
        help="provide an isolated native-Wayland compositor for this task",
    )

    pack_validate = subcommands.add_parser(
        "pack-validate", help="generate and validate a packwiz pack"
    )
    _add_project(pack_validate, "Gradle pack project")
    _add_component_target(pack_validate)

    pack_export_client = subcommands.add_parser(
        "pack-export-client", help="generate and export a Modrinth client pack"
    )
    _add_project(pack_export_client, "Gradle pack project")
    _add_component_target(pack_export_client)
    pack_export_client.add_argument("--output", type=Path, required=True)

    pack_export_server = subcommands.add_parser(
        "pack-export-server",
        help="generate and export a server installer archive",
    )
    _add_project(pack_export_server, "Gradle pack project")
    _add_component_target(pack_export_server)
    pack_export_server.add_argument("--output", type=Path, required=True)

    plan = subcommands.add_parser(
        "plan", help="emit a provider-neutral JSON plan for affected components"
    )
    plan.add_argument("--workspace", type=Path, default=Path.cwd())
    plan.add_argument("--base", help="base Git revision; omit to inspect the worktree")
    plan.add_argument("--head", default="HEAD", help="head Git revision")
    plan.add_argument("--component", action="append", default=[])
    plan.add_argument("--all", action="store_true", help="select every component")

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
    if args.component is None:
        return None
    workspace = _workspace(args)
    component = workspace.component(args.component)
    if component.kind != expected_kind:
        raise RuntimeError(
            f"Component {component.subject!r} is {component.kind}, expected {expected_kind}"
        )
    return workspace, component


def _generate_pack(args: argparse.Namespace) -> tuple[Path, Path, Path]:
    selected = _single_component(args, "pack")
    if selected is None:
        project = _project(args)
        gradle_root = project
        task = "generatePackwiz"
        output_base = project
    else:
        workspace, component = selected
        if component.gradle_project is None:
            raise RuntimeError(
                f"Pack component {component.subject!r} is not a Gradle project"
            )
        project = component.path
        gradle_root = workspace.root
        task = task_path(component.gradle_project, "generatePackwiz")
        output_base = workspace.root

    run_gradle(gradle_root, load_java().parent.parent, [task])
    generated = project / "build" / "packwiz"
    if not generated.is_dir():
        raise RuntimeError(f"Gradle did not generate the packwiz pack at {generated}")
    return generated, project, output_base


def _run_pack_validate(args: argparse.Namespace) -> None:
    generated, _, _ = _generate_pack(args)
    summary = validate_pack(generated, load_packwiz())
    print(
        "Pack valid: "
        f"{summary.metafiles} external metafiles, "
        f"{summary.local_mod_jars} local mod JARs, "
        f"{summary.config_files} config files",
        flush=True,
    )


def _run_pack_export_client(args: argparse.Namespace) -> None:
    generated, _, base = _generate_pack(args)
    output = export_client_pack(generated, _under(base, args.output), load_packwiz())
    print(f"Exported client pack: {output}", flush=True)


def _run_pack_export_server(args: argparse.Namespace) -> None:
    generated, source, base = _generate_pack(args)
    output = export_server_pack(
        generated,
        _under(base, args.output),
        load_packwiz_installer(),
        source / "README.md",
    )
    print(f"Exported server pack: {output}", flush=True)


def _under(base: Path, value: Path) -> Path:
    return value.resolve() if value.is_absolute() else (base / value).resolve()


def _run_build(args: argparse.Namespace) -> None:
    java_home = load_java().parent.parent
    selected = _selected_mods(args)
    if selected is None:
        project = _project(args)
        run_gradle(project, java_home, ["assemble"])
        artifact = find_artifact(project)
        if args.output_dir is not None:
            artifact = stage_artifact(artifact, _under(project, args.output_dir))
        print(f"Built artifact: {artifact}", flush=True)
        return

    workspace, components = selected
    run_gradle(
        workspace.root,
        java_home,
        [task_path(component.gradle_project, "assemble") for component in components],
    )
    for component in components:
        artifact = find_artifact(component.path)
        if args.output_dir is not None:
            artifact = stage_artifact(
                artifact,
                _under(workspace.root, args.output_dir) / component.subject,
            )
        print(f"Built {component.subject}: {artifact}", flush=True)


def _run_gradle_task(args: argparse.Namespace) -> None:
    workspace = Workspace.find(args.workspace)
    work = _under(workspace.root, args.work_dir)
    work.mkdir(parents=True, exist_ok=True)
    log = work / "gradle.log"
    session = (
        wayland_session(load_wayland_tools(), work / "wayland.log")
        if args.wayland
        else nullcontext(None)
    )
    with session as environment:
        run_gradle(
            workspace.root,
            load_java().parent.parent,
            tuple(dict.fromkeys(args.task)),
            log=log,
            timeout_seconds=args.timeout,
            environment=environment,
        )
    print(f"Gradle tasks passed. Log: {log}", flush=True)


def _run_plan(args: argparse.Namespace) -> None:
    workspace = Workspace.find(args.workspace)
    if args.all and args.component:
        raise RuntimeError("--all cannot be combined with --component")
    if args.all:
        subjects = set(workspace.components)
    elif args.component:
        subjects = {workspace.component(subject).subject for subject in args.component}
    else:
        changed = workspace.changed_files(args.base, args.head)
        if changed is None:
            print(
                f"bertie-ci: base Git revision {args.base!r} is unavailable; "
                "planning all components",
                file=sys.stderr,
                flush=True,
            )
        subjects = workspace.affected(changed)
    print(plan_json(workspace.plan(subjects)), flush=True)


def tolerate_unencodable_output() -> None:
    """Keep mirrored process output from failing on a narrow console encoding."""
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure is not None:
            reconfigure(errors="replace")


def main() -> None:
    tolerate_unencodable_output()
    parser = _parser()
    args = parser.parse_args()
    try:
        with unwind_on_sigterm():
            match args.command:
                case "build":
                    _run_build(args)
                case "gradle-task":
                    _run_gradle_task(args)
                case "pack-validate":
                    _run_pack_validate(args)
                case "pack-export-client":
                    _run_pack_export_client(args)
                case "pack-export-server":
                    _run_pack_export_server(args)
                case "plan":
                    _run_plan(args)
                case "release-plan":
                    workspace = Workspace.find(args.workspace)
                    print(workspace.release_plan(args.tag).to_json(), flush=True)
    except TerminationRequested as error:
        parser.exit(
            128 + error.signal_number,
            f"bertie-ci: terminated by {signal.Signals(error.signal_number).name}\n",
        )
    except (OSError, RuntimeError, subprocess.SubprocessError) as error:
        parser.exit(2, f"bertie-ci: {error}\n")


if __name__ == "__main__":
    main()
