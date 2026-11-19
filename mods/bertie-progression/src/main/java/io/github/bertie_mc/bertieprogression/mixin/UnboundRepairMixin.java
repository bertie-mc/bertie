package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.compat.UnboundData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "me.moltea.unbound.mc.item.ModItem", remap = false)
public abstract class UnboundRepairMixin {
    @Redirect(
            method = "isValidRepairItem",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"),
            require = 1)
    private boolean bertie$canonicalRepair(ItemStack stack, Item material) {
        return stack.is(UnboundData.repairMaterial(material));
    }
}
