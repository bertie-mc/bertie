package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.ender.EyeAcquisitions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrushableBlockEntity.class)
public abstract class EyeArchaeologyMixin {
    @Shadow
    private ResourceKey<LootTable> lootTable;

    @Shadow
    private ItemStack item;

    @Unique
    private boolean bertie$newSandLoot;

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void bertie$beforeLoot(Player player, CallbackInfo ci) {
        var self = (BrushableBlockEntity) (Object) this;
        bertie$newSandLoot = lootTable != null && self.getBlockState().is(Blocks.SUSPICIOUS_SAND);
    }

    @Inject(method = "unpackLootTable", at = @At("RETURN"))
    private void bertie$oldEye(Player player, CallbackInfo ci) {
        var self = (BrushableBlockEntity) (Object) this;
        if (bertie$newSandLoot && self.getLevel() instanceof ServerLevel level && level.random.nextDouble() < 0.02) {
            ItemStack result = EyeAcquisitions.eye("old_eye");
            if (!result.isEmpty()) item = result;
        }
        bertie$newSandLoot = false;
    }
}
