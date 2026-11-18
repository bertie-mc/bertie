package io.github.bertie_mc.bertieprogression.cybernetics;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MechanicalAdapterRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BertieProgression.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BertieProgression.MODID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BertieProgression.MODID);
    public static final DeferredBlock<MechanicalAdapterBlock> BLOCK = BLOCKS.register(
            "mechanical_adapter",
            () -> new MechanicalAdapterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)
                    .noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    public static final DeferredBlock<CopperCoreBlock> COPPER_CORE = BLOCKS.register(
            "assembled_copper_core",
            () -> new CopperCoreBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CopperCoreBlock.CoreEntity>>
            COPPER_CORE_ENTITY = ENTITIES.register(
                    "assembled_copper_core",
                    () -> BlockEntityType.Builder.of(CopperCoreBlock.CoreEntity::new, COPPER_CORE.get())
                            .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalAdapterBlockEntity>> ENTITY =
            ENTITIES.register(
                    "mechanical_adapter",
                    () -> BlockEntityType.Builder.of(MechanicalAdapterBlockEntity::new, BLOCK.get())
                            .build(null));

    public static void register(IEventBus bus) {
        ITEMS.register("mechanical_adapter", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        NeoForge.EVENT_BUS.register(MechanicalAdapterFormation.class);
    }

    private MechanicalAdapterRegistry() {}
}
