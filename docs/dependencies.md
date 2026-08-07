# Managing dependencies

Enter the development shell before running commands from this page:

```bash
nix develop
```

## Dependency ownership

| Dependency | Source of truth |
| --- | --- |
| Minecraft/NeoForge target | [`deps/platform.toml`](../deps/platform.toml) |
| Intentional external mod or pack | One file under [`deps/components/`](../deps/components) |
| Development/release selection and transitive graph | Generated files under [`deps/locks/`](../deps/locks) |
| Pack membership | [`pack/build.gradle.kts`](../pack/build.gradle.kts) |
| Owned-mod compile, runtime, or test use | That project's `build.gradle.kts` |
| Java library or Gradle plugin | [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) |

A component is a logical dependency with one or more equivalent exact distributions.
Provider and file kind belong to the distribution:

```toml
# deps/components/create.toml
[distributions.maven-mod]
provider = "maven"
kind = "mod"
module = "com.simibubi.create:create-1.21.1"
version = "6.0.10-281"
filename = "create-1.21.1-6.0.10.jar"

[distributions.modrinth-mod]
provider = "modrinth"
kind = "mod"
project-id = "LNytGWDc"
version-id = "UjX6dr61"
filename = "create-1.21.1-6.0.10.jar"
```

The manifest filename must match the component ID. Adding a file creates the provider-neutral
catalog alias `deps.create`; it does not add Create to the pack or any classpath.

Profiles under `deps/profiles/` choose a representation/provider. Development prefers a
mod wrapper. `release-modrinth` and `release-curseforge` prefer their matching provider,
then a native pack representation. A selected native datapack/resourcepack gains the
profile's Paxi loader dependency.

Some provider files are hybrid packs. Their primary `kind` participates in profile
selection, while `additional-kinds` records the other installation locations needed by
the same bytes:

```toml
[distributions.modrinth-datapack]
provider = "modrinth"
kind = "datapack"
additional-kinds = ["resourcepack"]
project-id = "example"
version-id = "release"
filename = "example.zip"
```

The exporter installs that immutable file in every required location. Native resource
packs go under `config/paxi/resourcepacks/` so the profile's Paxi dependency loads them
automatically; datapacks remain in the conventional instance-level `datapacks/`
directory.

Locks preserve required and optional archive relationships, missing optional mod IDs,
physical sides, bundled mod IDs, and edge origins. Gradle reads the development lock and
adds its required relationships as ordinary transitive module metadata. Gradle never
writes `deps/` or contacts provider APIs on behalf of the manifest.

### Describe optional addons

A component-level relationship records stable pack intent without pretending that every
provider release declares the same dependency metadata. Declare it on the addon so the
manifest explains why that component exists:

```toml
# deps/components/sparkles.toml
# Provides Incendium textures and Stardust Labs localization fixes.

[relationships.incendium]
kind = "optional-addon-for"
```

Relationships are reporting-only: they do not install either component transitively.
Both components remain intentional Gradle roots. Generated profile locks keep the
relationship separate from artifact dependency evidence, so reports can distinguish a
stable addon relationship from a provider-origin optional edge.

### Correct invalid dependency metadata

If one exact release marks a dependency optional but cannot load without it, attach a
release-specific `require` correction to that distribution. The target remains a normal
component, but it does not become a pack root:

```toml
[distributions.modrinth-mod.dependency-corrections.geckolib-is-required]
# The release loads GeoItem while constructing the mod.
action = "require"
mod-id = "geckolib"
component = "geckolib"
version-range = "[4.8.4]"
side = "both"
applies-to = "modrinth:8KT9aVZC:sSsuuNws"
```

`applies-to` must match the containing immutable distribution. The lock refresh verifies
that the selected target provides the named mod ID, preserves any original optional edge,
and adds the correction as a required edge with its own origin.

Comments are reserved for context that helps a maintainer understand a non-obvious
component or correction. There is no required prose field: routine catalog entries are
self-explanatory from their consumers and coordinates.

Provider edges that identify a known Modrinth or CurseForge project without pinning a
version are resolved to that component's profile selection. Corrections remain reserved
for exact release-resolution defects rather than stable pack intent.

## Declare a consumer

Use `packComponents` only for external logical roots of the complete pack:

```kotlin
dependencies {
    packComponents(deps.create)
    packMods(project(":mods:bertie-tiers"))
}
```

The pack's full test suites explicitly inherit those logical roots:

```kotlin
configurations.named("gametestComponents") {
    extendsFrom(configurations.named("packComponents").get())
}
```

Owned mods declare only what they consume. Required runtime relationships propagate;
optional test installations do not:

```kotlin
dependencies {
    compileOnly(deps.ironsSpellsNSpellbooks)
    runtimeOnly(deps.ironsSpellsNSpellbooks)
    gametestRuntimeOnly(deps.simplySwords)
}
```

Use `compileOnly` for an optional API that source imports, `runtimeOnly` for a hard runtime
dependency, and the suite-specific configurations for integrations installed only in that
test. Required libraries of those roots come from the lock and must not be repeated.

## Validate and refresh

Validate committed inputs and locks without network access:

```bash
bertie-ci deps-check --workspace .
```

Run the separate advisory provider audit when network access is available:

```bash
bertie-ci deps-audit --workspace .
```

`deps-audit` compares declared Modrinth files with current provider metadata and reports
possible missing mod/datapack/resourcepack representations. Provider loader tags are
discovery hints rather than compatibility evidence: when a tag omits the configured
loader, the audit inspects the JAR descriptor before reporting the selected file or an
alternate mod representation. Contradictions are reported as provider metadata
discrepancies, not artifact incompatibilities. Audit findings do not change manifests or
fail normal dependency validation.

Redistribution evidence for files embedded by an exporter lives in
`deps/redistribution.toml`. Evidence records hold the decision and its human-readable
support. Export-specific assignments connect exact embedded files to those records:

```toml
strict = false

[evidence.author-permission]
allowed = true
text = """
The author permits these files to be redistributed in modpacks.
Discussion: https://example.com/permission
"""

[exports.modrinth.artifacts."curseforge:123456:789012"]
name = "example-library"
evidence = ["author-permission"]

[exports.curseforge.artifacts."modrinth:project-a:version-a"]
name = "example-addon"
component = "example-addon"
evidence = ["author-permission"]
```

`allowed` records the conclusion supported by the free-form evidence: `false` is useful
for licenses that permit provider-native modpack references but prohibit directly
bundling the file. An assignment has the artifact name from the lock, its component when
it is a component selection, and one or more evidence IDs. Transitive-only artifacts omit
`component`; they do not need artificial root components. An artifact may reference more
than one record, and a denial wins if records conflict.

Assignments are scoped to the export that embeds the file. Wildcards and project- or
component-wide coverage are intentionally unsupported: selecting a new immutable file is
a review point even when it can reuse existing evidence. Unused evidence, unknown
references, stale assignments, and names or components that disagree with the resolved
lock are rejected. Strict mode additionally rejects both denied files and embedded files
without assignments; it remains disabled while release evidence is incomplete.

This also verifies that every required external mod ID in an owned mod's NeoForge
metadata has a direct runtime dependency in that mod's `build.gradle.kts`.

Reselect distributions and prune unreachable locked evidence after editing existing
component/profile inputs:

```bash
bertie-ci deps-lock --workspace .
```

The initial producer deliberately reuses committed evidence for immutable artifacts. A
new provider distribution may reuse the inspected archive metadata of an equivalent
distribution of the same component and kind; its immutable provider coordinates still
come from the component manifest. A component with no locked equivalent is rejected
instead of receiving invented metadata. Provider discovery remains outside this tool.

After dependency changes, update and review Gradle locks/checksums as usual:

```bash
gradle resolveAndLockAll --write-locks --write-verification-metadata sha256
```

## Generate and inspect releases

```bash
gradle :pack:generateMrpack
gradle :pack:generateCurseForgePack
gradle :pack:generatePackwiz
```

`generateMrpack` serializes `release-modrinth` directly. Modrinth files become native
index entries; other selected files are embedded and listed in the redistribution audit
under `pack/build/reports/dependencies/`. `generateCurseForgePack` does the corresponding
work for `release-curseforge`: CurseForge project/file IDs become native `manifest.json`
entries, while owned mods and non-CurseForge fallbacks are placed in `overrides/`.
`generatePackwiz` encodes `release-modrinth` for validation/server conversion.

```bash
bertie-ci pack-validate --workspace . --component pack
bertie-ci pack-export-client --workspace . --component pack \
  --output .bertie-ci/release/bertie.mrpack
bertie-ci pack-export-curseforge --workspace . --component pack \
  --output .bertie-ci/release/bertie-curseforge.zip
bertie-ci pack-export-server --workspace . --component pack \
  --output .bertie-ci/release/bertie-server.zip
```
