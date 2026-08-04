# AGENTS.md

Agent-specific instructions for the Bertie monorepo.

## Required reading

Before changing build, test, packaging, or release infrastructure, read:

- [`docs/dependencies.md`](docs/dependencies.md) for dependency ownership, runtime sides,
  reproducibility, and pack generation;
- [`docs/testing.md`](docs/testing.md) for source sets, test APIs, execution, and
  diagnostics;
- [`docs/cicd.md`](docs/cicd.md) for orchestration, affected planning, Wayland, exports,
  and releases;
- the nearest component README for component-specific behavior and constraints.

Do not recreate those policies in this file. Update the shared documentation when a
human-facing procedure or architecture changes.

## Delivery

Unless the user says otherwise, finish changes committed with an SSH signature, rebased
on `origin/main`, and pushed to GitHub. Use Conventional Commits, never force-push, leave a
clean tree, and keep exactly one worktree.

## Working rules

- Preserve concurrent and pre-existing work. Never rewrite or discard changes merely to
  obtain a clean tree.
- Search all references before deleting or moving files.
- Keep assertions and fixtures with the component whose behavior they test.
- Put shared Gradle mechanics in focused convention plugins; do not add root
  `allprojects` or `subprojects` configuration.
- Keep orchestration in Gradle and `bertie-ci`; workflow YAML passes inputs to their
  actions and commands.
- Never commit generated packwiz files, dependency JARs, Minecraft instances, or other
  build outputs.
- Never vendor third-party dependencies or assets. Preserve each component's licence and
  NOTICE.
- Work only in this repository unless the user explicitly includes one of the separately
  maintained Bertie projects in scope.

Enter `nix develop` for repository commands. Start with the smallest relevant checks from
the testing and dependency guides, then run broader affected checks before delivery.
