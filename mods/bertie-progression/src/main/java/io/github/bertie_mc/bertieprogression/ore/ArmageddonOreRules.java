package io.github.bertie_mc.bertieprogression.ore;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Boss-gated ores require an actual player, including when loot is queried outside a break event. */
public final class ArmageddonOreRules {
    private static final Map<String, String> ADVANCEMENTS = Map.of(
            "bloody_ore", "the_blood_keeper",
            "voiderite_ore", "the_voiderite_keeper",
            "elvenite_ore", "the_elvenite_keeper");

    public static ResourceLocation requiredAdvancement(ResourceLocation block) {
        String advancement = block.getNamespace().equals("armageddon_mod") ? ADVANCEMENTS.get(block.getPath()) : null;
        return advancement == null ? null : ResourceLocation.fromNamespaceAndPath("armageddon_mod", advancement);
    }

    public static ResourceLocation blockId(BlockState state) {
        return state == null ? null : BuiltInRegistries.BLOCK.getKey(state.getBlock());
    }

    public static boolean allows(ResourceLocation block, Entity entity) {
        ResourceLocation required = block == null ? null : requiredAdvancement(block);
        if (required == null) return true;
        if (!(entity instanceof ServerPlayer player) || player instanceof FakePlayer) return false;
        var advancement = player.server.getAdvancements().get(required);
        return advancement != null
                && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public static boolean allows(BlockState state, Entity entity) {
        return allows(blockId(state), entity);
    }

    public static ResourceLocation blockFromLootTable(ResourceLocation table) {
        if (table == null || !table.getPath().startsWith("blocks/")) return null;
        ResourceLocation block = ResourceLocation.fromNamespaceAndPath(
                table.getNamespace(), table.getPath().substring(7));
        return requiredAdvancement(block) == null ? null : block;
    }

    private ArmageddonOreRules() {}
}
