# Bertie modpack

An exploration, technology, and magic modpack for Minecraft 1.21.1 on NeoForge. The
roughly 500-mod manifest is managed with
[packwiz](https://packwiz.infra.link/).

## Installing

New client and server exports are published on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases) under
`pack/vX.Y.Z` tags. Releases made before the monorepo migration remain on the
[legacy release page](https://github.com/bertie-mc/bertie-pack/releases).

For a client, download `bertie-pack-<version>.mrpack` and import it with Prism
Launcher, the Modrinth App, or another launcher that supports Modrinth packs. Allocate
8–12 GB of RAM.

For a server, download `bertie-server-<version>.zip`, extract it, and supply Java 21:

```bash
unzip bertie-server-<version>.zip
cd bertie-server-<version>
./start.sh
```

The archive intentionally contains no third-party mod JARs. Its pack manifest and
installer download the server-side files on first run. Accept the Minecraft EULA in
`eula.txt` before starting the server.

## Developing the manifest

From the monorepo root, `nix develop` supplies the pinned packwiz and test tools.

```bash
nix develop
packwiz --help
```

Third-party mods are ordinary packwiz entries:

```bash
cd pack
packwiz modrinth add <exact-slug>
packwiz curseforge add <exact-slug>
packwiz refresh
```

Use exact slugs, verify the generated `side`, and inspect the diff for unrelated version
changes. Always commit `index.toml` and `pack.toml` with the manifest files they index.
The FTB family is a tested compatibility set; in particular, `ftb-xmod-compat` remains
pinned to 21.1.8 because newer builds require newer FTB Library and Quests versions than
this pack currently uses.

### Bertie-owned mods

Owned mod source lives in [`../mods`](../mods). The pack consumes released JARs through
fixed URL, filename, and SHA-256 metadata. There is deliberately no monorepo-wide packwiz
GitHub updater.

After releasing an owned mod:

1. Copy the release asset URL and filename into its `mods/<subject>.pw.toml`.
2. Calculate and update the exact SHA-256 hash.
3. Keep the physical `side` unchanged unless the mod itself changed sides.
4. Run `packwiz refresh` and review the complete diff.

Never place a built JAR in `pack/mods`. Local full-pack tests overlay current workspace
artifacts only inside ignored, ephemeral test instances; those artifacts are not manifest
inputs.

`explode-to-mine` is shipped but its explosion and ore balance remains provisional.
`Nekos-Enchanted-Books` is intentionally not included.

## Validation and integration tests

The project-owned suites are declared in [`bertie-ci.toml`](bertie-ci.toml):

- `manifest` checks that packwiz hashes are current, sides and filenames are valid, and
  no JAR is tracked.
- `world-join` installs the client pack, overlays current monorepo mod builds, and joins
  an integrated world under Xvfb.
- `readiness` installs the server pack, overlays only server-applicable workspace mods,
  and runs the command scenario in
  [`tests/runtime/server-readiness.json`](tests/runtime/server-readiness.json).

The same provider-neutral commands run locally:

```bash
bertie-ci pack-validate --workspace . --component pack
bertie-ci build --workspace . --all-mods --output-dir .bertie-ci/artifacts
bertie-ci prepare-pack-instance --workspace . --component pack \
  --side server --output-dir .bertie-ci/pack-server
bertie-ci overlay-components --workspace . --all-mods \
  --instance .bertie-ci/pack-server/instance.json \
  --artifact-dir .bertie-ci/artifacts --pack-component pack
bertie-ci server-test --instance .bertie-ci/pack-server/instance.json \
  --command-test pack/tests/runtime/server-readiness.json
```

GitHub Actions provides thin adapters:

- `check.yml` validates the manifest on relevant changes.
- `pack-client.yml` runs the full client suite nightly or on manual dispatch.
- `pack-server.yml` runs the full server suite nightly or on manual dispatch.
- `release.yml` independently exports the client and server artifacts.

The full runtime suites are intentionally not automatic merge gates because this pack is
large. Before releasing, manually run both against the exact commit and confirm their
results.

## Releasing

Bump `version` in `pack.toml`, refresh the manifest, commit, and confirm the required
pipelines for that commit. With SSH signing configured:

```bash
git tag -s pack/v0.2.0 -m "Release pack v0.2.0"
git push origin pack/v0.2.0
```

The tag must match `pack/vX.Y.Z` exactly and its version must equal `pack.toml`.

## Versions and licensing

- Minecraft 1.21.1
- NeoForge 21.1.233
- Java 21

The pack manifest, configs, and quest data are dedicated to the public domain under
[The Unlicense](UNLICENSE). The source tree contains no mod JARs. Client exports may
embed third-party mods that the Modrinth pack format cannot reference by URL; every mod
retains its author's licence. Review the export log and [NOTICE](NOTICE) before publishing.
