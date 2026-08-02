package com.berlord.bertie_progression.fan;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

/**
 * A fifth Create fan-processing type, on top of the stock four (splashing = water, smoking = fire,
 * haunting = soul fire, blasting = lava). This one triggers on Twilight Forest's OMINOUS FIRE.
 *
 * <p>PROOF OF CONCEPT (berlord 2026-08-01). The conversion table is hard-coded to one entry -
 * anything in {@code #minecraft:leaves} becomes a Steeleaf Ingot - deliberately, so the type itself
 * can be proven in game before a recipe type, serialiser and EMI category are built around it.
 * Because there is no recipe behind it, <b>this conversion does not appear in EMI or JEI at all</b>;
 * it only happens when a fan actually blows through ominous fire.
 */
public class OminousFanProcessingType implements FanProcessingType {

    private static final ResourceLocation OMINOUS_FIRE =
            ResourceLocation.fromNamespaceAndPath("twilightforest", "ominous_fire");
    private static final ResourceLocation STEELEAF_INGOT =
            ResourceLocation.fromNamespaceAndPath("twilightforest", "steeleaf_ingot");

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
        return stack.is(ItemTags.LEAVES);
    }

    @Override
    public List<ItemStack> process(ItemStack stack, Level level) {
        Item out = BuiltInRegistries.ITEM.getOptional(STEELEAF_INGOT).orElse(Items.AIR);
        if (out == Items.AIR) {
            return Collections.emptyList();
        }
        return List.of(new ItemStack(out, 1));
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
