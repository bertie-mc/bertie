# CI, local checks, and releases

Run repository commands from the pinned development shell:

```bash
nix develop
```

The shell supplies Java 21, Gradle, Python, packwiz, Sway, Mesa, and the GitHub CLI. This
repository does not use a Gradle wrapper.

## Check a change locally

Start with the component you changed:

```bash
gradle :mods:bertie-tiers:test
gradle :mods:bertie-tiers:runGameTests
gradle :mods:bertie-tiers:runClientTests
```

Use `runTests` to run every suite enabled by one project:

```bash
gradle :mods:bertie-tiers:runTests
gradle :pack:runTests
```

Changes to shared Gradle, test-driver, Nix, or CI code should also run:

```bash
nix flake check
gradle testInfrastructure
```

See [Testing](testing.md) for the source-set and task conventions.

## Preview the CI plan

`bertie-ci` maps changed files to affected components and their Gradle tasks. Inspect the
plan before pushing a broad change:

```bash
bertie-ci plan --workspace . --base origin/main --head HEAD
```

To list every component task, regardless of changed files:

```bash
bertie-ci plan --workspace . --all
```

The plan contains `build`, `unit`, `gametest`, `client`, and `validate` lists. A change
to a component also selects components that depend on it. Shared paths are configured in
[`bertie-ci.toml`](../bertie-ci.toml).

```mermaid
flowchart LR
    D["changed files"] --> P["bertie-ci plan"]
    P --> B["build"]
    P --> U["unit tests"]
    P --> G["GameTests"]
    P --> C["client tests"]
    P --> V["pack validation"]
    B --> T["Gradle tasks"]
    U --> T
    G --> T
    C --> T
    V --> K["Gradle generation + packwiz"]
```

Documentation, licensing files, and other ignored paths intentionally produce no
component tasks, so the check job finishes after planning.

## Reproduce a CI task

For fast local iteration, invoke Gradle directly. To reproduce CI timeout, process-group,
and log handling, use the exact task from the plan:

```bash
bertie-ci gradle-task --workspace . \
  --task :mods:bertie-tiers:runGameTests \
  --work-dir .bertie-ci/local/bertie-tiers \
  --timeout 1800
```

Client tests normally use the current desktop. On Linux, add `--wayland` to reproduce the
isolated native-Wayland session used by CI:

```bash
bertie-ci gradle-task --workspace . \
  --task :mods:short-circuit-fix:runClientTests \
  --work-dir .bertie-ci/local/short-circuit-fix \
  --timeout 1800 \
  --wayland
```

The Wayland run uses headless Sway with Xwayland disabled and software rendering. Gradle
still receives an ordinary graphical Minecraft task; `bertie-ci` starts and stops the
display session around it.

## GitHub checks

[`check.yml`](../.github/workflows/check.yml) runs for pull requests and pushes to
`main`. [`full-pack.yml`](../.github/workflows/full-pack.yml) runs nightly and can also be
started manually.

```mermaid
flowchart LR
    E["push or pull request"] --> J["one Check job"]
    J --> P["plan affected tasks"]
    P --> D["restore or prepare dependencies"]
    D --> W["Wayland when client tests are selected"]
    W --> G["one Gradle task graph"]
    G --> V["validate generated pack"]
    V --> R["reports and diagnostics"]
```

The job passes every selected build, unit-test, GameTest, client-test, infrastructure, and
pack-generation task to one Gradle invocation. Gradle deduplicates shared prerequisites
and schedules independent work in parallel. `--continue` lets independent tasks finish
after a failure. Minecraft process tasks share one execution slot because the full-pack
server and client runs reserve 8–10 GiB each.

The job uses `.bertie-ci/gradle-user-home` and restores an immutable cache key derived
from Gradle dependency inputs. On an exact miss it runs `prepareOfflineBuild`, saves the
completed entry, and then executes the planned graph offline. The cache is available to
the main check, nightly full-pack, and release workflows; there is no same-run dependency
artifact or dependency-preparation job.

Failed and successful jobs upload the useful parts of their work directories:

| Test kind | Reports and runtime data |
| --- | --- |
| Unit | `build/reports/tests`, `build/test-results` |
| GameTest | `build/test-results/gametest`, `build/minecraft-runs/gametest` |
| Client | `build/test-results/clienttest`, `build/test-diagnostics/clienttest`, `build/minecraft-runs/clienttest` |
| Supervised task graph | `.bertie-ci/gradle/gradle.log` and the Wayland log when used |

CI uploads one artifact for the complete job. Gradle task paths in the log and the
project-local reports identify each failed component.

## Add a component to CI

Every releasable component has a `bertie-ci.toml`. A NeoForge mod descriptor looks like:

```toml
format = "bertie-ci.component.v2"
subject = "example-mod"
kind = "neoforge-mod"
gradle-project = ":mods:example-mod"

[version]
file = "mod.properties"
key = "mod_version"
```

The root [`bertie-ci.toml`](../bertie-ci.toml) must discover the descriptor. Test tasks
are inferred from directories rather than listed in the descriptor:

| Directory | Planned task |
| --- | --- |
| `src/test` | `test` |
| `src/gametest` | `runGameTests` |
| `src/clienttest` | `runClientTests` |

The descriptor's `subject` is also used in release tags and artifact names. Keep the
version in the component's existing metadata: `mod.properties` for a mod,
`pack.properties` for the pack, and `pyproject.toml` for `bertie-ci`.

## Validate and export the pack

Use the same commands locally and in release jobs:

```bash
bertie-ci pack-validate --workspace . --component pack
bertie-ci pack-export-client --workspace . --component pack \
  --output .bertie-ci/release/bertie.mrpack
bertie-ci pack-export-server --workspace . --component pack \
  --output .bertie-ci/release/bertie-server.zip
```

Each command runs `:pack:generatePackwiz` before invoking the required packwiz or archive
operation. See [Managing dependencies](dependencies.md) for changing the generated pack.

## Publish a release

1. Update the version in the component's `mod.properties`, `pack.properties`, or
   `pyproject.toml`.
2. Run its tests and build or export commands on the release commit.
3. Create an annotated, SSH-signed `<subject>/vX.Y.Z` tag whose version exactly matches
   the metadata.
4. Push the tag.

For example:

```bash
git tag -s pack/v0.2.0 -m "Release pack v0.2.0"
git push origin pack/v0.2.0
```

[`release.yml`](../.github/workflows/release.yml) checks the tag and metadata before
publishing. Mod releases attach the built JAR to GitHub Releases. Pack releases attach a
client `.mrpack` and server `.zip`. A source-only `bertie-ci` release publishes the tagged
tool release.

For all CLI options, see the [`bertie-ci` command reference](../tools/bertie-ci/README.md).
