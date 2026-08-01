package com.berlord.foodsystem;

import com.berlord.foodsystem.buffs.BuffsConfig;
import com.berlord.foodsystem.buffs.FoodBuffsSyncPayload;
import com.berlord.foodsystem.item.EmeticItem;
import com.berlord.foodsystem.item.EternalSeasoningRecipe;
import com.berlord.foodsystem.item.LunchboxItem;
import com.berlord.foodsystem.item.NoRemainderShapedRecipe;
import com.berlord.foodsystem.item.StomachExtensionItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import com.berlord.foodsystem.stomach.Stomach;
import com.berlord.foodsystem.stomach.StomachData;
import com.berlord.foodsystem.stomach.StomachSyncPayload;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

/**
 * Berlord's Food System — standalone Valheim-style food system.
 * Based on Spice of Life: Valheim Reforged by robinfrt (used with permission).
 */
@Mod(BFS.MODID)
public class BFS {
    public static final String MODID = "berlords_food_system";

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);
    public static final Supplier<AttachmentType<StomachData>> STOMACH =
            ATTACHMENTS.register("stomach", () -> AttachmentType.serializable(StomachData::new).build());

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<Item> EMETIC =
            ITEMS.registerItem("emetic", p -> new EmeticItem(p, false, "item.berlords_food_system.emetic.tooltip"),
                    new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> DEMONIC_GRUEL =
            ITEMS.registerItem("demonic_gruel", p -> new EmeticItem(p, true, "item.berlords_food_system.demonic_gruel.tooltip"),
                    new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> STOMACH_EXTENSION =
            ITEMS.registerItem("stomach_extension", StomachExtensionItem::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> ETERNAL_SEASONING =
            ITEMS.registerItem("eternal_seasoning", Item::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> LUNCHBOX =
            ITEMS.registerItem("lunchbox", LunchboxItem::new, new Item.Properties().stacksTo(1));

    /** marker component: this food never leaves the stomach once eaten */
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final Supplier<DataComponentType<Unit>> ETERNAL = COMPONENTS.register("eternal",
            () -> DataComponentType.<Unit>builder()
                    .persistent(Codec.unit(Unit.INSTANCE))
                    .networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
                    .build());
    /** foods stored inside a Lunchbox */
    public static final Supplier<DataComponentType<List<ItemStack>>> LUNCHBOX_CONTENTS =
            COMPONENTS.register("lunchbox_contents", () -> DataComponentType.<List<ItemStack>>builder()
                    .persistent(ItemStack.CODEC.listOf())
                    .networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final Supplier<RecipeSerializer<EternalSeasoningRecipe>> ETERNAL_SEASONING_SERIALIZER =
            RECIPE_SERIALIZERS.register("eternal_seasoning",
                    () -> new SimpleCraftingRecipeSerializer<>(EternalSeasoningRecipe::new));
    public static final Supplier<RecipeSerializer<NoRemainderShapedRecipe>> SHAPED_NO_REMAINDER_SERIALIZER =
            RECIPE_SERIALIZERS.register("shaped_no_remainder", NoRemainderShapedRecipe.Serializer::new);

    public BFS(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);

        ATTACHMENTS.register(modEventBus);
        ITEMS.register(modEventBus);
        COMPONENTS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::buildCreativeTabs);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> BuffsConfig.loadFromDisk(FMLPaths.CONFIGDIR.get()));
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(StomachSyncPayload.TYPE, StomachSyncPayload.STREAM_CODEC, StomachSyncPayload::handle);
        registrar.playToClient(FoodBuffsSyncPayload.TYPE, FoodBuffsSyncPayload.STREAM_CODEC, FoodBuffsSyncPayload::handle);
    }

    private void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(EMETIC);
            event.accept(DEMONIC_GRUEL);
            event.accept(STOMACH_EXTENSION);
            event.accept(ETERNAL_SEASONING);
            event.accept(LUNCHBOX);
        }
        // the Advanced Feeding Upgrade is disabled under the stomach system
        Item advanced = com.berlord.foodsystem.compat.SBCompat.advancedFeedingUpgrade();
        if (advanced != null) {
            event.remove(new net.minecraft.world.item.ItemStack(advanced),
                    net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    @EventBusSubscriber(modid = MODID)
    public static final class GameEvents {

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("bfs")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("reload").executes(ctx -> {
                        BuffsConfig.loadFromDisk(FMLPaths.CONFIGDIR.get());
                        String json = BuffsConfig.get().rawJson;
                        for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                            PacketDistributor.sendToPlayer(player, new FoodBuffsSyncPayload(json));
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "[bfs] reloaded: " + BuffsConfig.get().foods.size() + " foods configured"), true);
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.literal("dumpids").executes(ctx -> {
                        java.nio.file.Path file = FMLPaths.CONFIGDIR.get().resolve("berlords-food-system-ids.txt");
                        StringBuilder sb = new StringBuilder();
                        sb.append("All ids usable in ").append(com.berlord.foodsystem.buffs.BuffsConfig.FILE_NAME)
                                .append(" (this instance, including modded)\n\n== EFFECTS (\"effects\": [{\"id\": ..., \"amplifier\": 0}]) ==\n");
                        BuiltInRegistries.MOB_EFFECT.keySet().stream().map(Object::toString).sorted()
                                .forEach(id -> sb.append(id).append('\n'));
                        sb.append("\n== ATTRIBUTES (\"attributes\": [{\"id\": ..., \"amount\": 0.25, \"operation\": \"add_value|add_multiplied_base|add_multiplied_total\"}]) ==\n");
                        BuiltInRegistries.ATTRIBUTE.keySet().stream().map(Object::toString).sorted()
                                .forEach(id -> sb.append(id).append('\n'));
                        sb.append("\n== ABILITIES (\"abilities\": {...}) ==\n")
                                .append("flight: true|false        - creative-style flight\n")
                                .append("climbing: true|false      - spider wall-climb\n")
                                .append("enderman_calm: true|false - endermen don't aggro from your gaze\n")
                                .append("magnet: <radius blocks>   - pulls items & xp orbs\n")
                                .append("xp_boost: <multiplier>    - 1.0 = off\n")
                                .append("durability_saver: <0..1>  - chance held tools take no durability damage\n");
                        try {
                            java.nio.file.Files.writeString(file, sb.toString());
                            int effects = BuiltInRegistries.MOB_EFFECT.keySet().size();
                            int attributes = BuiltInRegistries.ATTRIBUTE.keySet().size();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "[bfs] wrote " + effects + " effects + " + attributes + " attributes to " + file), false);
                        } catch (java.io.IOException e) {
                            ctx.getSource().sendFailure(Component.literal("[bfs] failed to write " + file + ": " + e.getMessage()));
                        }
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.literal("setslots")
                            .then(Commands.argument("count", IntegerArgumentType.integer(1, StomachData.MAX_SLOTS))
                                    .executes(ctx -> {
                                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                                        Stomach.get(player).unlockedSlots = IntegerArgumentType.getInteger(ctx, "count");
                                        Stomach.sync(player);
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "[bfs] stomach slots: " + Stomach.unlockedSlots(player)
                                                        + " (clamped to config bounds)"), true);
                                        return Command.SINGLE_SUCCESS;
                                    }))));
        }

        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, new FoodBuffsSyncPayload(BuffsConfig.get().rawJson));
            }
        }
    }
}
