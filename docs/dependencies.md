# Managing dependencies

Enter the development shell before running commands from this page:

```bash
nix develop
```

## Choose the file

| Dependency | File |
| --- | --- |
| Java library, Gradle plugin, Minecraft, NeoForge, or shared tool version | [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) |
| Third-party mod, datapack, resourcepack, or shaderpack | [`gradle/minecraft-artifacts.toml`](../gradle/minecraft-artifacts.toml) |
| Another project in this repository | The consumer's `build.gradle.kts` |
| Owned mod version | The mod's `mod.properties` |
| Pack version | [`pack/pack.properties`](../pack/pack.properties) |

Use the generated `libs` catalog for entries in `libs.versions.toml` and the generated
`mods` catalog for entries in `minecraft-artifacts.toml`.

## Add or update a third-party file

Add one table to `gradle/minecraft-artifacts.toml`. Include every provider that publishes
the selected file:

```toml
[mods.create]
maven = { module = "com.simibubi.create:create-1.21.1", version = "6.0.10-281" }
modrinth = { project-id = "LNytGWDc", version-id = "UjX6dr61", filename = "create-1.21.1-6.0.10.jar" }
curseforge = { slug = "create", project-id = 328085, file-id = 7963363 }
```

Use release-specific IDs and the exact filename from the provider. Gradle prefers the
Maven coordinate for development. Generated packwiz metadata uses Modrinth when available
and otherwise CurseForge.

Some projects publish pack-only data through loader-compatible JARs. If either Modrinth or
CurseForge does not offer a genuine standalone pack distribution, keep the artifact under
`[mods.*]` and mark the artifact itself as `fakePack = true`:

```toml
[mods.example-worldgen]
fakePack = true
modrinth = { project-id = "example", version-id = "jar-release", filename = "example.jar" }
curseforge = { slug = "example-worldgen", project-id = 123, file-id = 456 }
```

This deliberately chooses the mod-container release even if one provider also offers a `.zip`,
so switching providers cannot change how the artifact is installed. `fakePack` documents why a
pack-only archive appears in `mods/` and lets the pack classification test distinguish it from an
accidentally misclassified mod. It also fits CurseForge `manifest.json` semantics: the export
retains the project and file IDs and lets the launcher derive the upstream file type and location.

The table name becomes its `mods` alias only for entries under `[mods.*]`. For example,
`[mods.slag-n-embers]` becomes `mods.slagNEmbers`. Put standalone pack archives under
`[datapacks.*]`, `[resourcepacks.*]`, or `[shaderpacks.*]`. Ordinary pack distributions are
installed into the matching top-level Minecraft directory and are not exposed as Java
dependencies. Use those tables only when every declared provider is a genuine standalone pack.

### Restrict a file to one physical side

Omit `side` when a file can load on both client and dedicated server. Set it only for a
file that cannot load on one side:

```toml
[mods.tweakerge]
side = "client"
modrinth = { project-id = "yke6wdGF", version-id = "701i2Xre", filename = "tweakerge-0.4.3+mc1.21.1.jar" }
curseforge = { slug = "tweakerge", project-id = 915857, file-id = 7971130 }
```

Allowed values are `client`, `server`, and `both`; omission means `both`. This field is
for third-party files. Bertie-owned mods must load safely on both physical sides.

## Use a dependency in a project

Reference the generated alias in `build.gradle.kts`:

```kotlin
dependencies {
    implementation(mods.create)
    compileOnly(mods.emi)
    clienttestRuntimeOnly(mods.emi)
}
```

Choose the configuration that matches where the dependency is needed:

| Configuration | Use |
| --- | --- |
| `implementation`, `compileOnly`, `runtimeOnly` | Production code |
| `testImplementation`, `testRuntimeOnly` | JUnit only |
| `gametestImplementation`, `gametestRuntimeOnly` | GameTests only |
| `clienttestImplementation`, `clienttestRuntimeOnly` | Client tests only |

Suite configuration names describe test scope, not physical installation side.

Use project dependencies for owned mods:

```kotlin
dependencies {
    implementation(project(":mods:bertie-tiers"))
}
```

Add an owned mod to the pack with `packMods` in
[`pack/build.gradle.kts`](../pack/build.gradle.kts):

```kotlin
dependencies {
    packMods(project(":mods:bertie-tiers"))
}
```

## Update locks and checksums

After an intentional dependency change, regenerate the lock files and SHA-256 entries:

```bash
gradle resolveAndLockAll --write-locks --write-verification-metadata sha256
```

Review what changed before committing:

```bash
git diff -- '**/gradle.lockfile' '**/settings-gradle.lockfile' \
  gradle/verification-metadata.xml
```

The diff should contain only the modules and artifacts introduced by your change. Do not
approve an unexplained checksum.

Run the affected project's tests. For a pack dependency, also run:

```bash
gradle :pack:runGameTests
gradle :pack:runClientTests
bertie-ci pack-validate --workspace . --component pack
```

## Generate and inspect the pack

```bash
gradle :pack:generatePackwiz
```

Inspect `pack/build/packwiz`. Generated packwiz metadata, downloaded files, and owned mod
JARs are build output and must not be added to Git.

Validate or export the generated pack with:

```bash
bertie-ci pack-validate --workspace . --component pack
bertie-ci pack-export-client --workspace . --component pack \
  --output .bertie-ci/release/bertie.mrpack
bertie-ci pack-export-server --workspace . --component pack \
  --output .bertie-ci/release/bertie-server.zip
```

See [Testing](testing.md) for choosing tests and [CI and releases](cicd.md) for publishing
the result.
