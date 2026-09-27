# Enchanting Reroll Cost

Sets Easy Magic + Apothic Enchanting Compat rerolls to **one lapis lazuli plus the
XP-point price of the minimum first enchanting option**. The reference offer uses
20% of the table's effective Eterna, with Apothic's rounding and minimum level.

| Effective power | XP points per reroll | Lapis |
| --- | --- | --- |
| No shelves | 6 | 1 |
| 10 | 8 | 1 |
| 30 | 16 | 1 |
| 60 | 28 | 1 |
| 100 | 56 | 1 |

The price uses effective power after the player's Eterna limit and stays fixed when
only the enchanting seed changes. It applies to the vanilla, Apothic and Raven
tables. Creative players pay nothing. The button displays the same XP price that
the server checks and charges. The compatibility addon's enable switch remains
available; its fixed price and whole-level cost settings are superseded.

Ordinary enchanting still changes the player's enchanting seed. A weaker table
can still provide cheaper rerolls. This patch does not change orbiting items,
enchantment selection or item storage.

Requires Minecraft 1.21.1, NeoForge, Apothic Enchanting 1.6.x, and Easy Magic +
Apothic Enchanting Compat 1.0.x with its dependencies. No dependency JAR or asset is
bundled.

Build and check with `gradle :mods:enchanting-reroll-cost:build` and
`gradle :mods:enchanting-reroll-cost:runClientTests`.

Client tests cover all three tables at power 0, 30 and 100, a lower player Eterna
cap, displayed prices, repeated rerolls, exact-balance payments, rejected packets
with insufficient XP or lapis, and creative mode. They also check that the legacy
fixed-price and whole-level settings cannot override the scaled price.
