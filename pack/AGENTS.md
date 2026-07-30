# AGENTS.md

Instructions for AI coding agents working in the **bertie** modpack repository.

This repo is a **packwiz manifest**, not a mod. It describes ~490 mods; it contains none
of them. Minecraft **1.21.1**, NeoForge **21.1.233**, Java **21**.

---

## 1. GitHub is the source of truth

```bash
git add -A
git commit -m "type(scope): what changed"
git pull --rebase origin main
git push origin main
```

`git status` must be clean when you stop. Always `--rebase`. If you could not push, say so
explicitly rather than finishing silently with unpushed commits.

---

## 2. Never commit a mod jar

`.gitignore` blocks `*.jar` and CI fails the build if one is tracked. This is not tidiness:

- The pack references ~490 third-party mods whose licences range from permissive to
  **all rights reserved**. Committing their jars would redistribute them.
- A loose jar and a `.pw.toml` can resolve to the **same filename**, so the index lists one
  target path twice and the pack installs two copies — or two *versions* — of one mod.
- Removing the duplicate raw entry later deletes the file while the metafile still believes
  it is cached, so the mod silently vanishes from synced instances. That exact bug dropped
  `darkermagic` from the playtest instance.

To add a mod, reference it:

```bash
packwiz modrinth add <slug>
packwiz curseforge add <slug>
packwiz github add bertie-mc/<repo>   # bertie's own mods
packwiz refresh                        # ALWAYS, after any change
```

**Commit `index.toml` in the same commit as whatever you changed.** An index whose hashes
do not match the tree is worse than no index.

---

## 3. packwiz pitfalls

- **`packwiz <source> add "<free text>" -y` blind-accepts the first search hit.** It has
  added a completely unrelated mod to a bertie pack before. Use an exact slug or
  `owner/repo`. If you must search, drop `-y` and verify the match.
- **Always check `side`.** Wrong side ships no client jar or no server jar. CI enforces
  that a side is *declared*, but it cannot tell you the value is *correct*.
- **`add` and `update` can silently bump an existing pin.** Adding `ftb-xmod-compat` once
  moved a pack from 21.1.8 to 21.1.10 unasked. Diff `index.toml` and confirm only the
  intended lines moved. `packwiz pin <name>` freezes a version.
- **A `.pw.toml` carries no dependency information.** Copying one from another pack brings
  the jar reference and nothing it needs. Re-check the dependency closure by hand.

---

## 4. bertie's own mods come from GitHub Releases

Never build a bertie mod locally and drop the jar in here. Release it from its own repo
under [`bertie-mc`](https://github.com/bertie-mc) — bump `mod_version`, tag `vX.Y.Z`, let
its `release.yml` publish — then:

```bash
packwiz github add bertie-mc/<repo>     # first time
packwiz update <name>                   # subsequent releases
```

If a mod's source has moved on but no release exists yet, **release it first**. Pointing
the pack at an unreleased version is not possible, and shipping a hand-built jar instead is
what §2 exists to prevent.

---

## 5. CI is the contract

| Workflow | What a failure means |
|---|---|
| `validate.yml` | the index is stale, a metafile lacks a side, two mods target the same filename, a jar got committed, or a download no longer resolves |
| `server-boot.yml` | the pack *installs* but does not *run* — a mod crashes during loading, a dependency is missing, or a registry freeze fails |
| `release.yml` | the published artefacts could not be built |

Do not merge a red `validate`. `server-boot` takes ~20–40 minutes because it boots all ~490
mods; that is expected, and its log artefact is the first thing to read when it fails.

---

## 6. Worktrees

If another agent may be working in this checkout:

```bash
git worktree add ../bertie-pack-<task> -b <task-branch>
# work, commit, push the branch
git worktree remove ../bertie-pack-<task>
git branch -d <task-branch>
git worktree prune
```

### Dangling worktrees are forbidden

**`git worktree list` must show only the main checkout before you finish** — including
after a failed or abandoned task. Do not `--force` away changes you have not read.

---

## 7. Do not sidetrack

- **Do not edit mod source from here.** Each mod is its own repository with its own
  `AGENTS.md`.
- **Do not delete or revert another agent's work** to make your change apply. Several chats
  work this project in parallel and have undone each other before. Report the conflict;
  let the human arbitrate.
- **Before deleting anything, grep every reference.**
- **Answer from the manifest and the docs, not from memory** — exact ids, exact versions.

---

## 8. Before you report a task complete

```
[ ] packwiz refresh run; index.toml committed alongside the change
[ ] git status clean; committed and pushed with --rebase
[ ] git worktree list shows only the main checkout
[ ] no jar committed
[ ] side checked on anything added or copied
[ ] index diff contains only the lines you intended (no silent pin bumps)
[ ] validate.yml green
```
