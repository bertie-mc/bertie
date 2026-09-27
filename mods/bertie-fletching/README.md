# Bertie Fletching

Extends Fletchery Expanded 1.0.5 on NeoForge 1.21.1.

The fletching table produces four arrows per craft. The feather, shaft, tip and item-effect slots retain their original positions. A two-slot-high potion tank sits between the ingredient row and the effect slot.

Click the tank with a regular, splash or lingering potion to fill it and receive an empty bottle on the cursor. Shift-clicking a potion replaces it with the empty bottle in its inventory slot. One potion coats eight arrows; the tank holds enough for 64 arrows. Only identical potion contents can share a tank, regardless of bottle form. Fill level is drawn in the tank; hovering shows its contents without numerical liquid quantities.

The tank automatically coats each four-arrow batch until empty. Shift-crafting stops when the coating changes, so it does not silently produce uncoated arrows. Crafting requires space for the complete batch. Item effects and potion effects apply together; inventory, bow, crossbow and projectile visuals add potion-colored pixels over the completed item-effect arrow.

Coatings use vanilla tipped-arrow durations and preserve effect strength. Instant effects apply on impact. Tanks persist with the world and are emptied when the table is destroyed. Existing table ingredients are retained; old potions in the effect slot can be shift-clicked into the tank.

Build with `gradle :mods:bertie-fletching:build`. Run `:mods:bertie-fletching:test` and `:mods:bertie-fletching:runGameTests` for storage, crafting transaction, persistence and projectile coverage.

Original code is released under Unlicense. The screen and coating masks derive from PriestDiO's MIT-licensed Fletchery Expanded; see [NOTICE](NOTICE).
