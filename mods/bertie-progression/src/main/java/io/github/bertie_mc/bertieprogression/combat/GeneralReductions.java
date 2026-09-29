package io.github.bertie_mc.bertieprogression.combat;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.sweenus.simplyswords.util.HelperMethods;

public final class GeneralReductions {
    private static final Set<String> SIMPLY_SWORDS =
            Set.of("simplyswords:voidcloak", "simplyswords:ribbonwrath", "simplyswords:soultether");

    private GeneralReductions() {}

    public static boolean isMovedEffect(ResourceLocation id) {
        return SIMPLY_SWORDS.contains(id.toString());
    }

    public static double apply(LivingEntity target, DamageFamily family, double damage) {
        if (family == DamageFamily.PURE || damage <= 0) return damage;
        int cloak = CombatRegistry.effectLevel(target, "simplyswords:voidcloak");
        if (cloak > 0) {
            damage *= Math.max(0, 1 - cloak * 0.1);
            HelperMethods.decrementStatusEffect(
                    target, CombatRegistry.effect("simplyswords:voidcloak").orElseThrow());
        }
        if (CombatRegistry.effectLevel(target, "simplyswords:ribbonwrath") > 0) damage *= 0.85;
        if (CombatRegistry.effectLevel(target, "simplyswords:soultether") > 0) damage *= 0.5;
        return damage;
    }
}
