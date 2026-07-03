package com.berlord.foodsystem.mixin;

import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the two UpgradeWrapperBase fields the feeding wake needs:
 *  - {@code cooldown} (an absolute gameTime long): writing 0 makes isInCooldown() false, so
 *    the feeder runs its scan on the next tick — this is how we "force start" a scan.
 *  - {@code storageWrapper}: to register an inventory-change listener on the backpack.
 * Applied only when sophisticatedcore is installed (see BfsMixinPlugin).
 */
@Mixin(value = UpgradeWrapperBase.class, remap = false)
public interface UpgradeWrapperBaseAccessor {

    @Accessor("cooldown")
    void bfs$setCooldown(long value);

    @Accessor("storageWrapper")
    IStorageWrapper bfs$storageWrapper();
}
