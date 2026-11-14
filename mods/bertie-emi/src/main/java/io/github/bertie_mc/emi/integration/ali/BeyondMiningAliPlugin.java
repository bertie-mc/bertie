package io.github.bertie_mc.emi.integration.ali;

import com.yanny.aci.api.RangeValue;
import com.yanny.aci.tooltip.TooltipBuilder;
import com.yanny.ali.api.AliEntrypoint;
import com.yanny.ali.api.ILootModifier;
import com.yanny.ali.api.IOperation;
import com.yanny.ali.api.IPlugin;
import com.yanny.ali.api.IServerRegistry;
import com.yanny.ali.plugin.common.nodes.ItemNode;
import java.util.List;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.neoforged.fml.ModList;

/** Pearl projectiles spawn from the block's break handler, outside its native loot table. */
@AliEntrypoint
public final class BeyondMiningAliPlugin implements IPlugin {
    @Override
    public String getModId() {
        return "bertieemi";
    }

    @Override
    public void registerServer(IServerRegistry registry) {
        if (!ModList.get().isLoaded("the_beyond")) return;
        registry.registerLootModifiers(utils -> {
            var source = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse("the_beyond:bedazzled_end_stone"));
            var pearl = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("the_beyond:pearl_bead"));
            if (source.isEmpty() || pearl.isEmpty()) return List.of();
            var silkTouch =
                    utils.lookupProvider().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
            var noSilkTouch = MatchTool.toolMatches(ItemPredicate.Builder.item()
                            .withSubPredicate(
                                    ItemSubPredicates.ENCHANTMENTS,
                                    ItemEnchantmentsPredicate.enchantments(List.of(
                                            new EnchantmentPredicate(silkTouch, MinMaxBounds.Ints.atLeast(1))))))
                    .invert()
                    .build();
            var drop = new ItemNode(
                    1,
                    new RangeValue(1, 4),
                    new ItemStack(pearl.get()),
                    TooltipBuilder.value("Pearl Beads appear loose; right-click them to collect.")
                            .build(),
                    List.of(),
                    List.of(noSilkTouch));
            return List.of(new MiningDrop(source.get(), List.of(new IOperation.AddOperation(stack -> true, drop))));
        });
    }

    private record MiningDrop(Block source, List<IOperation> operations) implements ILootModifier<Block> {
        @Override
        public boolean predicate(Block block) {
            return block == source;
        }

        @Override
        public List<IOperation> getOperations() {
            return operations;
        }

        @Override
        public IType<Block> getType() {
            return IType.BLOCK;
        }
    }
}
