# Config Migrations

`config-migrations` applies ordered, versioned config fragments during the owning config system's
normal load. It is not an enforcement loop: after a version has run, players and server owners
remain free to edit those settings until a later change explicitly touches them again.

## Layout

The pack owns one manifest per logical target:

```text
config/config-migrations/
  migrations/
    minecraft/
      options.toml
    neoforge/
      create/
        server.toml
    fzzy/
      simply-swords.toml
    resourceful/
      creeper-overhaul.toml
    artifacts/
      items.toml
```

The first directory below `migrations` selects the concrete integration. Deeper directories and
filenames are organizational only, and each integration directory is discovered recursively.
Moving a manifest does not change its runtime state.

## Changes

Every target contains its ordered change history. Versions are positive and strictly increasing;
gaps are allowed.

```toml
mod = "create"
type = "SERVER"
file = "create-server.toml"

[[changes]]
version = 3
op = "merge"

[changes.fragment.recipes]
allowRegularCraftingInCrafter = false
maxRotationSpeed = 256
```

`merge` recursively combines tables. Scalars and lists replace the existing leaf, so assigning a
single field is simply a one-field merge. Keys not present in the fragment are untouched.

The stored version selects every change with a greater version. Pending changes are applied in
order and batched into one merge and final-persistence cycle, then the greatest applied version is
recorded. Changing an already published fragment therefore requires a new version.

Unpublished histories may be squashed by replacing old changes with their cumulative result.
Squashing versions that may already exist in state requires a new, greater version: installations
at an older version then apply that cumulative fragment and it reasserts every included field.
Only do that when reassertion is intended or those intermediate states are no longer supported.

There is no deletion operation until the pack has a concrete need for one.

## State

The running instance owns one tiny state file per physical config:

```text
config/config-migrations/state/
  config/artifacts/items.toml.version
  saves/Example/serverconfig/create-server.toml.version
```

Each file contains only the greatest applied version:

```text
3
```

The state tree mirrors the config's game-relative physical path. This keeps different worlds'
SERVER configs independent and avoids a shared state database. A missing version file means
version `0`, regardless of whether the config itself already exists.

The physical config path, not the manifest filename, is the state identity. Renaming a physical
config starts it at version 0. Deleting and regenerating a config while retaining its state does
not replay old changes.

## Integrations

The integration directories are `minecraft`, `neoforge`, `autoconfig`, `fzzy`, `owo`,
`resourceful`, `supermartijn642`, `wunderlib`, and `artifacts`. Their selectors and lifecycle
hooks are intentionally separate. The third-party libraries are optional: an absent library has
no active hook and is not a runtime dependency of this mod.

### Minecraft options

Minecraft's own options target is selected by its root-relative filename:

```toml
file = "options.txt"

[[changes]]
version = 1
op = "merge"

[changes.fragment]
autoJump = false
guiScale = 3
```

`options.txt` is a flat `key:value` file, so its fragment must also be flat. Booleans and numbers
are written as their textual values; TOML strings are copied literally for options whose native
value is structured text. Unknown option keys are preserved. The adapter runs during the initial
full client options load, after vanilla data fixing and before values and key mappings are
consumed. It does not invoke an early full options save, which would omit mod key mappings that
have not registered yet.

### NeoForge

A NeoForge manifest selects one registered config with `mod`, `type`, and `file`. The adapter
covers `STARTUP`, `CLIENT`, `COMMON`, and local physical `SERVER` loads. It supports NeoForge's
standard `ModConfigSpec` and Iceberg's `NeoForgeIcebergConfigSpec`.

Pending fragments are applied to the loaded document immediately before native acceptance, then
corrected through its `IConfigSpec` and persisted. NeoForge remains responsible for resolving and
creating the physical file, parsing, native invalid-config backups, acceptance, and loading events.
State is committed after acceptance and before the owner receives the loading event.

The generic `IConfigSpec` call site lives in FML's bootstrap layer, so each transformable
implementation needs a thin lifecycle bridge. The bridge contains no implementation-specific
migration behavior; additional implementations only need to connect `validateSpec` and
`acceptConfig`. A future upstream hook could remove those bridges entirely.

### AutoConfig

An AutoConfig manifest selects the root `@Config` name and supplies the serializer's config-relative
physical file for state identity:

```toml
config = "example"
file = "example.json5"
```

For a partitioned root, `partition` selects a direct child by that child's own `@Config` name.
The adapter decorates the registered serializer, merges the deserialized Java config object, and
lets AutoConfig run its normal post-load validation and serialization. State advances only when
that same migrated root is passed back to the native serializer; replacement with a default root
does not commit it. This is serializer-independent and supports custom serializers through the
normal AutoConfig interface.

### Fzzy Config

A Fzzy manifest uses one config-relative `file`, including its preferred native suffix:

```toml
file = "simplyswords/client.toml"
```

The actual name, folder, subfolder, and preferred file type are taken from the native load call.
The adapter merges through Fzzy's `FileType` tree before deserialization, then lets Fzzy perform
its normal validation and correction. Fzzy's correction and alternate-format writes are
asynchronous, so only a pending migration waits for the native write it initiated before advancing
state. Unrelated Fzzy loads and writes remain unchanged.

### owo-config

An owo-config manifest selects the wrapper's globally unique config name:

```toml
config = "accessories"
```

The wrapper supplies its physical path. The adapter merges its native Jankson document before
options consume it, then saves the accepted live model and advances state when that save returns.
It also distinguishes a completed native load from owo's caught parse or deserialization failures,
so a failed load is neither saved nor committed. Missing files are first materialized through the
wrapper's own save path.

### Resourceful Config

A Resourceful Config manifest selects the owning configurator and config ID:

```toml
mod = "creeperoverhaul"
config = "creeperoverhaul"
```

The adapter supports Resourceful Config's standard `ParsedConfig` and its JSONC file. It runs after
the initial native load and before the configurator's mandatory save, merges through native entry
setters and constraints, and commits when that save returns. The library normally converts its
legacy JSON file during load; if that legacy file remains, migration waits rather than assigning
JSONC state to a different physical file. Custom `ResourcefulConfig` implementations retain their
native lifecycle and are left untouched.

### SuperMartijn642's Config Lib

A Config Lib manifest selects `mod` and the registered `file` identifier. The adapter merges its
native TOML or JSON document after parsing and before entries become live. Config Lib then rebuilds
and writes its canonical document, after which the adapter advances state.

### WunderLib

A WunderLib manifest selects the config's resource ID:

```toml
id = "wover:client"
```

The adapter merges the loaded JSON root as `ConfigFile` construction finishes, before the concrete
config subclass consumes its values. It invokes the native forced save and commits when that save
returns.

### Artifacts

An Artifacts manifest selects one of its custom config managers:

```toml
config = "items"

[[changes]]
version = 1
op = "merge"

[changes.fragment.whoopee_cushion]
fartChance = 0.1
```

The available manager names are `client`, `general`, and `items`. The adapter runs after Artifacts
loads and corrects its NightConfig document, but before it registers the file watcher or reads the
live values. It corrects and saves again after merging, then commits the version.

All integrations share the manifest, version-state, and recursive-merge model. Their selectors,
hooks, paths, native value models, and lifecycles remain independent.

## Failure model

The version file advances only after the migrated config reaches its owner's persistence boundary.
A normal native save return counts as success; the adapter does not reopen or second-guess the
owner's output. Fzzy's asynchronous write is awaited because completion is its persistence boundary.
If persistence succeeds but writing state fails, the same merge may replay at the next launch.
Recursive merge is idempotent, so no rollback or transaction layer is needed.

Datagen bypasses migrations. `server.properties`, down migrations, deletion, scripts, and GUI
support are deferred.

## Build and test

```text
nix develop -c gradle :mods:config-migrations:test
nix develop -c gradle :mods:config-migrations:assemble
```

The JVM suite owns merge and lifecycle semantics. Bertie CI also runs the production JAR in two
clients: one with every optional integration absent, and one with the current real integration
libraries. The latter seeds migration manifests, registers real configs through every supported
library, joins an integrated server, and verifies the live values, persisted files, and version
state produced by the client and server lifecycle hooks.

## License

Released into the public domain under **The Unlicense**. See [UNLICENSE](UNLICENSE) and
[NOTICE](NOTICE).
