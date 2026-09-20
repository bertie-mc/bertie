package io.github.bertie_mc.creatures.server.block;

import io.github.bertie_mc.creatures.server.misc.ACSoundRegistry;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

/**
 * Custom sound types for Alex's Caves blocks.
 * Uses LazySoundType to defer sound event resolution until after registration.
 */
public class ACSoundTypes {
    public static final SoundType FLOOD_BASALT = new LazySoundType(
            1.0F,
            1.0F,
            ACSoundRegistry.FLOOD_BASALT_BREAK,
            ACSoundRegistry.FLOOD_BASALT_STEP,
            ACSoundRegistry.FLOOD_BASALT_PLACE,
            ACSoundRegistry.FLOOD_BASALT_BREAKING,
            ACSoundRegistry.FLOOD_BASALT_STEP);
    public static final SoundType THORNWOOD_BRANCH = new LazySoundType(
            1.0F,
            1.0F,
            ACSoundRegistry.THORNWOOD_BRANCH_BREAK,
            () -> SoundEvents.MANGROVE_ROOTS_STEP,
            () -> SoundEvents.MANGROVE_ROOTS_PLACE,
            () -> SoundEvents.MANGROVE_ROOTS_HIT,
            () -> SoundEvents.MANGROVE_ROOTS_FALL);

    private static class LazySoundType extends SoundType {
        private final Supplier<SoundEvent> breakSoundSupplier;
        private final Supplier<SoundEvent> stepSoundSupplier;
        private final Supplier<SoundEvent> placeSoundSupplier;
        private final Supplier<SoundEvent> hitSoundSupplier;
        private final Supplier<SoundEvent> fallSoundSupplier;

        public LazySoundType(
                float volume,
                float pitch,
                Supplier<SoundEvent> breakSound,
                Supplier<SoundEvent> stepSound,
                Supplier<SoundEvent> placeSound,
                Supplier<SoundEvent> hitSound,
                Supplier<SoundEvent> fallSound) {
            // Pass dummy sounds to parent - we override all methods anyway
            super(
                    volume,
                    pitch,
                    SoundEvents.STONE_BREAK,
                    SoundEvents.STONE_STEP,
                    SoundEvents.STONE_PLACE,
                    SoundEvents.STONE_HIT,
                    SoundEvents.STONE_FALL);
            this.breakSoundSupplier = breakSound;
            this.stepSoundSupplier = stepSound;
            this.placeSoundSupplier = placeSound;
            this.hitSoundSupplier = hitSound;
            this.fallSoundSupplier = fallSound;
        }

        @Override
        public SoundEvent getBreakSound() {
            return breakSoundSupplier.get();
        }

        @Override
        public SoundEvent getStepSound() {
            return stepSoundSupplier.get();
        }

        @Override
        public SoundEvent getPlaceSound() {
            return placeSoundSupplier.get();
        }

        @Override
        public SoundEvent getHitSound() {
            return hitSoundSupplier.get();
        }

        @Override
        public SoundEvent getFallSound() {
            return fallSoundSupplier.get();
        }
    }
}
