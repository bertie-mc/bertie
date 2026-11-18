package io.github.bertie_mc.bertieprogression;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Material-only hosting; the retired space mod and its dimensions remain absent. */
public final class RetainedMaterials {
    private record Specification(
            List<String> items,
            List<String> blocks,
            @SerializedName("item_aliases") Map<String, String> itemAliases,
            @SerializedName("block_aliases") Map<String, String> blockAliases) {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BertieProgression.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BertieProgression.MODID);
    private static final Map<String, Supplier<? extends Item>> MATERIAL_ITEMS = new LinkedHashMap<>();
    private static final Specification SPEC = load();

    private static Specification load() {
        try (var input = RetainedMaterials.class.getResourceAsStream("/retained_stellaris_materials.json")) {
            if (input == null) throw new IllegalStateException("Missing retained material definitions");
            return new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Specification.class);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot load retained material definitions", error);
        }
    }

    public static void register(IEventBus bus) {
        for (String name : SPEC.items()) MATERIAL_ITEMS.put(name, ITEMS.registerSimpleItem(name));
        for (String name : SPEC.blocks()) {
            boolean ore = name.endsWith("_ore");
            var block = BLOCKS.registerSimpleBlock(
                    name,
                    BlockBehaviour.Properties.of()
                            .mapColor(ore ? MapColor.STONE : MapColor.COLOR_ORANGE)
                            .strength(ore ? 3 : 5, ore ? 3 : 6)
                            .sound(ore ? SoundType.STONE : SoundType.METAL)
                            .requiresCorrectToolForDrops());
            MATERIAL_ITEMS.put(name, ITEMS.registerSimpleBlockItem(block));
        }
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(EventPriority.LOWEST, RetainedMaterials::aliases);
    }

    private static void aliases(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.ITEM))
            SPEC.itemAliases().forEach((oldId, newId) -> {
                var oldKey = ResourceLocation.parse(oldId);
                var newKey = ResourceLocation.parse(newId);
                if (!BuiltInRegistries.ITEM.containsKey(oldKey)) BuiltInRegistries.ITEM.addAlias(oldKey, newKey);
            });
        if (event.getRegistryKey().equals(Registries.BLOCK))
            SPEC.blockAliases().forEach((oldId, newId) -> {
                var oldKey = ResourceLocation.parse(oldId);
                var newKey = ResourceLocation.parse(newId);
                if (!BuiltInRegistries.BLOCK.containsKey(oldKey)) BuiltInRegistries.BLOCK.addAlias(oldKey, newKey);
            });
    }

    public static List<Item> creativeItems() {
        return MATERIAL_ITEMS.values().stream()
                .map(Supplier::get)
                .map(i -> (Item) i)
                .toList();
    }

    private RetainedMaterials() {}
}
