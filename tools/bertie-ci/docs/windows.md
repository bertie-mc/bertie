# Running bertie-ci on Windows

`bertie-ci` planning and Gradle task supervision are portable. Install Python 3.11 or
newer, JDK 21, and the repository's Gradle version, then set `JAVA_HOME` and make Gradle
available on `PATH`.

```powershell
python -m venv .venv
./.venv/Scripts/Activate.ps1
python -m pip install -e ./tools/bertie-ci
bertie-ci --help
```

Build and run tests with their exact Gradle tasks:

```powershell
bertie-ci plan --workspace . --component bertie-tiers
bertie-ci gradle-task --workspace . --task :mods:bertie-tiers:test
bertie-ci gradle-task --workspace . --task :mods:bertie-tiers:runGameTests
bertie-ci gradle-task --workspace . --task :mods:bertie-tiers:runClientTests
```

Client tests run on the current interactive desktop, just as a direct Gradle invocation
does. The `--wayland` option is for Linux CI and is not used on Windows.

Pack validation and both exports first run the pack project's `generatePackwiz` Gradle
task. That task produces one side-aware tree for both export formats and includes the
locally built owned-mod JARs. Validation and client export additionally need packwiz;
Gradle itself never invokes it. Server-pack export needs the packwiz-installer JAR named
by `BERTIE_CI_PACKWIZ_INSTALLER_JAR`.

| Variable | Purpose |
| --- | --- |
| `BERTIE_CI_JAVA_HOME` | JDK root; takes precedence over `JAVA_HOME` |
| `BERTIE_CI_GRADLE` | Gradle executable; otherwise `gradle` from `PATH` |
| `BERTIE_CI_PACKWIZ` | packwiz executable |
| `BERTIE_CI_PACKWIZ_INSTALLER_JAR` | JAR used for server-pack export |
