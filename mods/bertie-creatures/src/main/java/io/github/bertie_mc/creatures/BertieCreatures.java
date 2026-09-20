package io.github.bertie_mc.creatures;

import com.mojang.logging.LogUtils;
import io.github.bertie_mc.creatures.client.particle.ACParticleRegistry;
import io.github.bertie_mc.creatures.server.CommonProxy;
import io.github.bertie_mc.creatures.server.block.ACBlockRegistry;
import io.github.bertie_mc.creatures.server.entity.ACEntityRegistry;
import io.github.bertie_mc.creatures.server.entity.living.*;
import io.github.bertie_mc.creatures.server.item.ACItemRegistry;
import io.github.bertie_mc.creatures.server.level.feature.ThornwoodTreeFeature;
import io.github.bertie_mc.creatures.server.message.*;
import io.github.bertie_mc.creatures.server.misc.ACSoundRegistry;
import io.github.bertie_mc.creatures.server.potion.ACEffectRegistry;
import java.util.HashSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(BertieCreatures.MODID)
public class BertieCreatures {
    public static final String MODID = "bertiecreatures";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static CommonProxy PROXY = new CommonProxy();
    public static final CreatureConfig COMMON_CONFIG = new CreatureConfig();
    public static final CreatureConfig CLIENT_CONFIG = COMMON_CONFIG;
    public static final TicketController TICKET_CONTROLLER =
            new TicketController(ResourceLocation.fromNamespaceAndPath(MODID, "explosions"));
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public BertieCreatures(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG.SPEC);
        ACBlockRegistry.DEF_REG.register(bus);
        ACEntityRegistry.DEF_REG.register(bus);
        ACEntityRegistry.DEF_REG.getEntries().forEach(entry -> {
            String name = entry.getId().getPath();
            if (java.util.Set.of(
                            "deep_one_mage",
                            "hullbreaker",
                            "vesper",
                            "nucleeper",
                            "luxtructosaurus",
                            "tremorsaurus",
                            "grottoceratops",
                            "atlatitan")
                    .contains(name)) {
                ACItemRegistry.DEF_REG.register(
                        name + "_spawn_egg",
                        () -> new DeferredSpawnEggItem(
                                () -> (net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>)
                                        entry.get(),
                                0x37464a,
                                0xd5af63,
                                new Item.Properties()));
            }
        });
        ACItemRegistry.DEF_REG.register(bus);
        ACEffectRegistry.DEF_REG.register(bus);
        ACSoundRegistry.DEF_REG.register(bus);
        ACParticleRegistry.DEF_REG.register(bus);
        FEATURES.register("thornwood_tree", () -> new ThornwoodTreeFeature(NoneFeatureConfiguration.CODEC));
        FEATURES.register(bus);
        TABS.register(
                "creatures",
                () -> CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.bertiecreatures"))
                        .icon(() -> new ItemStack(ACItemRegistry.TECTONIC_SHARD.get()))
                        .displayItems((parameters, output) ->
                                ACItemRegistry.DEF_REG.getEntries().forEach(e -> output.accept(e.get())))
                        .build());
        TABS.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::payloads);
        bus.addListener(this::commonSetup);
        bus.addListener((RegisterTicketControllersEvent event) -> event.register(TICKET_CONTROLLER));
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(
                ACEntityRegistry.DEEP_ONE_MAGE.get(),
                DeepOneMageEntity.createAttributes().build());
        event.put(
                ACEntityRegistry.HULLBREAKER.get(),
                HullbreakerEntity.createAttributes().build());
        event.put(ACEntityRegistry.VESPER.get(), VesperEntity.createAttributes().build());
        event.put(
                ACEntityRegistry.NUCLEEPER.get(),
                NucleeperEntity.createAttributes().build());
        event.put(
                ACEntityRegistry.LUXTRUCTOSAURUS.get(),
                LuxtructosaurusEntity.createAttributes().build());
        event.put(
                ACEntityRegistry.TREMORSAURUS.get(),
                TremorsaurusEntity.createAttributes().build());
        event.put(
                ACEntityRegistry.GROTTOCERATOPS.get(),
                GrottoceratopsEntity.createAttributes().build());
        event.put(
                ACEntityRegistry.ATLATITAN.get(),
                AtlatitanEntity.createAttributes().build());
    }

    private void payloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(
                MountedEntityKeyMessage.ID, MountedEntityKeyMessage.CODEC, MountedEntityKeyMessage::handleServer);
        registrar.playToClient(
                UpdateEffectVisualityEntityMessage.ID,
                UpdateEffectVisualityEntityMessage.CODEC,
                UpdateEffectVisualityEntityMessage::handle);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var signs = new HashSet<>(BlockEntityType.SIGN.validBlocks);
            signs.add(ACBlockRegistry.THORNWOOD_SIGN.get());
            signs.add(ACBlockRegistry.THORNWOOD_WALL_SIGN.get());
            BlockEntityType.SIGN.validBlocks = signs;
            var hanging = new HashSet<>(BlockEntityType.HANGING_SIGN.validBlocks);
            hanging.add(ACBlockRegistry.THORNWOOD_HANGING_SIGN.get());
            hanging.add(ACBlockRegistry.THORNWOOD_WALL_HANGING_SIGN.get());
            BlockEntityType.HANGING_SIGN.validBlocks = hanging;
            ((FlowerPotBlock) Blocks.FLOWER_POT)
                    .addPlant(ACBlockRegistry.THORNWOOD_SAPLING.getId(), ACBlockRegistry.POTTED_THORNWOOD_SAPLING);
            ((FlowerPotBlock) Blocks.FLOWER_POT)
                    .addPlant(ACBlockRegistry.THORNWOOD_BRANCH.getId(), ACBlockRegistry.POTTED_THORNWOOD_BRANCH);
        });
    }

    public static void sendMSGToAll(CustomPacketPayload message) {
        PacketDistributor.sendToAllPlayers(message);
    }

    public static void sendMSGToServer(CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }
}
