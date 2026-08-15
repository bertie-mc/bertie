package io.github.bertie_mc.witheringwaver;

import io.github.bertie_mc.witheringwaver.entity.OrbitingSkullEntity;
import io.github.bertie_mc.witheringwaver.entity.SkullShrapnelEntity;
import io.github.bertie_mc.witheringwaver.entity.WitheringWaverEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class WwEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, WitheringWaver.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<WitheringWaverEntity>> WITHERING_WAVER =
            ENTITY_TYPES.register(
                    "withering_waver",
                    () -> EntityType.Builder.of(WitheringWaverEntity::new, MobCategory.MONSTER)
                            .sized(0.72F, 2.55F)
                            .fireImmune()
                            .clientTrackingRange(10)
                            .build("withering_waver"));

    public static final DeferredHolder<EntityType<?>, EntityType<OrbitingSkullEntity>> ORBITING_SKULL =
            ENTITY_TYPES.register(
                    "orbiting_skull",
                    () -> EntityType.Builder.of(OrbitingSkullEntity::new, MobCategory.MISC)
                            .sized(0.45F, 0.45F)
                            .fireImmune()
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("orbiting_skull"));

    public static final DeferredHolder<EntityType<?>, EntityType<SkullShrapnelEntity>> SKULL_SHRAPNEL =
            ENTITY_TYPES.register(
                    "skull_shrapnel",
                    () -> EntityType.Builder.of(SkullShrapnelEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .fireImmune()
                            .clientTrackingRange(6)
                            .updateInterval(1)
                            .build("skull_shrapnel"));
}
