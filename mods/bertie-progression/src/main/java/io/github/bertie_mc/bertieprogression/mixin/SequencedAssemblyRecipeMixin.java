package io.github.bertie_mc.bertieprogression.mixin;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly;
import io.github.bertie_mc.bertieprogression.backpack.BackpackHandover;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Carries a Sophisticated storage's contents through a Create sequenced assembly.
 *
 * <p>A sequenced assembly starts by replacing the input with the recipe's transitional item and ends
 * by replacing that with the result. Both swaps build a fresh stack, so a backpack fed into one
 * comes out the far end empty. {@code advance} is where every one of those swaps happens - the first
 * step, each middle step and the finish - so one injection covers the whole sequence.
 *
 * <p>Create's own progress component is written onto the returned stack just before this runs, and
 * copying the input's components over the top would put the PREVIOUS step's progress back. It is
 * read first and restored afterwards, which also removes it on the finished item, where the input
 * had one and the result must not.
 *
 * <p>Scoped by {@link BackpackHandover#isStorage}: every other sequenced assembly in the pack feeds
 * on plain items and is untouched.
 */
@Mixin(value = SequencedAssemblyRecipe.class, remap = false)
public abstract class SequencedAssemblyRecipeMixin {

    @Inject(method = "advance", at = @At("RETURN"), remap = false)
    private void bertieprogression$carryStorage(
            ResourceLocation recipeId, ItemStack input, RandomSource random, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack out = cir.getReturnValue();
        if (out == null || out.isEmpty() || !BackpackHandover.isStorage(input)) {
            return;
        }
        SequencedAssembly progress = out.get(AllDataComponents.SEQUENCED_ASSEMBLY);
        BackpackHandover.carry(input, out);
        if (progress != null) {
            out.set(AllDataComponents.SEQUENCED_ASSEMBLY, progress);
        } else {
            out.remove(AllDataComponents.SEQUENCED_ASSEMBLY);
        }
    }
}
