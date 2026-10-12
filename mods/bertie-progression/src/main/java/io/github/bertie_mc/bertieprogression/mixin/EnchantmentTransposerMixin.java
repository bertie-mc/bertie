package io.github.bertie_mc.bertieprogression.mixin;

import auviotre.enigmatic.legacy.handlers.TooltipHandler;
import auviotre.enigmatic.legacy.registries.EnigmaticItems;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosApi;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.item.books.EnchantmentTransposer", remap = false)
public abstract class EnchantmentTransposerMixin {
    @Inject(method = "canDisenchant", at = @At("HEAD"), cancellable = true, remap = false)
    private void bertie$requireCurses(
            Player player, ItemStack tome, ItemStack target, CallbackInfoReturnable<Boolean> cir) {
        if (CuriosApi.getCuriosInventory(player)
                .flatMap(inventory -> inventory.findFirstCurio(stack -> stack.is(EnigmaticItems.CURSED_RING.get())))
                .isEmpty()) cir.setReturnValue(false);
    }

    @Inject(method = "appendHoverText", at = @At("TAIL"), remap = false)
    private void bertie$curseTooltip(
            ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        TooltipHandler.cursedOnly(tooltip, stack);
    }
}
