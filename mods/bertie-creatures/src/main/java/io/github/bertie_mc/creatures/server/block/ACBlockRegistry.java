package io.github.bertie_mc.creatures.server.block;

import io.github.bertie_mc.creatures.BertieCreatures;
import io.github.bertie_mc.creatures.server.block.grower.ThornwoodGrower;
import io.github.bertie_mc.creatures.server.entity.ACEntityRegistry;
import io.github.bertie_mc.creatures.server.item.*;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ACBlockRegistry {
    public static final BlockBehaviour.Properties THORNWOOD_LOG_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0F)
            .sound(SoundType.WOOD)
            .instrument(NoteBlockInstrument.BASS);
    public static final BlockBehaviour.Properties THORNWOOD_PLANKS_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .instrument(NoteBlockInstrument.BASS);
    public static final WoodType THORNWOOD_WOOD_TYPE =
            WoodType.register(new WoodType("bertiecreatures:thornwood", BlockSetType.OAK));
    public static final DeferredRegister<Block> DEF_REG =
            DeferredRegister.create(BuiltInRegistries.BLOCK, BertieCreatures.MODID);
    public static final DeferredHolder<Block, Block> GROTTOCERATOPS_EGG = registerBlockAndItem(
            "grottoceratops_egg",
            () -> new DinosaurEggBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .randomTicks(),
                    () -> ACEntityRegistry.GROTTOCERATOPS.get(),
                    8,
                    10));
    public static final DeferredHolder<Block, Block> TREMORSAURUS_EGG = registerBlockAndItem(
            "tremorsaurus_egg",
            () -> new DinosaurEggBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .randomTicks(),
                    () -> ACEntityRegistry.TREMORSAURUS.get(),
                    10,
                    16));
    public static final DeferredHolder<Block, Block> ATLATITAN_EGG = registerBlockAndItem(
            "atlatitan_egg",
            () -> new DinosaurEggBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .randomTicks(),
                    () -> ACEntityRegistry.ATLATITAN.get(),
                    16,
                    16));
    public static final DeferredHolder<Block, Block> DINOSAUR_CHOP =
            registerBlockAndItem("dinosaur_chop", () -> new DinosaurChopBlock(3, 0.2F));
    public static final DeferredHolder<Block, Block> COOKED_DINOSAUR_CHOP =
            registerBlockAndItem("cooked_dinosaur_chop", () -> new DinosaurChopBlock(7, 0.35F));
    public static final DeferredHolder<Block, Block> PRIMAL_MAGMA =
            registerBlockAndItem("primal_magma", () -> new PrimalMagmaBlock());
    public static final DeferredHolder<Block, Block> FISSURE_PRIMAL_MAGMA =
            DEF_REG.register("fissure_primal_magma", () -> new FissurePrimalMagmaBlock());
    public static final DeferredHolder<Block, Block> THIN_BONE =
            registerBlockAndItem("thin_bone", () -> new ThinBoneBlock());
    public static final DeferredHolder<Block, Block> THORNWOOD_LOG =
            registerBlockAndItem("thornwood_log", () -> new StrippableLogBlock(THORNWOOD_LOG_PROPERTIES));
    public static final DeferredHolder<Block, Block> THORNWOOD_BRANCH =
            registerBlockAndItem("thornwood_branch", () -> new ThornwoodBranchBlock());
    public static final DeferredHolder<Block, Block> POTTED_THORNWOOD_BRANCH = DEF_REG.register(
            "potted_thornwood_branch",
            () -> new FlowerPotBlock(
                    () -> (FlowerPotBlock) Blocks.FLOWER_POT,
                    THORNWOOD_BRANCH,
                    BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredHolder<Block, Block> THORNWOOD_WOOD =
            registerBlockAndItem("thornwood_wood", () -> new StrippableLogBlock(THORNWOOD_LOG_PROPERTIES));
    public static final DeferredHolder<Block, Block> STRIPPED_THORNWOOD_LOG =
            registerBlockAndItem("stripped_thornwood_log", () -> new RotatedPillarBlock(THORNWOOD_LOG_PROPERTIES));
    public static final DeferredHolder<Block, Block> STRIPPED_THORNWOOD_WOOD =
            registerBlockAndItem("stripped_thornwood_wood", () -> new RotatedPillarBlock(THORNWOOD_LOG_PROPERTIES));
    public static final DeferredHolder<Block, Block> THORNWOOD_PLANKS =
            registerBlockAndItem("thornwood_planks", () -> new Block(THORNWOOD_PLANKS_PROPERTIES));
    public static final DeferredHolder<Block, Block> THORNWOOD_PLANKS_STAIRS = registerBlockAndItem(
            "thornwood_stairs",
            () -> new StairBlock(THORNWOOD_PLANKS.get().defaultBlockState(), THORNWOOD_PLANKS_PROPERTIES));
    public static final DeferredHolder<Block, Block> THORNWOOD_PLANKS_SLAB =
            registerBlockAndItem("thornwood_slab", () -> new SlabBlock(THORNWOOD_PLANKS_PROPERTIES));
    public static final DeferredHolder<Block, Block> THORNWOOD_PLANKS_FENCE =
            registerBlockAndItem("thornwood_fence", () -> new FenceBlock(THORNWOOD_PLANKS_PROPERTIES));
    public static final DeferredHolder<Block, Block> THORNWOOD_SIGN = DEF_REG.register(
            "thornwood_sign",
            () -> new StandingSignBlock(
                    THORNWOOD_WOOD_TYPE,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY)
                            .noCollission()
                            .strength(1.0F)
                            .sound(SoundType.WOOD)));
    public static final DeferredHolder<Block, Block> THORNWOOD_WALL_SIGN = DEF_REG.register(
            "thornwood_wall_sign",
            () -> new WallSignBlock(
                    THORNWOOD_WOOD_TYPE,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY)
                            .noCollission()
                            .strength(1.0F)
                            .sound(SoundType.WOOD)));
    public static final DeferredHolder<Block, Block> THORNWOOD_HANGING_SIGN = DEF_REG.register(
            "thornwood_hanging_sign",
            () -> new CeilingHangingSignBlock(
                    THORNWOOD_WOOD_TYPE,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .forceSolidOn()
                            .instrument(NoteBlockInstrument.BASS)
                            .noCollission()
                            .strength(1.0F)));
    public static final DeferredHolder<Block, Block> THORNWOOD_WALL_HANGING_SIGN = DEF_REG.register(
            "thornwood_wall_hanging_sign",
            () -> new WallHangingSignBlock(
                    THORNWOOD_WOOD_TYPE,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .forceSolidOn()
                            .instrument(NoteBlockInstrument.BASS)
                            .noCollission()
                            .strength(1.0F)
                            .dropsLike(THORNWOOD_HANGING_SIGN.get())));
    public static final DeferredHolder<Block, Block> THORNWOOD_PRESSURE_PLATE = registerBlockAndItem(
            "thornwood_pressure_plate",
            () -> new PressurePlateBlock(
                    BlockSetType.OAK,
                    BlockBehaviour.Properties.ofFullCopy(THORNWOOD_PLANKS.get())
                            .noCollission()
                            .strength(0.5F)
                            .sound(SoundType.WOOD)));
    public static final DeferredHolder<Block, Block> THORNWOOD_TRAPDOOR = registerBlockAndItem(
            "thornwood_trapdoor",
            () -> new TrapDoorBlock(
                    BlockSetType.OAK,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()));
    public static final DeferredHolder<Block, Block> THORNWOOD_BUTTON = registerBlockAndItem(
            "thornwood_button",
            () -> new ButtonBlock(
                    BlockSetType.OAK,
                    30,
                    BlockBehaviour.Properties.ofFullCopy(THORNWOOD_PLANKS.get())
                            .noCollission()
                            .strength(0.5F)
                            .sound(SoundType.WOOD)));
    public static final DeferredHolder<Block, Block> THORNWOOD_FENCE_GATE = registerBlockAndItem(
            "thornwood_fence_gate",
            () -> new FenceGateBlock(
                    BlockBehaviour.Properties.ofFullCopy(THORNWOOD_PLANKS.get())
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)
                            .forceSolidOn(),
                    SoundEvents.FENCE_GATE_CLOSE,
                    SoundEvents.FENCE_GATE_OPEN));
    public static final DeferredHolder<Block, Block> THORNWOOD_DOOR = DEF_REG.register(
            "thornwood_door",
            () -> new DoorBlock(
                    BlockSetType.OAK,
                    BlockBehaviour.Properties.ofFullCopy(THORNWOOD_PLANKS.get())
                            .strength(3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()));
    public static final DeferredHolder<Block, Block> THORNWOOD_SAPLING = registerBlockAndItem(
            "thornwood_sapling",
            () -> new CaveSaplingBlock(
                    ThornwoodGrower.GROWER,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.GRASS)
                            .noCollission()
                            .randomTicks()
                            .instabreak()
                            .sound(SoundType.GRASS),
                    true));
    public static final DeferredHolder<Block, Block> POTTED_THORNWOOD_SAPLING = DEF_REG.register(
            "potted_thornwood_sapling",
            () -> new FlowerPotBlock(
                    () -> (FlowerPotBlock) Blocks.FLOWER_POT,
                    THORNWOOD_SAPLING,
                    BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));

    private static DeferredHolder<Block, Block> registerBlockAndItem(String name, Supplier<Block> factory) {
        DeferredHolder<Block, Block> block = DEF_REG.register(name, factory);
        ACItemRegistry.DEF_REG.register(
                name, () -> new net.minecraft.world.item.BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredHolder<Block, Block> registerBlockOnly(String name, Supplier<Block> factory) {
        return DEF_REG.register(name, factory);
    }
}
