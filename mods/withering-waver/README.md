# withering-waver

Adds the **Withering Waver**, a rare wither skeleton variant. NeoForge 1.21.1.

## The mob

- Replaces 1 in 30 fresh wither skeleton spawns (natural, chunk-gen and structure spawns
  only), so it appears exactly where wither skeletons do, thirty times rarer.
- 120 HP, 10 armor, 6 armor toughness, 30% magic resistance (damage in the
  `witch_resistant_to` tag), immune to wither and fire. XP 25.
- Main attack: fires vanilla wither skulls (which apply the wither effect on hit),
  range 35 blocks.
- The whole skeleton family (vanilla, `#minecraft:skeletons`, AbstractSkeleton mods,
  other Wavers) and the Waver never damage each other — that includes its own skull
  explosions splashing back. Reap/assimilate kills are the one exception.
- Ability priority: assimilate first when below 50% HP, reap when fewer than 3 skulls
  orbit, assimilate opportunistically when unarmored, summon when nothing is reapable.

### Reaping
Cloak lifts, then every wither skeleton within 15 blocks dies instantly — those deaths
never drop a skull — and their skulls fly into an orbit around the Waver, on a circle
tilted 30° off horizontal whose radius grows with the skull count. Usable while skulls
are already orbiting. Once every skull is seated and a target is in range with line of
sight, the skulls accelerate for 2 s (hood drops back, hand points), then fire one by
one every 0.8 s. A fired skull disintegrates into 8 black shrapnel pieces: any piece
hitting applies Wither II for 8 s; more than 60% of a skull's pieces hitting upgrades
that to Wither III. Firing stops after 1 s of lost line of sight (skulls keep
orbiting). 12 s cooldown.

### Assimilate
Same cloak lift, but the cloak stays on and the hand grabs. Every non-wither skeleton
within 20 blocks (skeletons, strays, bogged, modded variants) is disassembled into the
Waver: full heal, and per skeleton +10% max HP, +2 armor +10%, +1 toughness +10% — the
percentages taken from its live unbuffed stats, so mob-scaling mods (Apotheosis,
L2Hostility) scale the plating too. Flat +30% magic resistance (to 60%) while armored.
Recasting while armored repeats the heal and stacks the buffs. When the bonus health is
depleted the white bone plates crumble and every bonus disappears. Victims drop
nothing. 25 s cooldown.

### Summon
Used when no wither skeletons are in radius: raises a hand and summons 3 wither
skeletons. They can never drop anything (items or XP), whoever kills them, and Reaping
is locked for 7 s after the cast. 30 s cooldown.

### Drops
- Wither skeleton loot at triple counts (coal, bones), never a stone sword.
- Wither skull at double the vanilla chance (5% + 2%/looting level, player kills).
- +1 guaranteed wither skull if killed while skulls are orbiting it.

## Verification state

- `gradle build` and the four GameTests (`runGameTestServer`) cover: base stats and the
  30% magic reduction, reap kill/orbit/no-skull-drop, assimilate buffs and crumble
  numbers, summon count/no-drops/reap-lock.
- Not exercised by tests: the client model/animations, orbit visuals, shrapnel flight
  feel, the 1-in-30 natural replacement (needs a real world), and volley line-of-sight
  behaviour. Those need an in-game look.

Textures were generated from `custom-textures/make_withering_waver.py`; iterate there,
not in `assets/`.
