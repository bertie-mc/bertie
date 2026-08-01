# Runtime fixtures

Fixture selectors resolve directly to metafile basenames in the canonical
[`pack/mods`](../../../pack/mods) tree. For example, `--fixture create` selects
`pack/mods/create.pw.toml` without an explicit profile.

[`profiles.json`](profiles.json) is only for aggregate dependency closures. A selector
such as `artifacts` maps to both `artifacts` and `curios`; one-to-one aliases belong
in neither the catalog nor Python. [`defaults.json`](defaults.json) lists canonical mods
installed for every fixture instance on a side.

Packwiz metafiles do not describe dependency closure. Add a profile only when a target
needs other top-level pack mods to load, include every required entry, and prove it with
the smallest relevant client or server suite.

The fixture pack is built from the same monorepo source by Nix, so updating pack metadata
and `flake.lock` is sufficient. Run `nix flake check` before publishing a
`bertie-ci/vX.Y.Z` tag.
