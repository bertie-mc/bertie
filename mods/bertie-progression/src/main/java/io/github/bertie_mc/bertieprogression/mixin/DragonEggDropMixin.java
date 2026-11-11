package io.github.bertie_mc.bertieprogression.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "com.iafenvoy.iceandfire.entity.DragonBaseEntity", remap = false)
public abstract class DragonEggDropMixin {
    @Shadow
    public abstract int getDragonStage();

    @ModifyArg(
            method = "mobInteract",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"),
            index = 0,
            require = 1)
    private ItemStack bertie$removeStageFourEgg(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return getDragonStage() == 4
                        && id.getNamespace().equals("iceandfire")
                        && id.getPath().startsWith("dragonegg_")
                ? ItemStack.EMPTY
                : stack;
    }
}
