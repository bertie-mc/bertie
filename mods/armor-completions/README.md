# Armor Completions

The leggings and boots six of the pack's armour sets never shipped with, made to
match, so each one can be worn as a full set.

| Set | Mod | Added |
|---|---|---|
| Spiny Shell | Born in Chaos | Leggings, boots |
| Bishop of Deceit | Haze n Stuff | Leggings, boots |
| Bone Reptile | L_Ender's Cataclysm | Leggings, boots |
| Pyromancer Brute | Haze n Stuff | Leggings, boots |
| Nameless One | Haze n Stuff | Boots |
| Necromancer | Haze n Stuff | Boots |

Ten items, all in the `armorcompletions` namespace — `armorcompletions:bishop_leggings`
and so on. The **Armor Completions** creative tab holds all six sets together,
the fourteen original pieces included.

Spiny Shell and Bone Reptile use native humanoid models. The four Haze n Stuff
sets go through the Iron's Spellbooks and GeckoLib armour renderer that draws
the originals, so a finished set reads as one piece of work rather than two.
The Bishop's robe panels follow the legs through its renderer's torso-extension
bones, and the Nameless One and Necromancer boots correct the left/right
attachment their original leggings had; those fixes apply to the upstream items
as well as the new ones.

The Bishop, Bone Reptile, Pyromancer Brute, Nameless One and Necromancer boots
have taller shafts, stepped closed toes, heels and layered cuffs. Cuboids are at
least one model unit thick, matching the normal Minecraft pixel scale. Each face maps
to enough existing atlas pixels for its physical size, avoiding stretched detail.
The texture PNGs and inventory icons are unchanged. Spiny Shell keeps its original
boot model. `tools/remodel_boots.py` regenerates the five boot geometries and UVs
from the existing atlases.

Armour class, material, spell attributes and durability all come from the set
being completed. Spiny Shell's own material declares zero defence for the leg
and boot slots it never used, and those zeroes stand until the set is balanced
as a whole. The pieces have no recipes yet, and no set bonus of their own.

## Checks

`gradle :mods:armor-completions:test :mods:armor-completions:runGameTestServer`

The unit tests bake the Java and GeckoLib geometry, resolve the inventory
sprites, check the redirect targets against the original items and drive the
armour renderer with asymmetric leg poses. The game test registers all ten
items, checks each one's material and slot, and equips them on an armour stand.
Boot-specific checks verify full-height geometry, opaque UV regions, pixel density
and native vertex output.
