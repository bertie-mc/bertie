package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.ore.ArmageddonOreRules;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootTable.class)
public abstract class ArmageddonOreLootMixin {
    private boolean bertieprogression$denied(LootContext context) {
        var state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        ResourceLocation block = state == null
                ? ArmageddonOreRules.blockFromLootTable(((LootTable) (Object) this).getLootTableId())
                : ArmageddonOreRules.blockId(state);
        if (block != null && ArmageddonOreRules.requiredAdvancement(block) != null) {
            var tool = context.getParamOrNull(LootContextParams.TOOL);
            if (state == null
                    || tool == null
                    || tool.isEmpty()
                    || context.hasParam(LootContextParams.EXPLOSION_RADIUS)
                    || !tool.isCorrectToolForDrops(state)) return true;
        }
        return !ArmageddonOreRules.allows(block, context.getParamOrNull(LootContextParams.THIS_ENTITY));
    }

    // The common return path is after NeoForge's global modifiers, including direct machine loot queries.
    @Inject(
            method =
                    "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1)
    private void bertieprogression$guardDrops(
            LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        if (bertieprogression$denied(context)) cir.setReturnValue(new ObjectArrayList<>());
    }

    @Inject(
            method =
                    "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void bertieprogression$guardRawDrops(LootContext context, Consumer<ItemStack> consumer, CallbackInfo ci) {
        if (bertieprogression$denied(context)) ci.cancel();
    }
}
