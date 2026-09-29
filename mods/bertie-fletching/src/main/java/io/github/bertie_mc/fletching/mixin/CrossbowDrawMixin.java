package io.github.bertie_mc.fletching.mixin;

import io.github.bertie_mc.fletching.ArrowProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CrossbowItem.class, remap = false)
public abstract class CrossbowDrawMixin {
    @Inject(method = "getChargeDuration", at = @At("RETURN"), cancellable = true)
    private static void bertie$charge(ItemStack crossbow, LivingEntity shooter, CallbackInfoReturnable<Integer> cir) {
        if (shooter instanceof Player player) {
            var data = player.getProjectile(crossbow)
                    .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag();
            if (ArrowProfile.redesigned(data) && ArrowProfile.read(data).tip("gargoyle"))
                cir.setReturnValue((int) Math.ceil(cir.getReturnValue() / .8));
        }
    }
}
