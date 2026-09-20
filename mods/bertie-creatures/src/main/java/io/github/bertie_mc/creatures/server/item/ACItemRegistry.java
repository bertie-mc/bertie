package io.github.bertie_mc.creatures.server.item;

import io.github.bertie_mc.creatures.BertieCreatures;
import io.github.bertie_mc.creatures.server.block.ACBlockRegistry;
import io.github.bertie_mc.creatures.server.entity.item.*;
import io.github.bertie_mc.creatures.server.entity.util.AlexsCavesBoat;
import java.awt.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ACItemRegistry {
    public static final Rarity RARITY_DEMONIC = Rarity.UNCOMMON;
    public static final DeferredRegister<Item> DEF_REG =
            DeferredRegister.create(Registries.ITEM, BertieCreatures.MODID);
    public static final DeferredHolder<Item, Item> DINOSAUR_NUGGET =
            DEF_REG.register("dinosaur_nugget", () -> new Item(new Item.Properties().food(ACFoods.DINOSAUR_NUGGETS)));
    public static final DeferredHolder<Item, Item> SEETHING_STEW = DEF_REG.register(
            "seething_stew",
            () -> new PrehistoricMixtureItem(new Item.Properties().stacksTo(1).food(ACFoods.SEETHING_STEW)));
    public static final DeferredHolder<Item, Item> TOUGH_HIDE =
            DEF_REG.register("tough_hide", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> HEAVY_BONE =
            DEF_REG.register("heavy_bone", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> TECTONIC_SHARD = DEF_REG.register(
            "tectonic_shard",
            () -> new Item(new Item.Properties().rarity(RARITY_DEMONIC).fireResistant()));
    public static final DeferredHolder<Item, Item> FISSILE_CORE = DEF_REG.register(
            "fissile_core", () -> new RadioactiveItem(new Item.Properties().rarity(Rarity.UNCOMMON), 0.001F));
    public static final DeferredHolder<Item, Item> SEA_GLASS_SHARDS =
            DEF_REG.register("sea_glass_shards", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> DARK_TATTERS =
            DEF_REG.register("dark_tatters", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> VESPER_WING =
            DEF_REG.register("vesper_wing", () -> new Item(new Item.Properties().food(ACFoods.VESPER_WING)));
    public static final DeferredHolder<Item, Item> VESPER_STEW = DEF_REG.register(
            "vesper_stew",
            () -> new Item(
                    new Item.Properties().food(ACFoods.VESPER_SOUP).stacksTo(1).craftRemainder(Items.BOWL)));
    public static final DeferredHolder<Item, Item> THORNWOOD_DOOR = DEF_REG.register(
            "thornwood_door",
            () -> new DoubleHighBlockItem(ACBlockRegistry.THORNWOOD_DOOR.get(), (new Item.Properties())));
    public static final DeferredHolder<Item, Item> THORNWOOD_SIGN = DEF_REG.register(
            "thornwood_sign",
            () -> new SignItem(
                    (new Item.Properties()).stacksTo(16),
                    ACBlockRegistry.THORNWOOD_SIGN.get(),
                    ACBlockRegistry.THORNWOOD_WALL_SIGN.get()));
    public static final DeferredHolder<Item, Item> THORNWOOD_HANGING_SIGN = DEF_REG.register(
            "thornwood_hanging_sign",
            () -> new HangingSignItem(
                    ACBlockRegistry.THORNWOOD_HANGING_SIGN.get(),
                    ACBlockRegistry.THORNWOOD_WALL_HANGING_SIGN.get(),
                    (new Item.Properties()).stacksTo(16)));
    public static final DeferredHolder<Item, Item> THORNWOOD_BOAT = DEF_REG.register(
            "thornwood_boat",
            () -> new CaveBoatItem(false, AlexsCavesBoat.Type.THORNWOOD, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> THORNWOOD_CHEST_BOAT = DEF_REG.register(
            "thornwood_chest_boat",
            () -> new CaveBoatItem(true, AlexsCavesBoat.Type.THORNWOOD, new Item.Properties().stacksTo(1)));
}
