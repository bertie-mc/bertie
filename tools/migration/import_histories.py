#!/usr/bin/env python3
"""Import the frozen Bertie polyrepo histories under their monorepo paths.

The source repositories are never modified. Each one is cloned into a temporary
directory, rewritten there, fetched into the target, and merged with a signed commit.
"""

from __future__ import annotations

import argparse
import dataclasses
import pathlib
import shutil
import subprocess
import tempfile


@dataclasses.dataclass(frozen=True)
class Component:
    repository: str
    revision: str
    destination: str
    branch: str = "main"


COMPONENTS = (
    Component("berlords-emi", "514abd3320c2cda3c1a34ef31185010585df6e5a", "mods/berlords-emi"),
    Component("berlords-food-system", "5a21b5fcfa22ea3489ab6810a28a2094f43a026e", "mods/berlords-food-system"),
    Component("bertie-blackhole", "234b1accf210d38f486290e5014578e8be13271d", "mods/bertie-blackhole"),
    Component("bertie-ci", "58a81559f91ad442a95128f23f6948685c7fb04d", "tools/bertie-ci"),
    Component("bertie-filters", "6c4d48835f8078f459ef1191417b616281a24659", "mods/bertie-filters"),
    Component("bertie-pack", "f551bd10b253d8371068a9971796c8f5502094a2", "pack"),
    Component("bertie-progression", "416939b444a5e553db9070c545e26591eb596982", "mods/bertie-progression"),
    Component("bertie-tiers", "330df0ec041594813721494b6d5d8667b5c436f7", "mods/bertie-tiers"),
    Component("bertie-weapons", "9e8aafb1ceffb4a79c845b4cf193b82e6d27b8c7", "mods/bertie-weapons"),
    Component("bush-tweaks", "aac66275989df8977ace8814c26003f47c4f6157", "mods/bush-tweaks"),
    Component("carving", "8ed879e470d4b1feca70e5f39434e66f7ab57e48", "mods/carving"),
    Component("ender-eyes", "de0e542cfb23a50663452568564b840724f85fda", "mods/ender-eyes"),
    Component("explode-to-mine", "b01572fd208a6f916c3a04297174ba111d4d9e6c", "mods/explode-to-mine"),
    Component("explosive-enhancement", "cd1b8811af1481f5112b4504b42eafe1d6cfe56d", "mods/explosive-enhancement"),
    Component("fart-bomb", "0b307f72ff609ed689895d486698914c054fdfa2", "mods/fart-bomb"),
    Component("fd-shader-fix", "378f8e7d0f52fdb2af91ac2a19a40ba395cdfb95", "mods/fd-shader-fix"),
    Component("forge-ink", "819a54a9d772d2e2fea66b26228f8d0647619e7e", "mods/forge-ink"),
    Component("frozen-reg-fix", "40714e5b4af108547909bd3bf409550cccc5cb4d", "mods/frozen-reg-fix"),
    Component("hephaestus-architecture", "6d84bf4748fc8ffb75787a4ec56a616e7015eee7", "mods/hephaestus-architecture"),
    Component("primitive-refined", "c69f73be3c0cfd194d266f4b021d390be1ebd340", "mods/primitive-refined"),
    Component("rustic-engineer-fix", "c878d464f91bf4f1c517aba267a34da502ad0fee", "mods/rustic-engineer-fix"),
    Component("short-circuit-fix", "78e82917befd7b54667ed652658dd56a3b52d8d9", "mods/short-circuit-fix"),
    Component("withered-hearts", "53738dc6afc767b9053cfb7d6bf9c3ca85569df7", "mods/withered-hearts"),
)


EMAIL_CALLBACK = (
    'return b"berlord@rambler.ru" '
    'if email.lower() == b"dinex435@gmail.com" else email'
)

MESSAGE_CALLBACK = """
lines = message.splitlines()
filtered = [
    line for line in lines
    if not (
        line.lstrip().lower().startswith(b"co-authored-by:")
        and b"claude" in line.lower()
    )
]
if len(filtered) == len(lines):
    return message
return b"\\n".join(filtered).rstrip(b"\\n") + b"\\n"
""".strip()


def run(*args: str | pathlib.Path, cwd: pathlib.Path | None = None) -> str:
    completed = subprocess.run(
        [str(arg) for arg in args],
        cwd=cwd,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
    )
    return completed.stdout.strip()


def validate_source(workspace: pathlib.Path, component: Component) -> pathlib.Path:
    source = workspace / component.repository
    if run("git", "status", "--porcelain=v1", cwd=source):
        raise RuntimeError(f"{component.repository} has uncommitted changes")
    revision = run("git", "rev-parse", component.branch, cwd=source)
    if revision != component.revision:
        raise RuntimeError(
            f"{component.repository} moved: expected {component.revision}, found {revision}"
        )
    return source


def verify_rewrite(repository: pathlib.Path) -> None:
    identities = run("git", "log", "--format=%ae%n%ce", "HEAD", cwd=repository)
    if "dinex435@gmail.com" in identities.lower():
        raise RuntimeError(f"email rewrite failed in {repository.name}")
    messages = run("git", "log", "--format=%B%x00", "HEAD", cwd=repository)
    for line in messages.splitlines():
        if line.lower().startswith("co-authored-by:") and "claude" in line.lower():
            raise RuntimeError(f"Claude trailer rewrite failed in {repository.name}")


def import_component(
    workspace: pathlib.Path,
    target: pathlib.Path,
    filter_repo: pathlib.Path,
    temporary_root: pathlib.Path,
    component: Component,
) -> None:
    source = validate_source(workspace, component)
    rewritten = temporary_root / component.repository
    run(
        "git",
        "clone",
        "--no-local",
        "--single-branch",
        "--branch",
        component.branch,
        source,
        rewritten,
    )
    run(
        filter_repo,
        "--force",
        "--to-subdirectory-filter",
        component.destination,
        "--email-callback",
        EMAIL_CALLBACK,
        "--message-callback",
        MESSAGE_CALLBACK,
        cwd=rewritten,
    )
    verify_rewrite(rewritten)

    remote = f"import-{component.repository}"
    run("git", "remote", "add", remote, rewritten, cwd=target)
    try:
        run("git", "fetch", "--no-tags", remote, component.branch, cwd=target)
        run(
            "git",
            "merge",
            "--allow-unrelated-histories",
            "--no-ff",
            "--no-edit",
            "-S",
            "-m",
            f"chore(migration): import {component.repository} history",
            f"{remote}/{component.branch}",
            cwd=target,
        )
    finally:
        run("git", "remote", "remove", remote, cwd=target)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", type=pathlib.Path, required=True)
    parser.add_argument("--target", type=pathlib.Path, required=True)
    parser.add_argument("--git-filter-repo", type=pathlib.Path, required=True)
    arguments = parser.parse_args()

    workspace = arguments.workspace.resolve()
    target = arguments.target.resolve()
    if run("git", "status", "--porcelain=v1", cwd=target):
        raise RuntimeError("target repository must be clean before importing histories")

    with tempfile.TemporaryDirectory(prefix="bertie-history-import-") as temporary:
        temporary_root = pathlib.Path(temporary)
        for component in COMPONENTS:
            import_component(
                workspace,
                target,
                arguments.git_filter_repo,
                temporary_root,
                component,
            )

    shutil.rmtree(target / ".git" / "filter-repo", ignore_errors=True)


if __name__ == "__main__":
    main()
