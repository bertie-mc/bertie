package io.github.bertie_mc.creatures.server.entity;

import io.github.bertie_mc.creatures.BertieCreatures;
import io.github.bertie_mc.creatures.server.entity.item.*;
import io.github.bertie_mc.creatures.server.entity.living.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ACEntityRegistry {
    public static final DeferredRegister<EntityType<?>> DEF_REG =
            DeferredRegister.create(Registries.ENTITY_TYPE, BertieCreatures.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<AlexsCavesBoatEntity>> BOAT = DEF_REG.register(
            "boat",
            () -> EntityType.Builder.<AlexsCavesBoatEntity>of(AlexsCavesBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build("ac_boat"));
    public static final DeferredHolder<EntityType<?>, EntityType<AlexsCavesChestBoatEntity>> CHEST_BOAT =
            DEF_REG.register(
                    "chest_boat",
                    () -> EntityType.Builder.<AlexsCavesChestBoatEntity>of(
                                    AlexsCavesChestBoatEntity::new, MobCategory.MISC)
                            .sized(1.375F, 0.5625F)
                            .clientTrackingRange(10)
                            .build("ac_chest_boat"));
    public static final DeferredHolder<EntityType<?>, EntityType<GrottoceratopsEntity>> GROTTOCERATOPS =
            DEF_REG.register(
                    "grottoceratops",
                    () -> EntityType.Builder.of(GrottoceratopsEntity::new, MobCategory.CREATURE)
                            .sized(2.3F, 2.5F)
                            .setTrackingRange(8)
                            .build("grottoceratops"));
    public static final DeferredHolder<EntityType<?>, EntityType<TremorsaurusEntity>> TREMORSAURUS = DEF_REG.register(
            "tremorsaurus",
            () -> EntityType.Builder.of(TremorsaurusEntity::new, MobCategory.CREATURE)
                    .sized(2.5F, 3.85F)
                    .setTrackingRange(8)
                    .build("tremorsaurus"));
    public static final DeferredHolder<EntityType<?>, EntityType<LuxtructosaurusEntity>> LUXTRUCTOSAURUS =
            DEF_REG.register(
                    "luxtructosaurus",
                    () -> EntityType.Builder.of(LuxtructosaurusEntity::new, MobCategory.MONSTER)
                            .sized(6.0F, 8.5F)
                            .setTrackingRange(12)
                            .fireImmune()
                            .build("luxtructosaurus"));
    public static final DeferredHolder<EntityType<?>, EntityType<TephraEntity>> TEPHRA = DEF_REG.register(
            "tephra",
            () -> EntityType.Builder.<TephraEntity>of(TephraEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .fireImmune()
                    .setShouldReceiveVelocityUpdates(true)
                    .setUpdateInterval(1)
                    .build("tephra"));
    public static final DeferredHolder<EntityType<?>, EntityType<AtlatitanEntity>> ATLATITAN = DEF_REG.register(
            "atlatitan",
            () -> EntityType.Builder.of(AtlatitanEntity::new, MobCategory.CREATURE)
                    .sized(5.0F, 8.0F)
                    .setTrackingRange(11)
                    .build("atlatitan"));
    public static final DeferredHolder<EntityType<?>, EntityType<CrushedBlockEntity>> CRUSHED_BLOCK = DEF_REG.register(
            "crushed_block",
            () -> EntityType.Builder.<CrushedBlockEntity>of(CrushedBlockEntity::new, MobCategory.MISC)
                    .sized(0.99F, 0.99F)
                    .setUpdateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .updateInterval(10)
                    .clientTrackingRange(20)
                    .build("crushed_block"));
    public static final DeferredHolder<EntityType<?>, EntityType<NuclearExplosionEntity>> NUCLEAR_EXPLOSION =
            DEF_REG.register(
                    "nuclear_explosion",
                    () -> EntityType.Builder.<NuclearExplosionEntity>of(NuclearExplosionEntity::new, MobCategory.MISC)
                            .sized(0.99F, 0.99F)
                            .setUpdateInterval(1)
                            .setShouldReceiveVelocityUpdates(true)
                            .updateInterval(10)
                            .clientTrackingRange(20)
                            .build("nuclear_explosion"));
    public static final DeferredHolder<EntityType<?>, EntityType<NucleeperEntity>> NUCLEEPER = DEF_REG.register(
            "nucleeper",
            () -> EntityType.Builder.of(NucleeperEntity::new, MobCategory.MONSTER)
                    .sized(0.98F, 3.95F)
                    .build("nucleeper"));
    public static final DeferredHolder<EntityType<?>, EntityType<HullbreakerEntity>> HULLBREAKER = DEF_REG.register(
            "hullbreaker",
            () -> EntityType.Builder.of(HullbreakerEntity::new, MobCategory.UNDERGROUND_WATER_CREATURE)
                    .sized(4.65F, 4.5F)
                    .setShouldReceiveVelocityUpdates(true)
                    .clientTrackingRange(20)
                    .build("hullbreaker"));
    public static final DeferredHolder<EntityType<?>, EntityType<InkBombEntity>> INK_BOMB = DEF_REG.register(
            "ink_bomb",
            () -> EntityType.Builder.<InkBombEntity>of(InkBombEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .setShouldReceiveVelocityUpdates(true)
                    .setUpdateInterval(1)
                    .build("ink_bomb"));
    public static final DeferredHolder<EntityType<?>, EntityType<DeepOneMageEntity>> DEEP_ONE_MAGE = DEF_REG.register(
            "deep_one_mage",
            () -> EntityType.Builder.of(DeepOneMageEntity::new, MobCategory.MONSTER)
                    .sized(1.35F, 2.5F)
                    .setTrackingRange(12)
                    .setShouldReceiveVelocityUpdates(true)
                    .setUpdateInterval(1)
                    .build("deep_one_mage"));
    public static final DeferredHolder<EntityType<?>, EntityType<WaterBoltEntity>> WATER_BOLT = DEF_REG.register(
            "water_bolt",
            () -> EntityType.Builder.<WaterBoltEntity>of(WaterBoltEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .setShouldReceiveVelocityUpdates(true)
                    .setUpdateInterval(1)
                    .build("water_bolt"));
    public static final DeferredHolder<EntityType<?>, EntityType<WaveEntity>> WAVE = DEF_REG.register(
            "wave",
            () -> EntityType.Builder.<WaveEntity>of(WaveEntity::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .setShouldReceiveVelocityUpdates(true)
                    .setUpdateInterval(1)
                    .build("wave"));
    public static final DeferredHolder<EntityType<?>, EntityType<VesperEntity>> VESPER = DEF_REG.register(
            "vesper",
            () -> EntityType.Builder.of(VesperEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 1.65F)
                    .setTrackingRange(12)
                    .setShouldReceiveVelocityUpdates(true)
                    .setUpdateInterval(1)
                    .build("vesper"));
}
