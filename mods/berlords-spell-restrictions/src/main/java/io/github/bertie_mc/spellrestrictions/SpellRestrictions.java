package io.github.bertie_mc.spellrestrictions;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(SpellRestrictions.ID)
public final class SpellRestrictions {
    public static final String ID = "berlordsspellrestrictions";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final Supplier<AttachmentType<ProgressData>> PROGRESS = ATTACHMENTS.register(
            "progress",
            () -> AttachmentType.builder(() -> new ProgressData())
                    .serialize(ProgressData.CODEC)
                    .copyOnDeath()
                    .build());
    public static final Supplier<Item> FORBIDDEN_FRAGMENT = ITEMS.registerSimpleItem("forbidden_knowledge_fragment");
    public static final Supplier<Item> DEPTH_FRAGMENT = ITEMS.registerSimpleItem("depth_knowledge_fragment");
    public static final Supplier<ResearchManuscriptItem> OCCULT_MANUSCRIPT =
            ITEMS.register("occult_manuscript", () -> new ResearchManuscriptItem(Restrictions.OCCULT));
    public static final Supplier<ResearchManuscriptItem> ABYSSAL_MANUSCRIPT =
            ITEMS.register("abyssal_manuscript", () -> new ResearchManuscriptItem(Restrictions.ABYSSAL));
    public static final Supplier<CraftingOrbItem> COMMON_ORB = orb("common", 0);
    public static final Supplier<CraftingOrbItem> UNCOMMON_ORB = orb("uncommon", 1);
    public static final Supplier<CraftingOrbItem> RARE_ORB = orb("rare", 2);
    public static final Supplier<CraftingOrbItem> EPIC_ORB = orb("epic", 3);
    public static final Supplier<CraftingOrbItem> LEGENDARY_ORB = orb("legendary", 4);

    static {
        TABS.register(
                "items",
                () -> CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup." + ID))
                        .icon(() -> RARE_ORB.get().getDefaultInstance())
                        .displayItems(
                                (parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                        .build());
    }

    public SpellRestrictions(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        ATTACHMENTS.register(bus);
        TABS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, RestrictionConfig.SPEC);
        bus.addListener(SpellRestrictions::registerPackets);
        NeoForge.EVENT_BUS.addListener(SpellRestrictions::onLogin);
        NeoForge.EVENT_BUS.addListener(SpellRestrictions::onRespawn);
        NeoForge.EVENT_BUS.addListener(SpellRestrictions::onDimension);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    private static Supplier<CraftingOrbItem> orb(String name, int tier) {
        return ITEMS.register(name + "_crafting_orb", () -> new CraftingOrbItem(tier));
    }

    private static void registerPackets(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(ProgressPacket.TYPE, ProgressPacket.CODEC, ProgressPacket::handle);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Restrictions.initialize(player);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Restrictions.initialize(player);
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ProgressPacket.send(player);
    }
}
