# Managing dependencies

Enter the development shell before running the commands on this page:

```bash
nix develop
```

## Which file to edit

| Dependency | File |
| --- | --- |
| Java library, Gradle plugin, Minecraft, NeoForge, or shared tool version | [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) |
| Third-party mod or shaderpack | [`gradle/minecraft-artifacts.toml`](../gradle/minecraft-artifacts.toml) |
| Another project in this repository | The consumer's `build.gradle.kts` |
| Owned mod version | The mod's `mod.properties` |
| Pack version | [`pack/pack.properties`](../pack/pack.properties) |

## Add or update a third-party mod

Add one table to `gradle/minecraft-artifacts.toml`. Record every provider that publishes
the selected file:

```toml
[mods.create]
maven = { module = "com.simibubi.create:create-1.21.1", version = "6.0.10-281" }
modrinth = { project-id = "LNytGWDc", version-id = "UjX6dr61", filename = "create-1.21.1-6.0.10.jar" }
curseforge = { slug = "create", project-id = 328085, file-id = 7963363 }
```

Use release-specific IDs and the exact filename shown by the provider. Gradle uses the
Maven coordinate when present, then Modrinth, then CurseForge. Generated packwiz files
use Modrinth when present and otherwise use CurseForge.

The table name becomes an alias in the generated `mods` catalog. Dashes are converted to
camel case, so `[mods.slag-n-embers]` is available as `mods.slagNEmbers`.

Every `[mods.*]` entry is included in the full pack. Put shaderpack archives under
`[shaderpacks.*]` instead.

### Client-only and server-only files

Omit `side` for a mod that can be installed on both physical sides. Use a lowercase value
only when the selected file cannot be loaded on one side:

```toml
[mods.tweakerge]
side = "client"
modrinth = { project-id = "yke6wdGF", version-id = "701i2Xre", filename = "tweakerge-0.4.3+mc1.21.1.jar" }
curseforge = { slug = "tweakerge", project-id = 915857, file-id = 7971130 }
```

Allowed values are `client`, `server`, and `both`; omission is equivalent to `both`.
Client runs receive `client` and `both` files. Dedicated-server runs receive `server` and
`both` files. The generated packwiz metadata carries the same installation-side setting.

Do not create client/server dependency configurations in component builds. The manifest
field is for third-party files with a physical-side restriction. Bertie-owned mods must
load safely on both sides and select their entrypoints and mixins through NeoForge.

## Use a third-party mod from an owned component

Reference the generated alias in the component's `build.gradle.kts`:

```kotlin
dependencies {
    compileOnly(mods.emi)
    implementation(mods.create)

    clienttestRuntimeOnly(mods.emi)
}
```

Choose the normal Gradle configuration for the code relationship:

| Configuration | Use |
| --- | --- |
| `implementation` | Production code compiles and runs against the dependency |
| `compileOnly` | Production code compiles against an optional integration |
| `runtimeOnly` | The dependency is needed only at runtime |
| `testImplementation` / `testRuntimeOnly` | JUnit suite only |
| `gametestImplementation` / `gametestRuntimeOnly` | GameTest suite only |
| `clienttestImplementation` / `clienttestRuntimeOnly` | Client-test suite only |

Suite configuration names describe where a dependency is needed; they do not declare its
physical side.

Owned mods use project dependencies:

```kotlin
dependencies {
    implementation(project(":mods:bertie-tiers"))
}
```

Add an owned mod to the pack with `packMods` in [`pack/build.gradle.kts`](../pack/build.gradle.kts):

```kotlin
dependencies {
    packMods(project(":mods:bertie-tiers"))
}
```

Pack generation builds that project and copies its JAR into the pack. It does not need a
GitHub release URL for an owned mod.

## Update locks and checksums

After an intentional dependency change, regenerate locks and SHA-256 verification data:

```bash
gradle resolveAndLockAll --write-locks --write-verification-metadata sha256
```

Review the result before committing it:

```bash
git diff -- '**/gradle.lockfile' '**/settings-gradle.lockfile' \
  gradle/verification-metadata.xml
```

The diff should contain only the modules and artifacts caused by the change. Gradle
verifies downloaded JARs and other artifact files. Mutable POM and Gradle Module Metadata
files are not checksum-verified; dependency locks still fix the resolved module graph.

If Gradle reports an unapproved checksum, first confirm that the provider file and version
ID are the ones you intended to use. Do not approve an unexplained file change.

Run the affected component tests after updating the lock state. For a pack dependency,
run both physical-runtime suites and pack validation:

```bash
gradle :pack:runGameTests
gradle :pack:runClientTests
bertie-ci pack-validate --workspace . --component pack
```

See [Testing](testing.md) for choosing a smaller component suite.

## Generate and inspect the pack

Generate the packwiz tree with:

```bash
gradle :pack:generatePackwiz
```

Inspect the output under `pack/build/packwiz`. It contains provider metafiles for
third-party artifacts, locally built Bertie JARs, and files from `pack/config`.

Generated `pack.toml`, `index.toml`, `.pw.toml` files, downloaded artifacts, and owned
JARs are build output. Do not add them to Git.

Validate or export the generated pack through `bertie-ci`:

```bash
bertie-ci pack-validate --workspace . --component pack
bertie-ci pack-export-client --workspace . --component pack \
  --output .bertie-ci/release/bertie.mrpack
bertie-ci pack-export-server --workspace . --component pack \
  --output .bertie-ci/release/bertie-server.zip
```

See [CI/CD](cicd.md) for CI jobs and releases.
