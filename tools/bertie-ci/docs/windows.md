# Running bertie-ci on Windows

The `bertie-ci` commands are portable Python and Gradle code. Nix is the supported
dependency provider on Linux; native Windows supplies the same pinned dependencies
explicitly.

## Supported operations

| Operation | Native Windows |
| --- | --- |
| Build, JVM tests, and GameTests | Yes |
| Pack validation and export | Yes, with packwiz |
| Dedicated-server runtime tests | Yes |
| Client runtime tests | Yes, in an unlocked interactive desktop session |

Windows has no Xvfb equivalent. A client test opens a real Minecraft window and cannot
run unattended over SSH, as a service, or on a locked workstation. Use Linux with Nix for
headless client CI.

## Prerequisites

Install Git, Python 3.11 or newer, JDK 21, Gradle 8.14.4, and optionally packwiz. Set
`JAVA_HOME` to the JDK root and make `java`, `gradle`, and `python` available on
`PATH`.

```powershell
gradle --version
java -version
python --version
$env:JAVA_HOME
```

Projects do not carry Gradle wrappers. `BERTIE_CI_GRADLE` can name a specific Gradle
executable; otherwise `gradle` is resolved from `PATH`.

Enable Windows long paths before creating Minecraft instances. This requires an elevated
PowerShell and a reboot:

```powershell
Set-ItemProperty -Path 'HKLM:/SYSTEM/CurrentControlSet/Control/FileSystem' -Name LongPathsEnabled -Value 1
```

## Install the CLI from the monorepo

```powershell
git clone https://github.com/bertie-mc/bertie.git C:/src/bertie
cd C:/src/bertie
python -m venv .venv
./.venv/Scripts/Activate.ps1
python -m pip install -e ./tools/bertie-ci
$env:BERTIE_CI_FIXTURE_PACK = (Resolve-Path ./pack)
bertie-ci --help
```

The editable install reads `versions.json` and `fixtures/` from
`tools/bertie-ci`. If the package is copied elsewhere, set `BERTIE_CI_VERSIONS` and
`BERTIE_CI_FIXTURES` to those paths explicitly.

## Runtime tool JARs

Instance preparation needs packwiz-installer. Server tests add HeadlessMC; client tests
also add `mc-runtime-test`. Download the versions pinned in
[`versions.json`](../versions.json) and verify their SHA-256 values:

| Variable | Current file | SHA-256 |
| --- | --- | --- |
| `BERTIE_CI_HEADLESSMC_JAR` | `headlessmc-launcher-2.10.0.jar` | `52bd5006f478377b3893011d458562977d38c65ead6d2b31089beb4d614f13cd` |
| `BERTIE_CI_MCRT_JAR` | `mc-runtime-test-1.21.1-4.5.1-neoforge-release.jar` | `404e566645730470dc873db88c28d483995c9b7bb6999a6a2af9630a41bf7774` |
| `BERTIE_CI_PACKWIZ_INSTALLER_JAR` | `packwiz-installer.jar` | `c9f646908d340d84773948a9a7d98bc1dae250d35e1016dc6e2b8459760b5598` |

For example:

```powershell
$tools = 'C:/Users/me/AppData/Local/bertie-ci/tools'
$env:BERTIE_CI_HEADLESSMC_JAR = "$tools/headlessmc-launcher-2.10.0.jar"
$env:BERTIE_CI_MCRT_JAR = "$tools/mc-runtime-test-1.21.1-4.5.1-neoforge-release.jar"
$env:BERTIE_CI_PACKWIZ_INSTALLER_JAR = "$tools/packwiz-installer.jar"
```

Use `Get-FileHash <path> -Algorithm SHA256` before running downloaded JARs. The URLs in
`versions.json` are authoritative.

## Running workspace suites

The monorepo uses component subjects, just like Linux and hosted CI:

```powershell
bertie-ci build --workspace . --component bertie-tiers --output-dir .bertie-ci/artifacts
bertie-ci unit-test --workspace . --component bertie-tiers
bertie-ci gametest --workspace . --component bertie-tiers
```

For a client integration test:

```powershell
bertie-ci prepare-mod-instance --workspace . --component forge-ink --artifact .bertie-ci/artifacts/forge-ink --fixture forbidden-arcanus,irons-spells --side client --output-dir .bertie-ci/client
bertie-ci client-test --instance .bertie-ci/client/instance.json
```

For a dedicated-server scenario, prepare an instance and pass the project-owned command
document. The full pack example is:

```powershell
bertie-ci prepare-pack-instance --workspace . --component pack --side server --output-dir .bertie-ci/server
bertie-ci server-test --instance .bertie-ci/server/instance.json --command-test pack/tests/runtime/server-readiness.json
```

Standalone repositories use the same commands with their root component descriptor.
`--project` remains available for direct operations that do not need descriptor
planning.

## Environment variables

| Variable | Purpose |
| --- | --- |
| `BERTIE_CI_JAVA_HOME` | JDK root; takes precedence over `JAVA_HOME` |
| `BERTIE_CI_GRADLE` | Gradle 8 executable |
| `BERTIE_CI_PACKWIZ` | packwiz executable |
| `BERTIE_CI_HEADLESSMC_JAR` | HeadlessMC launcher JAR |
| `BERTIE_CI_MCRT_JAR` | `mc-runtime-test` JAR |
| `BERTIE_CI_PACKWIZ_INSTALLER_JAR` | packwiz-installer JAR |
| `BERTIE_CI_VERSIONS` | `versions.json` path |
| `BERTIE_CI_FIXTURES` | fixture catalog directory |
| `BERTIE_CI_FIXTURE_PACK` | canonical `pack/` checkout |

Runtime downloads are cached under `%USERPROFILE%/.cache/bertie-ci` by default.
`--cache-dir` selects another location.

## Common failures

- A missing `JAVA_HOME/bin/java.exe` means `JAVA_HOME` points to the wrong directory.
- Gradle 9 is not supported by the current ModDevGradle setup; use Gradle 8.14.4.
- A locked file below `.bertie-ci` usually means an earlier Java process is still alive
  or antivirus is holding the instance.
- Client tests on a remote or locked desktop are expected to fail; use headless Linux
  instead.
