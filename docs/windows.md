# Development on Windows

Bertie can be developed natively on Windows without Nix. Install Git, JDK 21, and
Gradle 8. Set `JAVA_HOME` and make `java` and `gradle` available on `PATH`. The
repository does not include a Gradle wrapper.

The same Gradle task names documented in [Testing](testing.md),
[Managing dependencies](dependencies.md), and [CI and releases](cicd.md) apply on
Windows. Run them from PowerShell without `nix develop`. Skip `nix flake check`; it
verifies the Nix development environment used on Linux and in CI.

## Format and test

Run Gradle commands from the repository root:

```powershell
gradle spotlessApply
gradle :check
gradle :mods:bertie-tiers:runTests
```

Client tests use the current interactive desktop. The `--wayland` option accepted by
`bertie-ci` is only for Linux CI.

## Optional: install bertie-ci

Gradle development does not require `bertie-ci`. To preview affected checks, reproduce
supervised CI runs, validate the pack, or create release archives, install Python 3.11 or
newer and set up the command with:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e .\tools\bertie-ci
bertie-ci --help
bertie-ci plan --workspace . --component bertie-tiers
bertie-ci gradle-task --workspace . --task :mods:bertie-tiers:test
```

Pack validation and client export also require packwiz. Server export requires the
packwiz-installer JAR. See the [`bertie-ci` command reference](../tools/bertie-ci/README.md)
for pack commands and executable overrides.
