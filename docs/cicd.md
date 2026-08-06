# CI and releases

Run repository commands from the pinned development shell:

```bash
nix develop
```

The repository uses the Gradle supplied by Nix rather than a Gradle wrapper.
For native Windows setup and equivalent commands, see [Development on Windows](windows.md).

## Check a change locally

Format Java, Kotlin, and Gradle Kotlin files with:

```bash
gradle spotlessApply
```

Start with the project and suite you changed:

```bash
gradle :mods:bertie-tiers:test
gradle :mods:bertie-tiers:runGameTests
gradle :mods:bertie-tiers:runClientTests
```

Run every suite enabled by one project with `runTests`:

```bash
gradle :mods:bertie-tiers:runTests
gradle :pack:runTests
```

Before finishing, run the repository checks:

```bash
gradle :check
```

This runs `spotlessCheck` and `testInfrastructure`. It does not run component tests,
GameTests, or client tests; run those through their project tasks as shown above.

Changes to shared Gradle plugins, test infrastructure, Nix, or CI also need:

```bash
nix flake check
```

See [Testing](testing.md) for choosing a suite and finding its reports.

## Preview CI

`bertie-ci` can show the tasks affected by the current branch:

```bash
bertie-ci plan --workspace . --base origin/main --head HEAD
```

To list all component checks:

```bash
bertie-ci plan --workspace . --all
```

The output contains exact Gradle task paths. Component discovery and shared paths are
configured in [`bertie-ci.toml`](../bertie-ci.toml).

A pull request or push to `main` starts the
[`Check` workflow](../.github/workflows/check.yml). It runs `gradle :check` and the
selected builds, unit tests, GameTests, client tests, and pack validation. The
[`Full pack` workflow](../.github/workflows/full-pack.yml) performs a scheduled complete
pack check and can also be started manually.

## Reproduce a CI failure

Run the failed Gradle task directly for normal local iteration. To use the same timeout,
logging, and process cleanup as CI, pass the task to `bertie-ci`:

```bash
bertie-ci gradle-task --workspace . \
  --task :mods:bertie-tiers:runGameTests \
  --work-dir .bertie-ci/local/bertie-tiers \
  --timeout 1800
```

Client tests use the current desktop by default. On Linux, add `--wayland` to run them in
the isolated Wayland environment used by CI:

```bash
bertie-ci gradle-task --workspace . \
  --task :mods:short-circuit-fix:runClientTests \
  --work-dir .bertie-ci/local/short-circuit-fix \
  --timeout 1800 \
  --wayland
```

Download the failed job's artifact from GitHub when the local failure does not contain
enough information. It includes the relevant Gradle, Minecraft, JUnit, Wayland, and
screenshot diagnostics.

## Add a component

Each releasable component has a `bertie-ci.toml` discovered by the root
[`bertie-ci.toml`](../bertie-ci.toml). For a NeoForge mod:

```toml
format = "bertie-ci.component.v2"
subject = "example-mod"
kind = "neoforge-mod"
gradle-project = ":mods:example-mod"

[version]
file = "mod.properties"
key = "mod_version"
```

Tests are selected from the source directories present in the project:

| Directory | Task |
| --- | --- |
| `src/test` | `test` |
| `src/gametest` | `runGameTests` |
| `src/clienttest` | `runClientTests` |

Keep mod versions in `mod.properties` and the pack version in `pack/pack.properties`.

## Validate or export the pack

```bash
bertie-ci pack-validate --workspace . --component pack
bertie-ci pack-export-client --workspace . --component pack \
  --output .bertie-ci/release/bertie.mrpack
bertie-ci pack-export-server --workspace . --component pack \
  --output .bertie-ci/release/bertie-server.zip
```

See [Managing dependencies](dependencies.md) for editing and inspecting the generated
pack.

## Publish a release

1. Update the version in the component's `mod.properties` or `pack.properties`.
2. Run its tests and build or export commands.
3. Create an annotated, SSH-signed `<subject>/vX.Y.Z` tag matching that version.
4. Push the tag.

For example:

```bash
git tag -s pack/v0.2.0 -m "Release pack v0.2.0"
git push origin pack/v0.2.0
```

The release workflow publishes mod JARs or the pack's client and server archives. See the
[`bertie-ci` command reference](../tools/bertie-ci/README.md) for other command options.
