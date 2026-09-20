package io.github.bertie_mc.betterhorses;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;

@Mod(BetterHorses.ID)
public final class BetterHorses {
    public static final String ID = "betterhorses";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredItem<HorseEffigyItem> EFFIGY =
            ITEMS.register("horse_effigy", () -> new HorseEffigyItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<HorseshoeItem> IRON = shoe("iron", ShoeTier.IRON);
    public static final DeferredItem<HorseshoeItem> GOLD = shoe("gold", ShoeTier.GOLD);
    public static final DeferredItem<HorseshoeItem> DIAMOND = shoe("diamond", ShoeTier.DIAMOND);
    public static final DeferredItem<HorseshoeItem> NETHERITE = shoe("netherite", ShoeTier.NETHERITE);
    public static final DeferredItem<SpecialSaddleItem> PASSENGER = saddle("passenger");
    public static final DeferredItem<SpecialSaddleItem> WARRIOR = saddle("warrior");
    public static final DeferredItem<SpecialSaddleItem> WANDERER = saddle("wanderer");
    public static final DeferredItem<HorseAppleItem> ZOMBIE_APPLE =
            ITEMS.register("zombie_apple", () -> new HorseAppleItem(HorseAppleItem.Effect.ZOMBIE));
    public static final DeferredItem<HorseAppleItem> SKELETON_APPLE =
            ITEMS.register("skeleton_apple", () -> new HorseAppleItem(HorseAppleItem.Effect.SKELETON));
    public static final DeferredItem<HorseAppleItem> BREED_APPLE =
            ITEMS.register("breed_apple", () -> new HorseAppleItem(HorseAppleItem.Effect.BREED));
    public static final DeferredItem<AnimalArmorItem> ARMOR = ITEMS.register(
            "netherite_horse_armor",
            () ->
                    new AnimalArmorItem(
                            ArmorMaterials.NETHERITE,
                            AnimalArmorItem.BodyType.EQUESTRIAN,
                            false,
                            new Item.Properties().stacksTo(1).fireResistant()) {
                        @Override
                        public ResourceLocation getTexture() {
                            return id("textures/entity/horse_armor_netherite.png");
                        }
                    });
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredHolder<MenuType<?>, MenuType<BetterHorseMenu>> HORSE_MENU =
            MENUS.register("horse", () -> IMenuTypeExtension.create(BetterHorseMenu::fromNetwork));
    public static final ModConfigSpec CONFIG;
    public static final ModConfigSpec.DoubleValue DAMAGE_TRANSFER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        DAMAGE_TRANSFER = builder.comment(
                        "Fraction of damage after horse armor redirected to the controlling rider; the rider's defenses then apply.")
                .defineInRange("warriorDamageTransfer", 0.8, 0.0, 1.0);
        CONFIG = builder.build();
    }

    public BetterHorses(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        MENUS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, CONFIG);
        NeoForge.EVENT_BUS.addListener(HorseEvents::interact);
        NeoForge.EVENT_BUS.addListener(HorseEvents::incomingDamage);
        NeoForge.EVENT_BUS.addListener(HorseEvents::transferDamage);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES))
                ITEMS.getEntries().forEach(item -> event.accept(item.get()));
        });
    }

    private static DeferredItem<HorseshoeItem> shoe(String name, ShoeTier tier) {
        return ITEMS.register(name + "_horseshoes", () -> new HorseshoeItem(tier));
    }

    private static DeferredItem<SpecialSaddleItem> saddle(String name) {
        return ITEMS.register(name + "_saddle", () -> new SpecialSaddleItem(name));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}
