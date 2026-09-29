# Combat integration

The combat package provides one damage classification and shared defense calculations
for the pinned Bertie pack. Optional adapters target the installed mod versions. They
do not replace attack amounts, cooldowns, equipment progression or encounter scripts.

## Families and properties

| Family | Numeric defenses |
| --- | --- |
| Physical | Armor, Protection, applicable physical percentage reductions |
| Magic | Iron's general × school Spell Resistance, armor, Protection |
| Energy | Percentage Energy Resistance, Protection, applicable late flat subtraction |
| Pure | Enigmatic Pure Resistance only; ordinary mitigation is skipped |

Fire, Explosion, Projectile, Poison, freezing and fall remain secondary properties.
Specialist protection contributes to the same Protection sum; it does not create a
second enchantment layer. Primary Iron spell damage excludes ordinary elemental and
projectile specialist defenses, regardless of its visuals. Ordinary ignition and
poison applied by a spell remain separate effects. Living summons use native attacks.

Spell provenance follows nonliving entities spawned by Iron casting callbacks and
their children. A cached marker and persisted caster/school identify borrowed boss
projectiles and fields without scanning the world. Missing native Spell Resistance
is applied once; the normal Iron damage helper is not applied a second time.

Thorns and fall are Energy; Bleeding is Physical. Genuine drowning, suffocation, void
and HP payments are Pure. Real non-Iron explosions are Physical + Explosion. FD boss
damage is Pure and retains encounter gates. Maledictus's selected soul finisher is
Pure; the rest of Cataclysm is not broadly promoted to Pure.

Damage IDs remain intact for Adaptive, death messages and source-specific behavior.
Tags under `data/bertieprogression/tags/damage_type/combat` provide explicit families.
Context routing separates reused upstream IDs for Bleeding/Detonation, Flameseed's
burn/burst, Brimstone, Emberblade, Void Flame, Scorchfang and Whisper of Poison.
An unknown source falls back to its native tags. Pure does not globally disable hit
cooldowns; only settled debt/payment types receive that override.

## Formulas

For incoming damage `D` and nonnegative effective armor `A`:

```text
E = max(D - 20, 0)
K = 8 + 8E / (E + 40)
damage after armor = D × K / (K + A)
damage after Protection = damage × 10^(-P / 40)
```

`P` includes ordinary Protection and matching specialist points: two per Fire,
Blast or Projectile Protection level, three per Feather Falling level. Gems above
40 continue the curve. Ordinary Protection's book/anvil maximum is X; its loot
generation limit remains IV. Negative armor keeps Apothic's separate amplification
branch. Toughness counters pierce, shred, Breach and registered hostile effect armor
penalties by `min(0.02 × toughness, 0.6)`; it no longer counters hit size.

Independent positive Energy resistance percentages multiply remaining damage.
Stronghold armor is one additive set source: 25 percentage points per piece, 100% at
four pieces. Weakness subtracts percentage points from combined resistance, allowing
vulnerability below zero. For example, two 50% reductions and a 50-point weakness
leave 25% resistance. Malstone retains its native cap. Malstone Physical Resistance
applies to the same families as armor and multiplies with it.

Waver has 30% Energy resistance, or 60% after assimilation. Witch keeps 85%.
Forgotten Ice supplies independent 30% Energy and 30% Projectile reductions.
Blazing Brand, Rend and Hazen Ichor remove armor only. Enigmatic Ichor retains its
other penalties but no toughness loss. Retired saved effect modifiers are removed
when an entity joins the level.

## Damage accounting

The integration collects legacy percentage contributions on the current
`DamageContainer`, then completes the common Energy layer after the ordinary
pre-damage event. Late amplifiers and caps use remaining damage, not the original
hit. Defense callbacks, flat subtraction, transfers and debts complete before HP
pools. State belongs to each nested hit; short-lived casting/healing/redirect
contexts use balanced thread-local scopes.

```text
numeric mitigation
→ Mageslayer conversion / retained Soul Ward handling
→ flat subtraction (floor zero)
→ retained transfers / deferred damage
→ Ethereal Lantern
→ absorption hearts
→ Azure Dike
→ health
```

Pure bypasses Soul Ward, Aegis, Bloodglass, Decay, Etherium, powered MekaSuit, ordinary
Resistance, conditional reductions, subtraction, Dodge and Evasion. Pure Resistance
is an explicit reduction exception; Lantern, absorption and Dike are finite HP-pool
exceptions. Boss phase protection stays native. L2's ownerless immunity deliberately
retains its original raw-flag eligibility, including eligible Pure hits.

Etherium keeps its low-health reduction and broadens its existing threshold-based
immunity to ordinary projectiles. Lantern is a distinct finite pool. Dike is charged
after yellow hearts; already prepaid special paths are not charged again. MekaSuit
uses its native power/module conditions for all non-Pure families.

Damage sharing preserves family and attacker and lets the recipient apply its own
defenses. Actual reflection preserves family/properties and credits the reflector.
A redirect marker prevents recursive transfer/reflection loops. Fixed retaliation
attacks keep their own types. Native shares, amounts and refund conditions remain.

Heartstop buffers half of post-defense damage. Patience buffers half with a 20%
surcharge, retaining its repayment timing and 1-HP floor. Their repayments and
Crystallization's final penalty are Pure. Wither explosion healing deliberately
retains its original unreduced basis.

## Recovery and avoidance

Non-L2 ordinary lifesteal rates add and use Physical direct melee damage against
living creatures, after defenses and absorption, capped at pre-hit HP. Excess
healing does not become absorption. Iron spell lifesteal and Apotheosis Overheal
remain separate native mechanics.

Non-L2 healing modifiers add/subtract percentage points around 100%, floored at
zero. Audited conditional percentage providers are evaluated once and their native
second application is suppressed. Hard cancellations such as Fear remain hard
stops. Simply More Bleeding subtracts 50 points through normal healing. L2's healing
multiplier is not merged in this pass.

Passive Apothic, Terra Curio and Enigmatic Dodge contributions multiply remaining
hit chances and only protect Physical, excluding fall. Iron Evasion remains separate;
Pure, damaging status ticks, contact fire/lava and self-damage do not consume it.
Enigmatic deflection and Pastel rebound exclude tracked Iron spell projectiles.

## Deliberate limits

- L2 strength redesign, Dispel's replacement removal amount, Void Touch and Imagine
  Breaker remain separate. L2's existing explicit damage variants retain their native
  bypass flags. Its shared family filters, reflection and flat-subtraction ordering
  are integrated without inventing new numerical values.
- The detailed armor/toughness/penetration strength audit remains separate.
- Most Malum pact redesign remains separate. Arcanaphage's input filter is Physical;
  its existing damage replay remains until that redesign.
- Primordial Fire, Moonstone Strike, Lich, Ghast/Wither components, laser interception
  and Yeti's narrow projectile distinctions have no bespoke normalization patch.
- Living Bomb's fallback damage, narrow lightning healing and encounter phase
  gates retain their native behavior.
- Ace remains installed. Its Technomancy school already uses its real resistance
  stat; Cataclysm's duplicate language label is not a registered stat. Inert bonuses
  for removed schools require a later content decision.
- Useful Magic is disabled and absent from pack membership; no adapter is shipped.
  Removed Drowned Fire player sources are not re-enabled. The unverified Sirena
  reference does not identify a mechanic and has no patch.

## Verification

`CombatMathTest` checks formulas, percentage composition, flat subtraction, healing
and lifesteal bounds. `CombatGameTests` exercises actual NeoForge damage and healing
with the relevant optional mods, including family properties, native spell resistance,
Stronghold, Waver, Aegis, Mageslayer, pool order, debt, transfer and passive avoidance.
The compatibility-target fixture loads every available optional target so a dormant
item cannot conceal an invalid injection until first use.

Tests use a dedicated logical server. They do not certify every visual effect,
equipment combination or multiplayer encounter, and do not replace live balance
testing. No per-hit world scan or background task is introduced.
