# Bertie

Bertie is a Minecraft 1.21.1 NeoForge modpack and the custom mods maintained for it.
This monorepo is the public source of truth for the pack, 21 independently versioned
mods, shared Kotlin Gradle build logic, and local-first test infrastructure.

## Repository layout

- `mods/` — custom NeoForge mods, each with its own version and test descriptor
- `pack/` — the canonical packwiz manifest
- `build-logic/` — small, composable Gradle convention plugins
- `tools/bertie-ci/` — provider-neutral build, test, instance, and release tooling
- `.github/` — thin GitHub Actions adapters around `bertie-ci`

`bertie-mod-atlas`, `bertie-progression-planning`, `bertie-workspace`, the
organisation `.github` repository, and the upstream-tracking
`Nekos-Enchanted-Books` fork intentionally remain separate.

## Development environment

Install Git and [Nix](https://nixos.org/download/), then enter the pinned environment:

```bash
git clone git@github.com:bertie-mc/bertie.git
cd bertie
nix develop
```

Nix supplies JDK 21, Gradle 8.14.4, Python, packwiz, GitHub tooling, and the pinned
HeadlessMC runtime dependencies. The repository deliberately has no Gradle wrapper and
does not download toolchains through Gradle.

Build and tests are separate operations:

```bash
gradle :mods:carving:assemble
gradle :mods:carving:test
bertie-ci gametest --workspace . --component carving
```

Project-owned suites live in each component's `bertie-ci.toml`. To inspect or run the
same affected-component plan used by CI:

```bash
bertie-ci plan --workspace . --all
nix flake check
```

This machine does not need a desktop. Linux client integration tests launch the real
Minecraft client under Xvfb; server tests run without a GUI. Full-pack client and server
suites are deliberately scheduled or manually dispatched because the pack is large.

See [bertie-ci](tools/bertie-ci/README.md) for the test model and
[the pack README](pack/README.md) for pack maintenance.

## Releases and history

Components keep independent versions. New signed tags have the exact form
`subject/vX.Y.Z`, for example `carving/v1.2.0`, `pack/v0.2.0`, or
`bertie-ci/v5.0.0`. Releases are started manually after the required pipelines are green
for the commit being tagged.

The former component repositories remain available as read-only archives so their
historical tags, release assets, and links continue to work. Their imported, rewritten
history is documented in [the migration record](docs/polyrepo-migration.md).

## Licensing

Licensing is component-specific. Consult the `UNLICENSE`, `LICENSE`, and `NOTICE`
files inside each component; no repository-root licence overrides them.
