# AGENTS.md

Agent-specific instructions for the Bertie monorepo.

## Required reading

Before changing code, read [`docs/overview.md`](docs/overview.md) for repository
boundaries and the nearest component README for component-specific behavior and
constraints. Before changing build, test, packaging, or release infrastructure, also read:

- [`docs/dependencies.md`](docs/dependencies.md) for adding dependencies, runtime sides,
  locks, checksums, and pack generation;
- [`docs/testing.md`](docs/testing.md) for choosing, writing, running, and diagnosing tests;
- [`docs/cicd.md`](docs/cicd.md) for local checks, affected planning, pack exports, and
  releases.

Do not recreate those guides in this file. Update them when a developer-facing command,
convention, or procedure changes. Keep implementation details in code and focused
comments rather than narrating them in the guides.

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
