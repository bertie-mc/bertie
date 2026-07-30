# bertie

A **NeoForge 1.21.1** modpack: the S1 progression questline layered on top of a large
exploration / tech / magic mod set.

**490 mods.** Managed with [packwiz](https://packwiz.infra.link/).

---

## Installing

### Client

Download `bertie-<version>.mrpack` from the [Releases page](../../releases) and import it
with a launcher that understands the Modrinth pack format — [Prism
Launcher](https://prismlauncher.org/), the Modrinth App, or ATLauncher.

Allocate **8–12 GB** of RAM. This is a large pack; 4 GB will not boot it comfortably.

### Server

Download `bertie-server-<version>.zip` from the Releases page, unzip it, and run the
included start script. It ships the server-side mods, the configs, and a NeoForge
installer step — you supply Java **21**.

```bash
unzip bertie-server-<version>.zip -d bertie-server
cd bertie-server
./start.sh          # start.bat on Windows
```

Accept the EULA in `eula.txt` on first run.

---

## Developing the pack

You need the [packwiz CLI](https://packwiz.infra.link/installation/) on your PATH.

```bash
packwiz modrinth add <slug>          # add a Modrinth mod
packwiz curseforge add <slug>        # add a CurseForge mod
packwiz github add bertie-mc/<repo>  # add one of bertie's own mods
packwiz update --all                 # update everything not pinned
packwiz refresh                      # rebuild index.toml — run after ANY change
```

**Always `packwiz refresh` and commit `index.toml` together with whatever you changed.**
An index whose hashes do not match the tree is worse than no index at all.

### bertie's own mods come from GitHub Releases

The mods written for this pack live in their own repositories under
[`bertie-mc`](https://github.com/bertie-mc) and are consumed here as tagged releases:

| Mod | Repo |
|---|---|
| `bertie_s1` | [bertie-s1](https://github.com/bertie-mc/bertie-s1) — the S1 progression content |
| `berlords_carving` | [carving](https://github.com/bertie-mc/carving) |
| `berlords_emi` | [berlords-emi](https://github.com/bertie-mc/berlords-emi) |
| `bertie_filters` | [bertie-filters](https://github.com/bertie-mc/bertie-filters) |
| `bertie_weapons` | [bertie-weapons](https://github.com/bertie-mc/bertie-weapons) |
| `bertie_blackhole` | [bertie-blackhole](https://github.com/bertie-mc/bertie-blackhole) |
| `forgeink` | [forge-ink](https://github.com/bertie-mc/forge-ink) |
| `hephaestusarchitecture` | [hephaestus-architecture](https://github.com/bertie-mc/hephaestus-architecture) |
| `fdshaderfix` | [fd-shader-fix](https://github.com/bertie-mc/fd-shader-fix) |
| `frozenregfix` | [frozen-reg-fix](https://github.com/bertie-mc/frozen-reg-fix) |

**Never drop a built jar into `mods/`.** Release it from its own repo and
`packwiz github add` it. A loose jar and a metafile can resolve to the same filename,
which makes the index list one path twice and installs two versions of the same mod.

---

## CI

| Workflow | When | What it does |
|---|---|---|
| `validate.yml` | every push and PR | index is current, every download resolves, no duplicate targets, every file has a `side` |
| `server-boot.yml` | PRs and nightly | installs NeoForge, resolves the server-side pack, boots a dedicated server headlessly and requires it to reach `Done (` |
| `release.yml` | tag `v*` | exports the client `.mrpack` and a server zip, attaches both to a GitHub Release |

A green `validate` means the pack is internally consistent and every mod is still
downloadable. A green `server-boot` means the server half actually starts with all 490
mods loaded — that is the test that catches a mod which resolves but crashes on load.

---

## Releasing

```bash
# bump `version` in pack.toml, commit, then:
git tag -a v0.2.0 -m "Release v0.2.0"
git push origin v0.2.0
```

`release.yml` builds and publishes both artifacts.

---

## Versions

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.233 |
| Java | 21 |

---

## Licence

The pack manifest, configs and quest data in this repository are dedicated to the public
domain under [The Unlicense](UNLICENSE).

**This repository contains no mod jars.** Every mod is downloaded at install time from
Modrinth, CurseForge, or its own GitHub Release, and each remains under its own author's
licence. See [NOTICE](NOTICE).
