package io.github.bertie_mc.bertieprogression.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches Cataclysm's private cooking counters on the Altar of Amethyst.
 *
 * <p>Must be an INTERFACE. Accessors are mixed into the target and reached by casting the target
 * instance to this type; casting to the mixin CLASS instead does not work, because that class never
 * exists at runtime.
 */
@Mixin(targets = "com.github.L_Ender.cataclysm.blockentities.AltarOfAmethyst_Block_Entity", remap = false)
public interface AltarOfAmethystAccessor {

    @Accessor("cookingTime")
    int bertieprogression$cookingTime();

    @Accessor("cookingTime")
    void bertieprogression$setCookingTime(int value);

    @Accessor("cookingTimeTotal")
    int bertieprogression$cookingTimeTotal();
}
