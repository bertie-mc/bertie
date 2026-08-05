# Bertie

Bertie is an exploration, technology, and magic modpack for Minecraft 1.21.1 on
NeoForge. This monorepo contains the pack, its custom mods, shared build and testing
infrastructure, and the tools used to validate and release them.

## Getting started

Install Git and [Nix](https://nixos.org/download/), then enter the pinned development
environment:

```bash
git clone git@github.com:bertie-mc/bertie.git
cd bertie
nix develop
```

The environment supplies the repository's Java, Gradle, Python, packwiz, Wayland, and
GitHub tooling. Run `nix flake check` to verify the development environment itself.

## Documentation

- [Dependencies](docs/dependencies.md) — add or update libraries, mods, shaderpacks, lock
  files, checksums, and generated pack contents.
- [Testing](docs/testing.md) — choose a suite, write tests, run them locally, and inspect
  Minecraft failures.
- [CI/CD](docs/cicd.md) — preview affected tasks, reproduce CI checks, export the pack, and
  publish releases.
- [Pack maintenance and installation](pack/README.md) — the `pack` component.
- [bertie-ci command reference](tools/bertie-ci/README.md).

Each owned mod also has a component README describing its behavior and development notes.

## Repository layout

- `mods/` — owned NeoForge mods;
- `pack/` — the full-pack Gradle test and packaging project;
- `core/` — Minecraft input preparation and reusable in-game test APIs/drivers;
- `build-logic/` — Gradle convention plugins and pack generation;
- `tools/bertie-ci/` — CI planning, task supervision, Wayland provisioning, and release
  packaging;
- `docs/` — developer documentation;
- `.github/` — workflows and reusable actions that invoke Gradle and `bertie-ci`.

`bertie-mod-atlas`, `bertie-progression-planning`, the organisation `.github`
repository, and the `Nekos-Enchanted-Books` fork are maintained separately.

## Licensing

Licensing is component-specific. Consult the `UNLICENSE`, `LICENSE`, and `NOTICE` files
inside each component; no repository-root licence overrides them.
