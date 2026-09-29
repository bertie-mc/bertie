package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.config.ModConfig;
import com.fletchery.mod.item.CustomArrowItem;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CustomArrowItem.class, remap = false)
public abstract class ArrowTooltipMixin {
    @Inject(method = "buildFeatherTooltip", at = @At("HEAD"), cancellable = true)
    private void bertie$wheatPercentage(String feather, ModConfig config, CallbackInfoReturnable<Component> cir) {
        if (feather.equals("minecraft:wheat") || feather.equals("item.minecraft.wheat") || feather.equals("wheat")) {
            cir.setReturnValue(Component.translatable(
                    "fletchery_expanded.tooltip.feather.wheat",
                    String.format(Locale.ROOT, "%.0f", (1.0F - config.wheatSpeed) * 100.0F),
                    String.format(Locale.ROOT, "%.0f", config.wheatGravity * 100.0F)));
        }
    }

    @Redirect(
            method = "appendHoverText",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;getBoolean(Ljava/lang/String;)Z"))
    private boolean bertie$separateTooltip(CompoundTag tag, String key) {
        return !(key.equals("isPotionEffect") && tag.getBoolean("bertieCoating")) && tag.getBoolean(key);
    }

    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void bertie$bothEffects(
            ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flags, CallbackInfo ci) {
        var tag =
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.getBoolean("bertieCoating")) return;
        PotionContents potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        potion.addPotionTooltip(lines::add, 0.125f, context.tickRate());
    }
}
