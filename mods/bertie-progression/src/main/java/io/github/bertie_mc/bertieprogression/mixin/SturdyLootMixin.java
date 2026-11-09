package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.fountain.SturdyDeepslateRules;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class SturdyLootMixin {
    @Inject(method = "getLootTable", at = @At("HEAD"), cancellable = true)
    private void bertieprogression$sturdyLoot(CallbackInfoReturnable<ResourceKey<LootTable>> cir) {
        if (SturdyDeepslateRules.applies((Block) (Object) this)) {
            cir.setReturnValue(
                    ResourceKey.create(Registries.LOOT_TABLE, SturdyDeepslateRules.ID.withPrefix("blocks/")));
        }
    }
}
