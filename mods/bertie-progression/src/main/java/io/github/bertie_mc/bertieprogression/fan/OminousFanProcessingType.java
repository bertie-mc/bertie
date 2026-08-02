package io.github.bertie_mc.bertieprogression.fan;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import io.github.bertie_mc.bertieprogression.recipe.ModRecipes;
import io.github.bertie_mc.bertieprogression.recipe.OminousFanRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A fifth Create fan-processing type, on top of the stock four (splashing = water, smoking = fire,
 * haunting = soul fire, blasting = lava). This one triggers on Twilight Forest's OMINOUS FIRE.
 *
 * <p>Conversions are data-driven through {@link OminousFanRecipe}; Bertie Progression's built-in
 * EMI plugin presents the same recipes under its Ominous Fan Blowing category.
 */
public class OminousFanProcessingType implements FanProcessingType {

    private static final ResourceLocation OMINOUS_FIRE =
            ResourceLocation.fromNamespaceAndPath("twilightforest", "ominous_fire");

    private static Optional<RecipeHolder<OminousFanRecipe>> find(ItemStack stack, Level level) {
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.OMINOUS_FAN_TYPE.get(), new SingleRecipeInput(stack), level);
    }

    @Override
    public boolean isValidAt(Level level, BlockPos pos) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        return OMINOUS_FIRE.equals(id);
    }

    @Override
    public int getPriority() {
        // Above the stock four. Ominous fire is still a fire block, so smoking would otherwise
        // claim it first and the type would never be reached.
        return 1000;
    }

    @Override
    public boolean canProcess(ItemStack stack, Level level) {
        return find(stack, level).isPresent();
    }

    @Override
    public List<ItemStack> process(ItemStack stack, Level level) {
        return find(stack, level)
                .map(h -> List.of(h.value().assemble(new SingleRecipeInput(stack),
                        level.registryAccess())))
                .orElse(Collections.emptyList());
    }

    @Override
    public void spawnProcessingParticles(Level level, Vec3 pos) {
        if (level.random.nextInt(8) != 0) {
            return;
        }
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.25, pos.z, 0, 0.05, 0);
    }

    @Override
    public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
        particleAccess.setColor(0x6A0DAD);
        particleAccess.setAlpha(0.5f);
        if (random.nextInt(32) == 0) {
            particleAccess.spawnExtraParticle(ParticleTypes.SOUL, 0.5f);
        }
    }

    @Override
    public void affectEntity(Entity entity, Level level) {
        // Deliberately inert for the proof of concept - the point is item processing, not a hazard.
    }
}
