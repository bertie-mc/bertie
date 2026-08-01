# Bertie

Bertie is a Minecraft 1.21.1 NeoForge modpack and its custom mods. This repository is
the public source of truth for the pack, the mods maintained specifically for it, their
shared Gradle build logic, and the local-first test tooling used by CI.

The repository is being assembled from the former `bertie-mc` polyrepo. Until the
migration is declared complete, the existing component repositories remain the release
sources of record.

## Layout

- `mods/` — independently versioned NeoForge mods
- `pack/` — the packwiz source
- `build-logic/` — shared Kotlin Gradle convention plugins
- `tools/bertie-ci/` — portable build and integration-test orchestration

Private planning material, `bertie-mod-atlas`, and the upstream-tracking
`Nekos-Enchanted-Books` fork intentionally remain separate repositories.

## Licensing

Licensing is component-specific. Consult the `UNLICENSE`, `LICENSE`, and `NOTICE` files
inside each component; no repository-root licence overrides them.
