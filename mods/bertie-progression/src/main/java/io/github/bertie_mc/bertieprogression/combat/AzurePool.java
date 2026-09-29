package io.github.bertie_mc.bertieprogression.combat;

import earth.terrarium.pastel.attachments.data.azure_dike.AzureDikeProvider;
import earth.terrarium.pastel.items.trinkets.PastelTrinketItem;
import earth.terrarium.pastel.registries.PastelDamageTypes;
import earth.terrarium.pastel.registries.PastelEntityTypeTags;
import earth.terrarium.pastel.registries.PastelItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class AzurePool {
    private AzurePool() {}

    public static float consume(LivingEntity target, DamageSource source, float damage) {
        // The retained Primordial Fire path has already paid this pool outside hurt().
        if (source.is(PastelDamageTypes.PRIMORDIAL_FIRE) && !target.getType().is(PastelEntityTypeTags.SOULLESS))
            return damage;
        float remainder = AzureDikeProvider.absorbDamage(target, damage);
        if (DamageFamilies.isPure(source)
                && PastelTrinketItem.hasEquipped(target, PastelItems.AZURESQUE_DIKE_CORE.get())) {
            // Pure can spend shield HP, but does not inherit the core's separate vulnerability.
            remainder *= 0.5F;
        }
        return remainder;
    }
}
