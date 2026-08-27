# Bertie modpack

An exploration, technology, and magic modpack for Minecraft 1.21.1 on NeoForge.

## Installing

Client and server exports are published on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases) under
`pack/vX.Y.Z` tags.

For a client, import `bertie-pack-<version>.mrpack` with Prism Launcher, the Modrinth
App, or another Modrinth-pack-compatible launcher. Allocate 8–12 GB of RAM.
Alternatively, import `bertie-pack-<version>-curseforge.zip` with the CurseForge App.

For a server, extract `bertie-server-<version>.zip`, supply Java 21, accept the EULA in
`eula.txt`, and run `start.sh`. The archive contains the generated side-aware pack tree,
the locally built Bertie mod JARs, and an installer for external dependencies.

## Dependency model

Each file under [`deps/components/`](../deps/components) records one intentional logical
component and its exact equivalent distributions. Profiles select a
development or release representation; generated locks contain the complete transitive
graph, including optional and missing-optional edges.

```toml
[distributions.modrinth-mod]
provider = "modrinth"
kind = "mod"
project-id = "LNytGWDc"
version-id = "UjX6dr61"
filename = "create-1.21.1-6.0.10.jar"
```

The settings plugin exposes component aliases through the provider-neutral `deps` catalog.
Aliases have no effect until a build script consumes them. This project explicitly lists
its external roots with `packComponents(deps.example)`; dependency-only libraries are
reachable lock entries rather than repeated roots. The full test suites explicitly inherit
`packComponents`, while their compile classpaths stay narrow.

Development prefers loader-compatible mod distributions. `release-modrinth` and
`release-curseforge` prefer files native to their target platform, then native packs.
When a selected datapack/resourcepack needs global loading, its profile adds Paxi as an
ordinary conditional transitive edge.

The full-pack GameTest launch verifies the server projection on a physical dedicated
server. An incorrectly classified dependency is reclassified, fixed, replaced, or removed.

Bertie-owned mods are ordinary Gradle project dependencies in
[`build.gradle.kts`](build.gradle.kts). Tests therefore exercise the local builds, and
pack generation copies those exact project JARs directly into the generated tree. They
are the owned artifacts used by both export formats; packaging does not consult separately
published releases.

Pack-owned installation files live in [`config/`](config/). Pack identity and release
version live in [`pack.properties`](pack.properties). Generate the direct client archive
or the packwiz conversion used by validation/server packaging with:

```bash
gradle :pack:generateMrpack
gradle :pack:generateCurseForgePack
gradle :pack:generatePackwiz
```

Generated archives, packwiz files, and redistribution audits under `build/` are outputs
and are never checked in. Gradle does not invoke the packwiz executable.

## Changing dependencies

When adding or updating a third-party dependency:

1. Add or update its file under `deps/components/`, including its exact distributions.
2. Declare the intended consumer in this or an owned project's build script.
3. Run `bertie-ci deps-lock` and `bertie-ci deps-check`; review the profile-lock diff.
4. Resolve and review Gradle locks and checksum verification metadata.
5. Generate the relevant release output and run the pack tests/validation.

Do not edit generated `.pw.toml`, `index.toml`, `pack.toml`, or JAR files. Do not invoke
packwiz from Gradle.

## Validation and integration tests

`:pack` resolves both physical-runtime projections directly with Gradle. GameTests receive
the server projection and client tests receive the client projection; tests never consume
the generated packwiz tree.

```bash
gradle :pack:runGameTests
gradle :pack:runClientTests
gradle :pack:runTests
```

The GameTest suite runs in a dedicated-server process. The client suite runs in a client
process and can create an in-process dedicated server through the Java test driver. Local
client tests use the current desktop; Linux CI runs the same Gradle task in an isolated
native-Wayland session supplied by `bertie-ci`.

Packaging commands ask Gradle for the selected locked projection. Client exports copy the
direct Gradle-generated platform archives; validation and server export use packwiz
conversion:

```bash
bertie-ci pack-validate --workspace . --component pack
bertie-ci pack-export-client --workspace . --component pack \
  --output .bertie-ci/release/bertie.mrpack
bertie-ci pack-export-curseforge --workspace . --component pack \
  --output .bertie-ci/release/bertie-curseforge.zip
bertie-ci pack-export-server --workspace . --component pack \
  --output .bertie-ci/release/bertie-server.zip
```

The Modrinth and packwiz/server outputs consume `release-modrinth`; the CurseForge import
archive consumes `release-curseforge`. Both profiles start from the same intentional
component roots and choose equivalent immutable distributions. The server command wraps
the generated tree with its bootstrap installer and start scripts.

## Releasing

Bump `version` in [`pack.properties`](pack.properties) and confirm the required test and
packaging pipelines for that commit. With SSH signing configured:

```bash
git tag -s pack/v0.2.0 -m "Release pack v0.2.0"
git push origin pack/v0.2.0
```

The tag must match `pack/vX.Y.Z` exactly and its version must equal the `version` in
`pack.properties`.

## Versions and licensing

- Minecraft 1.21.1
- NeoForge 21.1.235
- Java 21

The pack declarations, configs, and quest data are dedicated to the public domain under
[The Unlicense](UNLICENSE). Every downloaded mod and shaderpack retains its author's
licence. Review the export log and [NOTICE](NOTICE) before publishing.
