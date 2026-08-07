package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.forge.BrickForgeBonus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pays out {@link BrickForgeBonus} when Slag's Brick Forge finishes a smelt.
 *
 * <p>Targeted by STRING, and the forge parameter carries {@link Coerce}, for the same reason the
 * Altar of Amethyst mixin does: Slag is an optional runtime dependency and its {@code ForgeBE} is
 * not on this module's compile classpath.
 *
 * <p>{@code burn} is the one place a completed smelt is visible with its recipe still in hand. It
 * returns true exactly when a result was produced, so the roll happens once per craft.
 *
 * <p>The bonus is DROPPED rather than pushed into the output slot. The slot is already holding the
 * craft's own result and may be full or about to be pulled by a hopper; a drop is never silently
 * swallowed, and a block landing on the forge is how the player learns this exists at all.
 */
@Mixin(targets = "dev.lopyluna.slag.content.blocks.forge.ForgeBE", remap = false)
public abstract class ForgeBEMixin {

    @Inject(method = "burn", at = @At("RETURN"), remap = false)
    private static void bertieprogression$oreBonus(
            RegistryAccess access,
            RecipeHolder<?> recipe,
            net.minecraft.core.NonNullList<ItemStack> items,
            int maxStackSize,
            @Coerce BlockEntity forge,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || recipe == null) {
            return;
        }
        Level level = forge.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack result = recipe.value().getResultItem(access);
        ItemStack bonus = BrickForgeBonus.roll(result, level.random);
        if (bonus.isEmpty()) {
            return;
        }
        BlockPos pos = forge.getBlockPos();
        Containers.dropItemStack(level, pos.getX(), pos.getY() + 1, pos.getZ(), bonus);
    }
}
