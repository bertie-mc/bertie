# CI/CD

Local commands and GitHub Actions invoke the same Gradle tasks. Gradle builds and launches
the projects. Java drivers run in-game tests. `bertie-ci` plans and supervises CI work.

## Development shell

Enter the pinned environment before running repository commands:

```bash
nix develop
```

Nix supplies JDK 21, Gradle, Python, packwiz, Sway, Mesa, and the GitHub tooling. The
repository has no Gradle wrapper and does not ask Gradle to download a Java toolchain.

Run the shared environment and workflow checks with:

```bash
nix flake check
gradle testInfrastructure
```

## What each tool does

| Component | Work |
| --- | --- |
| Gradle and `build-logic` | Resolve and verify dependencies, compile projects, stage isolated instances, launch test processes, publish reports, and generate the packwiz tree |
| Java test drivers | Discover in-game tests, coordinate Minecraft threads and lifecycle, manage client options and embedded servers, and write per-test results |
| `bertie-ci` | Discover components, plan affected tasks, supervise process groups and timeouts, provision CI Wayland, and run packwiz validation or exports |
| GitHub Actions | Supply hosted runners, pass plan entries to bertie-ci/Gradle, upload diagnostics, and publish releases |

```mermaid
flowchart LR
    GH["GitHub Actions<br/>hosted adapter"] --> CI["bertie-ci<br/>plan and supervise"]
    CI -->|Gradle task path| G["Gradle<br/>resolve, build, stage, launch"]
    G --> J["JUnit JVM"]
    G --> S["NeoForge GameTest server"]
    G --> C["Minecraft client JVM"]
    C --> D["Java client-test driver"]
    CI -. "Wayland environment<br/>client CI only" .-> C
    J --> R["reports and diagnostics"]
    S --> R
    D --> R
    G --> W["generated packwiz tree"]
    W -->|"validate or export"| CI
```

CI does not maintain a second dependency graph, and Gradle does not configure Wayland or
invoke packwiz.

## Components and versions

The root [`bertie-ci.toml`](../bertie-ci.toml) discovers component descriptors and defines
which shared paths affect Gradle projects or every component. Each component descriptor
records its subject, kind, optional Gradle path, and version file:

```toml
format = "bertie-ci.component.v2"
subject = "example-mod"
kind = "neoforge-mod"
gradle-project = ":mods:example-mod"

[version]
file = "mod.properties"
key = "mod_version"
```

Each component reads its version from its own directory:

| Component kind | Version source |
| --- | --- |
| NeoForge mod | `mod.properties`, key `mod_version` |
| Pack | `pack.properties`, key `version` |
| bertie-ci tool | `pyproject.toml`, key `project.version` |

Source-set directories declare which test suites exist. Component descriptors do not
duplicate test lists, mod IDs, dependency sides, or Minecraft launch configuration.

## Change-based test planning

Inspect the same plan CI uses:

```bash
bertie-ci plan --workspace . --all
bertie-ci plan --workspace . --base origin/main --head HEAD
```

The plan has five matrices:

| Matrix | Work |
| --- | --- |
| `build` | Assemble affected owned mods |
| `unit` | Run affected `test` tasks |
| `gametest` | Run affected `runGameTests` tasks |
| `client` | Run affected `runClientTests` tasks |
| `validate` | Validate affected generated packs |

A changed component also selects its downstream dependents. Changes to shared Gradle or
testing infrastructure select every Gradle project; changes to repository-wide CI tooling
select every component. Paths matched by `ignored-paths`, including documentation and
licensing files, produce no plan entries. The patterns live in the root workspace
descriptor.

```mermaid
flowchart LR
    D["changed paths"] --> P["bertie-ci plan"]
    W["workspace scopes"] --> P
    C["component descriptors"] --> P
    P --> B["build matrix"]
    P --> U["unit matrix"]
    P --> G["GameTest matrix"]
    P --> T["client-test matrix"]
    P --> V["pack-validation matrix"]
    B --> E["Gradle tasks"]
    U --> E
    G --> E
    T --> E
    V --> X["Gradle generation + packwiz validation"]
```

To reproduce one planned task with bertie-ci's timeout and log handling:

```bash
bertie-ci gradle-task --workspace . \
  --task :mods:bertie-tiers:runGameTests \
  --work-dir .bertie-ci/local/bertie-tiers \
  --timeout 1800
```

For local iteration, run the Gradle task directly.

## CI checks

The main [`check.yml`](../.github/workflows/check.yml) workflow runs on pull requests and
pushes to `main`:

1. Plan affected components from the Git range.
2. Always validate the Nix flake and shared test infrastructure.
3. Run affected builds and unit tests.
4. Run GameTest and client-test entries as separate jobs.
5. Structurally validate the generated pack when affected.
6. Combine all job outcomes into one required-check result.

Jobs upload JUnit reports, run directories, logs, crash reports, client screenshots, and
bertie-ci work directories for diagnosis. Matrix jobs use `fail-fast: false`, so one
component failure does not hide results from the others.

The [`full-pack.yml`](../.github/workflows/full-pack.yml) workflow runs nightly and on
manual dispatch. It executes every suite declared by `:pack` and validates the generated
pack, independently of change-based planning.

Reusable unit, GameTest, client-test, and mod-build workflows are available to standalone
Bertie repositories through the tagged `bertie-ci` release. Their YAML delegates to the
same actions and commands used by this monorepo.

## Native Wayland in CI

Client-test Gradle tasks launch graphical Minecraft. Locally they inherit the current
desktop. For a Linux CI client matrix entry, the Gradle action asks `bertie-ci` for an
isolated native-Wayland session.

`bertie-ci` starts Sway on wlroots' headless backend with Xwayland disabled, provides a
Wayland-capable GLFW and software rendering, attaches a persistent virtual keyboard to
the Wayland seat, starts Gradle in that environment, and terminates its process group
afterward. Neither Gradle nor the Java driver contains display-backend policy.

## Pack validation and export

Pack validation and export start with Gradle generation:

```mermaid
flowchart LR
    M["Minecraft artifact manifest"] --> G["Gradle :pack:generatePackwiz"]
    O["owned project JARs"] --> G
    K["pack/config and pack.properties"] --> G
    G --> T["one side-aware tree<br/>pack/build/packwiz"]
    T --> V["bertie-ci + packwiz<br/>validate"]
    T --> C["bertie-ci + packwiz<br/>client .mrpack"]
    T --> S["bertie-ci<br/>server archive + installer"]
```

Gradle generates the tree without executing packwiz. `bertie-ci` then invokes packwiz or
packages the generated files:

```bash
bertie-ci pack-validate --workspace . --component pack
bertie-ci pack-export-client --workspace . --component pack \
  --output .bertie-ci/release/bertie.mrpack
bertie-ci pack-export-server --workspace . --component pack \
  --output .bertie-ci/release/bertie-server.zip
```

Both exports use the generated side-aware tree. Owned JARs come from local Gradle project
outputs; publishing does not look up or compare separately released copies of owned mods.

## Releases

Each component has its own version and release tag. A release starts when an annotated,
SSH-signed tag in the exact form `<subject>/vX.Y.Z` is pushed:

```bash
git tag -s pack/v0.2.0 -m "Release pack v0.2.0"
git push origin pack/v0.2.0
```

Before tagging:

1. Update the component's local version metadata.
2. Run the component's required build and test pipelines for that commit.
3. Confirm that the tag subject matches its `bertie-ci.toml` subject and the tag version
   exactly matches the component metadata.

The release workflow validates the tag before doing any publishing:

- a mod release builds its JAR and publishes it to GitHub Releases;
- a pack release generates and publishes the client `.mrpack` and server `.zip`;
- a tool release publishes the tagged source release.

Never create a release by editing generated packwiz files or by substituting separately
published owned-mod JARs.

See [Dependencies](dependencies.md) for lock/checksum maintenance, [Testing](testing.md)
for suite authoring, and the [bertie-ci command reference](../tools/bertie-ci/README.md)
for CLI usage.
