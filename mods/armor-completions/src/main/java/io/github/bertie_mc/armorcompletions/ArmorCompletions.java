package io.github.bertie_mc.armorcompletions;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ArmorCompletions.MOD_ID)
public final class ArmorCompletions {
    public static final String MOD_ID = "armorcompletions";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public record Piece(ArmorFamily family, ArmorItem.Type type) {}
    public static final Map<Piece, DeferredItem<Item>> PIECES = new LinkedHashMap<>();

    static {
        for (ArmorFamily family : ArmorFamily.values()) {
            for (ArmorItem.Type type : family.missingTypes()) {
                PIECES.put(new Piece(family, type), ITEMS.register(family.itemName(type), () -> CompletionItems.create(family, type)));
            }
        }
        TABS.register("armor_completions", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.armorcompletions"))
                .icon(() -> new ItemStack(PIECES.values().iterator().next().get()))
                .displayItems((parameters, output) -> {
                    for (ArmorFamily family : ArmorFamily.values()) {
                        output.accept(BuiltInRegistries.ITEM.get(family.helmet));
                        output.accept(BuiltInRegistries.ITEM.get(family.chestplate));
                        if (family.leggings != null) output.accept(BuiltInRegistries.ITEM.get(family.leggings));
                        for (ArmorItem.Type type : family.missingTypes()) output.accept(PIECES.get(new Piece(family, type)).get());
                    }
                }).build());
    }

    public ArmorCompletions(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }

    public static ArmorItem sourceArmor(ArmorFamily family) {
        Item item = BuiltInRegistries.ITEM.get(family.helmet);
        if (!(item instanceof ArmorItem armor)) throw new IllegalStateException("Source armor is not registered: " + family.helmet);
        return armor;
    }

    public static Item.Properties properties(ArmorFamily family, ArmorItem.Type type) {
        ArmorItem source = sourceArmor(family);
        int sourceDurability = source.getDefaultInstance().getMaxDamage();
        int multiplier = sourceDurability / source.getType().getDurability(1);
        Item.Properties properties = new Item.Properties().stacksTo(1);
        return multiplier > 0 ? properties.durability(type.getDurability(multiplier)) : properties;
    }
}
