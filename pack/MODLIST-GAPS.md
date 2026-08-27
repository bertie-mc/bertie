# Modlist gaps

Every mod that at least one of the five big lists does not have. **87 of 584.**
The five are the online doc (417), `docs/modlist.md` (400), the atlas (426), the full test
pack (481) and `deps` (461 - components plus the artifacts its locks pin). None of them is
the authority - each is missing things the others have.

**0 rows have no known reason.** Those are the ones only berlord can settle.

56 mods cut on 2026-08-27 are left out - they are settled. So are the 86 libraries that only ever appear in a pack.

| Mod | Missing from | Reason |
|---|---|---|
| `wings-of-fire` | modlist.md | the modlist.md mirror is partial - it never transcribed everything |
| `bad-wither-no-cookie` | full-test | wanted - berlord confirmed 2026-08-27; it simply never got installed |
| `magitech` | full-test | added after the full test pack was last synced (it is in a newer pack) |
| `railways-untold` | full-test | wanted - berlord confirmed 2026-08-27; it simply never got installed |
| `typewriter-daycounter` | full-test | ships as a datapack, not a mod jar |
| `upgraded-mobs` | full-test | ships as a datapack, not a mod jar |
| `usefulmagic` | full-test | wanted - berlord confirmed 2026-08-27; it simply never got installed |
| `what-are-they-up-to` | full-test | wanted - berlord confirmed 2026-08-27; it simply never got installed |
| `wingscontracts` | full-test | wanted - berlord confirmed 2026-08-27; it simply never got installed |
| `ferrite-core` | online doc, modlist.md | in the pack and classified by the atlas, but never written back to the doc or its mirror |
| `modernfix` | online doc, modlist.md | in the pack and classified by the atlas, but never written back to the doc or its mirror |
| `noisium` | online doc, modlist.md | in the pack and classified by the atlas, but never written back to the doc or its mirror |
| `apothic-attributes` | online doc, atlas | added after the online doc was last touched |
| `ftb-library` | online doc, atlas | added after the online doc was last touched |
| `ftb-quests` | online doc, atlas | added after the online doc was last touched |
| `ftb-teams` | online doc, atlas | added after the online doc was last touched |
| `malum-vestis` | modlist.md, full-test | CurseForge only; never added |
| `distanthorizons` | full-test, deps manifest | wishlist entry in the doc's WIP sections; never installed |
| `dragonkind-evolved` | full-test, deps manifest | wishlist entry in the doc's WIP sections; never installed |
| `drippy-loading-screen` | full-test, deps manifest | wishlist entry in the doc's WIP sections; never installed |
| `edf-remastered` | full-test, deps manifest | wishlist entry in the doc's WIP sections; never installed |
| `fancymenu` | full-test, deps manifest | wishlist entry in the doc's WIP sections; never installed |
| `mekanism-fission-recipe` | full-test, deps manifest | wishlist entry in the doc's WIP sections; never installed |
| `nekos-enchanted-books` | full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `true-ending` | full-test, deps manifest | wishlist entry in the doc's WIP sections; never installed |
| `apotheosis` | online doc, atlas, full-test | added after the online doc was last touched |
| `apotheosis-x-irons-spellbooks-compat` | online doc, atlas, full-test | added after the online doc was last touched |
| `apothic-combat` | online doc, atlas, full-test | added after the online doc was last touched |
| `apothic-enchanting` | online doc, atlas, full-test | added after the online doc was last touched |
| `apothic-spawners` | online doc, atlas, full-test | added after the online doc was last touched |
| `easy-magic-apotheosis-compat` | online doc, atlas, full-test | CurseForge only; listed 2026-08-05, never added to a pack |
| `ftb-filter-system` | online doc, atlas, full-test | added after the online doc was last touched |
| `ftb-xmod-compat` | online doc, atlas, full-test | added after the online doc was last touched |
| `ender-eyes` | online doc, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `explosive-enhancement` | online doc, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `withered-hearts` | online doc, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `2primogem-craft` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `allaybottle` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `create-deep-dark` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `diamethysts` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `dragon-scale` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `dragon-scale-loot` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `dragonsteel-more-magic-series` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `etheria` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `evolved-mekanism` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `genshincraft` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `keepers-of-the-stones-2` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `knight-quest` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `lne-wizards` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `more-ores-more-gem` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `oceanic-weaponry` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `oxidized` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `quest-items` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `runes` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `warden-tools` | modlist.md, full-test, deps manifest | asset donor - harvested for parts, never meant to ship |
| `hide-experimental-warning` | online doc, modlist.md, atlas, full-test | adopted onto the list 2026-08-27; it came from `deps/components` and is not in a pack yet |
| `particle-storm` | online doc, modlist.md, atlas, full-test | adopted onto the list 2026-08-27; it came from `deps/components` and is not in a pack yet |
| `paxi` | online doc, modlist.md, atlas, full-test | adopted onto the list 2026-08-27; it came from `deps/components` and is not in a pack yet |
| `sparkles` | online doc, modlist.md, atlas, full-test | adopted onto the list 2026-08-27; it came from `deps/components` and is not in a pack yet |
| `vanity-core` | online doc, modlist.md, atlas, full-test | added 2026-08-28 - Malum: Vestis requires it |
| `berlords_carving` | online doc, modlist.md, atlas, deps manifest | ours - the third-party lists never covered our own mods |
| `fdshaderfix` | online doc, modlist.md, atlas, deps manifest | ours - the third-party lists never covered our own mods |
| `frozenregfix` | online doc, modlist.md, atlas, deps manifest | ours - the third-party lists never covered our own mods |
| `berlords-food-system` | online doc, modlist.md, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `alexscavesworldgenfix` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `bertie-blackhole` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `bertie-emi` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `bertie-filters` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `bertie-progression` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `bertie-tiers` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `bertie-weapons` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `bertietoolcraft` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `bush-tweaks` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `cataclysm-fortresses` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `config-migrations` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `cultural-delights-fix` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `dread-queen` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `explode-to-mine` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `fart-bomb` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `forge-ink` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `hephaestus-architecture` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `primitive-refined` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `rustic-engineer-fix` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `screenshot-copy` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `short-circuit-fix` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `voidfog` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
| `withering-waver` | online doc, modlist.md, atlas, full-test, deps manifest | ours - the third-party lists never covered our own mods |
