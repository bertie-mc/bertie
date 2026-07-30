# CLAUDE.md

**Read [AGENTS.md](AGENTS.md) before doing any work here.**

This repo is the **bertie** modpack — a packwiz manifest describing ~490 mods. It contains
none of them. Minecraft 1.21.1, NeoForge 21.1.233, Java 21.

## Non-negotiables

1. **GitHub is the source of truth.** Commit, `git pull --rebase`, push; leave `git status`
   clean. Say so explicitly if you could not push.
2. **Never commit a mod jar.** CI fails if one is tracked. Mods are referenced, not
   redistributed — their licences range from permissive to all-rights-reserved. A loose jar
   beside a metafile also makes the index list one path twice and installs two versions.
3. **`packwiz refresh` after every change**, and commit `index.toml` in the same commit.
4. **bertie''s own mods come from GitHub Releases.** Release from the mod''s own repo first,
   then `packwiz github add bertie-mc/<repo>` or `packwiz update <name>`. Never hand-build a
   jar into `mods/`.
5. **Never `add`/`update` with `-y` on a free-text search** — it blind-accepts the first hit
   and has added an unrelated mod before. Check `side`, and diff the index for silent pin
   bumps.
6. **Worktrees must not dangle.** `git worktree list` must show only the main checkout
   before you finish.
7. **Do not revert another agent''s work.** Several chats work this project in parallel.
   Report conflicts; do not arbitrate them.

Full detail: **[AGENTS.md](AGENTS.md)**.
