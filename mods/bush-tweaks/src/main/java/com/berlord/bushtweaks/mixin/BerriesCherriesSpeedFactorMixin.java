package com.berlord.bushtweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * All 18 B&C bush/grape blocks ship with speedFactor(0.2f) in their block
 * properties — a permanent soul-sand-style 80% slow that applies regardless
 * of crouching and stacks with any entityInside slowdown. Vanilla berry
 * bushes have no speed factor; their slow comes only from makeStuckInBlock,
 * which BerriesCherriesBushDamageMixin already applies (and skips while
 * crouching). So the native factor is neutralized here entirely.
 */
@Mixin(
        targets = {
                "net.mcreator.berriesandcherries.block.RaspberrybushBlock",
                "net.mcreator.berriesandcherries.block.RaspberryBushTopBlock",
                "net.mcreator.berriesandcherries.block.RaspberrybushnoneBlock",
                "net.mcreator.berriesandcherries.block.RaspberrybushnonetopBlock",
                "net.mcreator.berriesandcherries.block.StrawberrybushBlock",
                "net.mcreator.berriesandcherries.block.StrawberryBushNoneBlock",
                "net.mcreator.berriesandcherries.block.BlueberryBushBottomBlock",
                "net.mcreator.berriesandcherries.block.BlueberryBushTopBlock",
                "net.mcreator.berriesandcherries.block.NoneBlueberryBushBottomBlock",
                "net.mcreator.berriesandcherries.block.NoneBlueberryBushTopBlock",
                "net.mcreator.berriesandcherries.block.GreenGrapeTreeBottomBlock",
                "net.mcreator.berriesandcherries.block.GreenGrapeTreeTopBlock",
                "net.mcreator.berriesandcherries.block.NoneGreenGrapeTreeBottomBlock",
                "net.mcreator.berriesandcherries.block.NoneGreenGrapeTreeTopBlock",
                "net.mcreator.berriesandcherries.block.BlackGrapeTreeBottomBlock",
                "net.mcreator.berriesandcherries.block.BlackGrapeTreeTopBlock",
                "net.mcreator.berriesandcherries.block.NoneBlackGrapeTreeBottomBlock",
                "net.mcreator.berriesandcherries.block.NoneBlackGrapeTreeTopBlock"
        },
        remap = false
)
public class BerriesCherriesSpeedFactorMixin {

    // merged into each target class, overriding BlockBehaviour.getSpeedFactor
    public float getSpeedFactor() {
        return 1.0F;
    }
}
