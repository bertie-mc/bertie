package io.github.bertie_mc.bertieprogression.mixin;

import com.refinedmods.refinedstorage.common.api.upgrade.UpgradeState;
import io.github.bertie_mc.bertieprogression.compat.RefinedFortune;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "com.refinedmods.refinedstorage.common.constructordestructor.BlockBreakDestructorStrategyFactory",
        remap = false)
public abstract class RefinedFortuneMixin {
    @Inject(method = "enchantTool", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$higherFortune(
            ServerLevel level, UpgradeState upgrades, ItemStack tool, CallbackInfo ci) {
        if (RefinedFortune.enchantTool(level, upgrades, tool)) ci.cancel();
    }
}
