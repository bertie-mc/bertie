# Polyrepo migration record

The public Bertie product sources moved to this repository on 2026-08-01. The former
repositories remain archived so historical commits, pull requests, releases, and pack
download URLs continue to resolve.

## Import policy

Each default-branch history was imported beneath its final component directory. File
contents, author names, author dates, and commit messages were retained with two explicit
corrections requested by the maintainers:

- `dinex435@gmail.com` was corrected to `berlord@rambler.ru` for author and committer
  identities.
- `Co-Authored-By` trailers naming Claude were removed.

Those changes and the path prefix necessarily changed commit IDs and invalidated existing
commit signatures. Rewritten historical commits were not re-signed as another person.
The original signed objects remain in the archived repositories. Every new migration and
integration commit was signed using the committer's configured SSH key.

Historical release tags and assets remain in the archived repositories. New releases use
repository-wide namespaced tags in the exact form `<subject>/vX.Y.Z`.

## Frozen source revisions

| Former repository | Imported revision | Destination |
|---|---|---|
| `berlords-emi` | `514abd3320c2cda3c1a34ef31185010585df6e5a` | `mods/berlords-emi` |
| `berlords-food-system` | `5a21b5fcfa22ea3489ab6810a28a2094f43a026e` | `mods/berlords-food-system` |
| `bertie-blackhole` | `234b1accf210d38f486290e5014578e8be13271d` | `mods/bertie-blackhole` |
| `bertie-ci` | `58a81559f91ad442a95128f23f6948685c7fb04d` | `tools/bertie-ci` |
| `bertie-filters` | `6c4d48835f8078f459ef1191417b616281a24659` | `mods/bertie-filters` |
| `bertie-pack` | `f551bd10b253d8371068a9971796c8f5502094a2` | `pack` |
| `bertie-progression` | `416939b444a5e553db9070c545e26591eb596982` | `mods/bertie-progression` |
| `bertie-tiers` | `330df0ec041594813721494b6d5d8667b5c436f7` | `mods/bertie-tiers` |
| `bertie-weapons` | `9e8aafb1ceffb4a79c845b4cf193b82e6d27b8c7` | `mods/bertie-weapons` |
| `bush-tweaks` | `aac66275989df8977ace8814c26003f47c4f6157` | `mods/bush-tweaks` |
| `carving` | `8ed879e470d4b1feca70e5f39434e66f7ab57e48` | `mods/carving` |
| `ender-eyes` | `de0e542cfb23a50663452568564b840724f85fda` | `mods/ender-eyes` |
| `explode-to-mine` | `b01572fd208a6f916c3a04297174ba111d4d9e6c` | `mods/explode-to-mine` |
| `explosive-enhancement` | `cd1b8811af1481f5112b4504b42eafe1d6cfe56d` | `mods/explosive-enhancement` |
| `fart-bomb` | `0b307f72ff609ed689895d486698914c054fdfa2` | `mods/fart-bomb` |
| `fd-shader-fix` | `378f8e7d0f52fdb2af91ac2a19a40ba395cdfb95` | `mods/fd-shader-fix` |
| `forge-ink` | `819a54a9d772d2e2fea66b26228f8d0647619e7e` | `mods/forge-ink` |
| `frozen-reg-fix` | `40714e5b4af108547909bd3bf409550cccc5cb4d` | `mods/frozen-reg-fix` |
| `hephaestus-architecture` | `6d84bf4748fc8ffb75787a4ec56a616e7015eee7` | `mods/hephaestus-architecture` |
| `primitive-refined` | `c69f73be3c0cfd194d266f4b021d390be1ebd340` | `mods/primitive-refined` |
| `rustic-engineer-fix` | `c878d464f91bf4f1c517aba267a34da502ad0fee` | `mods/rustic-engineer-fix` |
| `short-circuit-fix` | `78e82917befd7b54667ed652658dd56a3b52d8d9` | `mods/short-circuit-fix` |
| `withered-hearts` | `53738dc6afc767b9053cfb7d6bf9c3ca85569df7` | `mods/withered-hearts` |

## Deliberately separate repositories

- `bertie-mod-atlas`
- `bertie-progression-planning`
- `bertie-workspace`
- `Nekos-Enchanted-Books`, which remains in its upstream fork network
- `.github`, which provides organization-wide community health files and the profile
