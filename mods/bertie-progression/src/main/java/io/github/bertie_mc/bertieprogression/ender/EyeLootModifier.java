package io.github.bertie_mc.bertieprogression.ender;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class EyeLootModifier extends LootModifier {
    public static final MapCodec<EyeLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, EyeLootModifier::new));

    private record Reward(String eye, double chance) {}

    private static final Map<String, Reward> REWARDS = Map.ofEntries(
            Map.entry("minecraft:chests/trial_chambers/reward_ominous", new Reward("trial_eye", 0.01)),
            Map.entry("minecraft:chests/igloo_chest", new Reward("cold_eye", 0.10)),
            Map.entry("aquamirae:chests/frozen_chest", new Reward("cold_eye", 0.17)),
            Map.entry("minecraft:chests/village/village_snowy_house", new Reward("cold_eye", 0.005)),
            Map.entry("minecraft:chests/buried_treasure", new Reward("exotic_eye", 0.05)),
            Map.entry("minecraft:chests/abandoned_mineshaft", new Reward("lost_eye", 0.01)),
            Map.entry("minecraft:chests/village/village_weaponsmith", new Reward("lost_eye", 0.01)),
            Map.entry("minecraft:chests/village/village_toolsmith", new Reward("lost_eye", 0.01)),
            Map.entry("minecraft:chests/village/village_armorer", new Reward("lost_eye", 0.01)));

    public EyeLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generated, LootContext context) {
        var table = context.getQueriedLootTableId();
        Reward reward = table == null ? null : REWARDS.get(table.toString());
        if (reward != null && context.getRandom().nextDouble() < reward.chance()) {
            ItemStack result = EyeAcquisitions.eye(reward.eye());
            if (!result.isEmpty()) generated.add(result);
        }
        return generated;
    }

    @Override
    public MapCodec<? extends LootModifier> codec() {
        return CODEC;
    }
}
