package io.github.bertie_mc.creatures.server.misc;

import io.github.bertie_mc.creatures.BertieCreatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.Fluid;

public class ACTagRegistry {
    public static final TagKey<Block> UNMOVEABLE = registerBlockTag("unmovable");
    public static final TagKey<Block> DINOSAURS_SPAWNABLE_ON = registerBlockTag("dinosaurs_spawnable_on");
    public static final TagKey<Block> STOPS_DINOSAUR_EGGS = registerBlockTag("stops_dinosaur_eggs");
    public static final TagKey<Block> GROTTOCERATOPS_FOOD_BLOCKS = registerBlockTag("grottoceratops_food_blocks");
    public static final TagKey<Block> RELICHEIRUS_NIBBLES = registerBlockTag("relicheirus_nibbles");
    public static final TagKey<Block> RELICHEIRUS_KNOCKABLE_LOGS = registerBlockTag("relicheirus_knockable_logs");
    public static final TagKey<Block> COOKS_MEAT_BLOCKS = registerBlockTag("cooks_meat_blocks");
    public static final TagKey<Block> REGENERATES_AFTER_PRIMORDIAL_BOSS_FIGHT =
            registerBlockTag("regenerates_after_primordial_boss_fight");
    public static final TagKey<Block> LUXTRUCTOSAURUS_BREAKS = registerBlockTag("luxtructosaurus_breaks");
    public static final TagKey<Block> NUKE_PROOF = registerBlockTag("nuke_proof");
    public static final TagKey<Item> DEEP_ONE_BARTERS = registerItemTag("deep_one_barters");
    public static final TagKey<EntityType<?>> DINOSAURS = registerEntityTag("dinosaurs");
    public static final TagKey<EntityType<?>> RESISTS_TREMORSAURUS_ROAR =
            registerEntityTag("resists_tremorsaurus_roar");
    public static final TagKey<EntityType<?>> RESISTS_RADIATION = registerEntityTag("resists_radiation");
    public static final TagKey<EntityType<?>> GLOWING_ENTITIES = registerEntityTag("glowing_entities");
    public static final TagKey<EntityType<?>> RESISTS_BUBBLED = registerEntityTag("resists_bubbled");
    public static final TagKey<DamageType> DEEP_ONE_IGNORES = registerDamageTypeTag("deep_one_ignores");

    private static TagKey<Block> registerBlockTag(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, name));
    }

    private static TagKey<Item> registerItemTag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, name));
    }

    private static TagKey<EntityType<?>> registerEntityTag(String name) {
        return TagKey.create(
                Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, name));
    }

    private static TagKey<DamageType> registerDamageTypeTag(String name) {
        return TagKey.create(
                Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, name));
    }

    private static TagKey<Biome> registerBiomeTag(String name) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, name));
    }

    private static TagKey<Fluid> registerFluidTag(String name) {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, name));
    }

    private static TagKey<Structure> registerStructureTag(String name) {
        return TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, name));
    }
}
