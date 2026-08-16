package io.github.bertie_mc.bertieprogression.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import io.github.bertie_mc.bertieprogression.crucible.CrucibleTransmutation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns the basin itself into a Zardius Crucible when the crucible recipe lands.
 *
 * <p>{@code apply} is the single point where a finished basin craft is committed - the mixer has
 * already run its full cycle by the time it is called - and it is called twice: once to simulate
 * (asking whether the results would fit) and once for real. Only the real pass is taken over, so
 * Create's own accounting decides whether the craft may start at all.
 *
 * <p>Returning true tells the mixer the craft succeeded, which is what stops it re-running against
 * a basin that no longer exists.
 */
@Mixin(value = BasinRecipe.class, remap = false)
public abstract class BasinRecipeMixin {

    @Inject(
            method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;"
                    + "Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void bertieprogression$becomeCrucible(
            BasinBlockEntity basin, Recipe<?> recipe, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (simulate || !CrucibleTransmutation.isCrucibleRecipe(recipe)) {
            return;
        }
        Level level = basin.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        CrucibleTransmutation.transform(level, basin);
        cir.setReturnValue(true);
    }
}
