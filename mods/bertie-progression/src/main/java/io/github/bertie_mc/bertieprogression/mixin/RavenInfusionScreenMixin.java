package io.github.bertie_mc.bertieprogression.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bertie_mc.bertieprogression.enchanting.RavenInfusion;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentScreen", remap = false)
public abstract class RavenInfusionScreenMixin {
    @WrapOperation(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/resources/language/I18n;get(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"),
            require = 1)
    private String bertie$ravenCostTooltip(String key, Object[] arguments, Operation<String> original) {
        var menu = ((AbstractContainerScreen<?>) (Object) this).getMenu();
        if ("container.enchant.level.many".equals(key)
                && RavenInfusion.isInput(menu.getSlot(0).getItem())) {
            return original.call(key, new Object[] {RavenInfusion.LEVEL_COST});
        }
        return original.call(key, arguments);
    }
}
