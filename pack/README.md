# Bertie modpack

An exploration, technology, and magic modpack for Minecraft 1.21.1 on NeoForge.

## Installing

Client and server exports are published on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases) under
`pack/vX.Y.Z` tags.

For a client, import `bertie-pack-<version>.mrpack` with Prism Launcher, the Modrinth
App, or another Modrinth-pack-compatible launcher. Allocate 8–12 GB of RAM.

For a server, extract `bertie-server-<version>.zip`, supply Java 21, accept the EULA in
`eula.txt`, and run `start.sh`. The archive contains the generated side-aware pack tree,
the locally built Bertie mod JARs, and an installer for external dependencies.

## Dependency model

[`gradle/minecraft-artifacts.toml`](../gradle/minecraft-artifacts.toml) is the
authoritative manifest for third-party Minecraft artifacts. Each logical mod, datapack,
resourcepack, or shaderpack groups its exact Maven, Modrinth, and CurseForge coordinates:

```toml
[mods.create]
maven = { module = "com.simibubi.create:create-1.21.1", version = "6.0.10-281" }
modrinth = { project-id = "LNytGWDc", version-id = "UjX6dr61", filename = "create-1.21.1-6.0.10.jar" }
curseforge = { slug = "create", project-id = 328085, file-id = 7963363 }
```

Provider selection is global: Gradle uses Maven, then Modrinth, then CurseForge;
packwiz generation uses Modrinth, then CurseForge. Provider records have no per-artifact
source selectors. A logical artifact may declare lowercase `side = "client"`, `"server"`,
or `"both"`; omission means `"both"`.

Pack-only projects stay under `[mods.*]` with `fakePack = true` when either Modrinth or CurseForge
lacks a genuine standalone pack release. In that case the manifest selects loader-compatible JAR
releases consistently on every provider. The flag records that the artifact intentionally has no
executable code while preserving provider portability and future CurseForge `manifest.json`
exports.

The settings plugin exposes selected modules through the provider-neutral `mods` catalog;
component build files do not repeat side buckets. Build logic centrally derives physical
runtimes: clients receive `client` and `both` third-party artifacts, while dedicated
servers receive `server` and `both`. Owned project artifacts remain present in both and
must use NeoForge entrypoints and mixins safely.

Standalone datapacks and resourcepacks are staged in the instance-root `datapacks/` and
`resourcepacks/` directories and loaded globally by Paxi. These packs do not appear on Java
classpaths. Fake pack mods are instead loaded from `mods/`, so one archive can expose both its
data and assets without being duplicated into both pack directories.

The pack's JUnit suite also inspects every resolved third-party `[mods.*]` archive. An
archive with `pack.mcmeta` and only `data/` or `assets/` content fails the build unless it
contains executable classes itself or in a nested JAR, or its `[mods.*]` artifact explicitly sets
`fakePack = true`. This keeps intentional pack-only mod containers visible without hiding newly
misclassified dependencies.

The full-pack GameTest launch verifies the server projection on a physical dedicated
server. An incorrectly classified dependency is reclassified, fixed, replaced, or removed.

Bertie-owned mods are ordinary Gradle project dependencies in
[`build.gradle.kts`](build.gradle.kts). Tests therefore exercise the local builds, and
pack generation copies those exact project JARs directly into the generated tree. They
are the owned artifacts used by both export formats; packaging does not consult separately
published releases.

Pack-owned installation files live in [`config/`](config/). Pack identity and release
version live in [`pack.properties`](pack.properties). The complete packwiz tree—including
`pack.toml`, `index.toml`, third-party metafiles, locally built owned-mod JARs, and
configuration—is generated under `build/`:

```bash
gradle :pack:generatePackwiz
```

Generated packwiz files are outputs and are never checked in. Gradle does not invoke the
packwiz executable.

## Changing dependencies

When adding or updating a third-party dependency:

1. Add or update its logical table in `gradle/minecraft-artifacts.toml`, recording the
   exact coordinates for each available provider.
2. Resolve and review Gradle dependency locks and checksum verification metadata.
3. Run `gradle :pack:generatePackwiz` and inspect `pack/build/packwiz` when packaging
   metadata changed.
4. Run the relevant pack tests and `bertie-ci pack-validate`.

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

Packaging commands first ask Gradle to generate the tree, then let `bertie-ci` supervise
packwiz validation or export:

```bash
bertie-ci pack-validate --workspace . --component pack
bertie-ci pack-export-client --workspace . --component pack \
  --output .bertie-ci/release/bertie.mrpack
bertie-ci pack-export-server --workspace . --component pack \
  --output .bertie-ci/release/bertie-server.zip
```

Both exports consume the same deterministic side-aware packwiz projection. The client
command emits a Modrinth pack; the server command wraps the generated tree with its
bootstrap installer and start scripts.

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
- NeoForge 21.1.233
- Java 21

The pack declarations, configs, and quest data are dedicated to the public domain under
[The Unlicense](UNLICENSE). Every downloaded mod and shaderpack retains its author's
licence. Review the export log and [NOTICE](NOTICE) before publishing.
