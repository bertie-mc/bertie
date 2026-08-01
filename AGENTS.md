# AGENTS.md

Instructions for agents working in the Bertie repository.

## Repository state

Finish changes committed, SSH-signed, rebased on `origin/main`, and pushed to GitHub.
Never force-push. Leave a clean tree and exactly one worktree. Use Conventional Commits.

## Repository boundaries

- `mods/` contains the owned NeoForge mods in the current Minecraft 1.21.1 train.
- `pack/` is a packwiz manifest. Never commit mod JARs or generated Minecraft instances.
- `build-logic/` owns shared Gradle mechanics. Projects opt into small convention plugins;
  do not add root `allprojects` or `subprojects` configuration.
- `tools/bertie-ci/` owns provider-neutral orchestration. GitHub workflow YAML stays a thin
  adapter around its commands.
- Assertions and fixtures belong to the component they test.

After every pack manifest change, run `packwiz refresh` and commit the resulting index
with the change. Use exact project slugs when adding third-party mods, verify every
metafile's side, and inspect the diff for accidental version changes. In-repository mods
remain fixed release references: update their URL, filename, and hash deliberately rather
than enabling a broad GitHub updater.

`bertie-mod-atlas`, `bertie-progression-planning`, the organisation `.github` repository,
and `Nekos-Enchanted-Books` are separate projects. Update their references when paths or
public procedures change, but do not copy their source here.

## Toolchain and build

Enter the pinned environment with `nix develop`. Nix supplies JDK 21, Gradle 8, packwiz,
Python, and headless runtime dependencies. Do not add Gradle wrappers or Foojay toolchain
downloads.

Build and test operations remain separate. Target a mod with its Gradle project path, for
example `gradle :mods:berlords-carving:assemble` or `gradle :mods:berlords-carving:test`. Use the smallest
relevant unit, GameTest, client, or server suite, then run broader affected-project checks.
For every owned NeoForge module directly under `mods/`, the runtime mod ID is the directory
basename with hyphens removed; the component subject and release tag keep the readable kebab-case
directory name.

## Releases

Components retain independent versions. Release tags are annotated and have the exact
form `<subject>/vX.Y.Z`, such as `primitive-refined/v0.3.0` or `pack/v0.2.0`. Releases are
manual after the current commit's required pipelines pass. Pack entries for in-repository
mods use fixed release URLs and are updated deliberately; do not add a monorepo-wide
packwiz GitHub auto-update convention.

## Safety and licensing

Never vendor dependency JARs or third-party assets. Preserve every component's own
licence and NOTICE; the repository has no blanket licence. Do not rewrite or discard
concurrent work. Before deleting or moving anything, search all references.
