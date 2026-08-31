package io.github.bertie_mc.bertieprogression;

import com.mojang.serialization.Codec;
import io.github.bertie_mc.bertieprogression.item.CraftingLicenseItem;
import io.github.bertie_mc.bertieprogression.item.DescentAnchorItem;
import io.github.bertie_mc.bertieprogression.item.FinderItem;
import io.github.bertie_mc.bertieprogression.item.NetherlyMealItem;
import io.github.bertie_mc.bertieprogression.item.WeepingEyeItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BertieProgression.MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BertieProgression.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BertieProgression.MODID);

    public static final Supplier<DataComponentType<GlobalPos>> RETURN_POS = DATA_COMPONENTS.register(
            "return_pos",
            () -> DataComponentType.<GlobalPos>builder()
                    .persistent(GlobalPos.CODEC)
                    .networkSynchronized(GlobalPos.STREAM_CODEC)
                    .build());

    /**
     * Set by {@link FinderItem#onCraftedBy} and consumed by its inventoryTick. It is the only signal
     * that separates a player craft from an autocrafted one - onCraftedBy never fires without a player.
     */
    public static final Supplier<DataComponentType<Boolean>> PLAYER_CRAFTED = DATA_COMPONENTS.register(
            "player_crafted",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    private static final List<DeferredItem<? extends Item>> ALL = new ArrayList<>();

    private static DeferredItem<Item> simple(String id, int stack) {
        DeferredItem<Item> it = ITEMS.registerSimpleItem(id, new Item.Properties().stacksTo(stack));
        ALL.add(it);
        return it;
    }

    private static DeferredItem<Item> rare(String id, int stack, Rarity rarity) {
        DeferredItem<Item> it = ITEMS.registerSimpleItem(
                id, new Item.Properties().stacksTo(stack).rarity(rarity));
        ALL.add(it);
        return it;
    }

    // --- Opening / foundry ---
    public static final DeferredItem<Item> OPENING_MALLET = ITEMS.registerSimpleItem(
            "opening_mallet", new Item.Properties().stacksTo(1).durability(256));
    public static final DeferredItem<Item> STONE_CRUCIBLE_BLANK = simple("stone_crucible_blank", 16);
    public static final DeferredItem<Item> STONE_POUR_CHANNEL = simple("stone_pour_channel", 16);

    // --- Create bridge ---
    public static final DeferredItem<Item> KINETIC_VANE = simple("kinetic_vane", 64);
    // Naga-trophy ritual output that grants access to the Twilight Lich.
    public static final DeferredItem<Item> SHIELD_MAIDEN = simple("shield_maiden", 16);
    // Lich-trophy ritual output that gates the progression following the Lich.
    public static final DeferredItem<Item> ACOLYTE_OF_DEFLECTION = simple("acolyte_of_deflection", 16);
    public static final DeferredItem<Item> KINETIC_PATTERN_PLATE = simple("kinetic_pattern_plate", 16);

    // --- Table license chain ---
    /** Consumable: permanently unlocks the 3x3 grid for the player who uses it. */
    public static final DeferredItem<CraftingLicenseItem> CRAFTING_LICENSE = ITEMS.register(
            "crafting_license",
            () -> new CraftingLicenseItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // --- Portals / dimension keys ---
    public static final DeferredItem<Item> TWILIGHT_CONCORD = simple("twilight_concord", 16);
    public static final DeferredItem<DescentAnchorItem> DESCENT_ANCHOR = ITEMS.register(
            "descent_anchor",
            () -> new DescentAnchorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // --- Avaritia ---
    public static final DeferredItem<Item> NULL_BLAZE_CUBE = simple("null_blaze_cube", 64);

    // --- Deep dark / echo chain ---
    public static final DeferredItem<Item> SPIRIT_FOCUSED_ECHO = simple("spirit_focused_echo", 16);
    public static final DeferredItem<WeepingEyeItem> WEEPING_EYE = ITEMS.register(
            "weeping_eye",
            () -> new WeepingEyeItem(
                    new Item.Properties().stacksTo(16),
                    ResourceLocation.parse("malum:weeping_well"),
                    FinderItem.STANDARD_RADIUS));

    /**
     * Netherly Meal: 8 nutrition, saturation 20. Vanilla stores saturation as a MODIFIER, and the
     * real value is nutrition * modifier * 2 - so 20 saturation off 8 nutrition needs 1.25f, not 20f.
     */
    public static final DeferredItem<NetherlyMealItem> NETHERLY_MEAL = ITEMS.register(
            "netherly_meal",
            () -> new NetherlyMealItem(new Item.Properties()
                    .stacksTo(16)
                    .food(new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationModifier(1.25F)
                            .build())));

    // --- Finders: craftable charts that resolve into real exploration maps. ---
    public static final DeferredItem<FinderItem> SIROK_NEST_MAP = ITEMS.register(
            "sirok_nest_map",
            () -> new FinderItem(
                    new Item.Properties().stacksTo(16),
                    ResourceLocation.parse("block_factorys_bosses:sandworm_nest"),
                    "item.bertieprogression.sirok_nest_map.filled",
                    FinderItem.STANDARD_RADIUS));
    public static final DeferredItem<FinderItem> KRAKEN_SHIP_MAP = ITEMS.register(
            "kraken_ship_map",
            () -> new FinderItem(
                    new Item.Properties().stacksTo(16),
                    ResourceLocation.parse("block_factorys_bosses:kraken_ship"),
                    "item.bertieprogression.kraken_ship_map.filled",
                    FinderItem.STANDARD_RADIUS));
    public static final DeferredItem<FinderItem> YETI_HIDEOUT_MAP = ITEMS.register(
            "yeti_hideout_map",
            () -> new FinderItem(
                    new Item.Properties().stacksTo(16),
                    ResourceLocation.parse("block_factorys_bosses:yeti_hideout"),
                    "item.bertieprogression.yeti_hideout_map.filled",
                    FinderItem.STANDARD_RADIUS));

    // --- Elemental cores produced by the four 7x7 mechanical-crafter recipes. ---
    public static final DeferredItem<Item> ABYSSAL_CORE = simple("abyssal_core", 16);
    public static final DeferredItem<Item> DESERT_CORE = simple("desert_core", 16);
    public static final DeferredItem<Item> CURSED_CORE = simple("cursed_core", 16);
    public static final DeferredItem<Item> STORM_CORE = simple("storm_core", 16);

    // --- Eezo: the bedrock-lookalike ore, its raw drop and the ingot it smelts into. ---
    public static final DeferredItem<BlockItem> EEZO_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.EEZO_ORE);
    public static final DeferredItem<Item> RAW_EEZO = simple("raw_eezo", 64);
    public static final DeferredItem<Item> EEZO_INGOT = simple("eezo_ingot", 64);

    public static final DeferredItem<Item> BOSS_REMATCH_SEAL = simple("boss_rematch_seal", 16);

    /** Dropped by villagers killed by a player - loot_modifiers/innocent_soul.json. */
    public static final DeferredItem<Item> INNOCENT_SOUL = simple("innocent_soul", 64);

    public static final Supplier<CreativeModeTab> MAIN_TAB = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bertieprogression"))
                    .icon(() -> new ItemStack(NETHERLY_MEAL.get()))
                    .displayItems((params, out) -> {
                        out.accept(OPENING_MALLET.get());
                        for (DeferredItem<? extends Item> it : ALL) out.accept(it.get());
                        out.accept(DESCENT_ANCHOR.get());
                        out.accept(WEEPING_EYE.get());
                        out.accept(NETHERLY_MEAL.get());
                        out.accept(SIROK_NEST_MAP.get());
                        out.accept(KRAKEN_SHIP_MAP.get());
                        out.accept(YETI_HIDEOUT_MAP.get());
                        out.accept(CRAFTING_LICENSE.get());
                        out.accept(EEZO_ORE.get());
                    })
                    .build());

    private ModItems() {}
}
