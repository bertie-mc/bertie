package io.github.bertie_mc.creatures.client;

import io.github.bertie_mc.creatures.BertieCreatures;
import io.github.bertie_mc.creatures.client.particle.*;
import io.github.bertie_mc.creatures.client.render.entity.*;
import io.github.bertie_mc.creatures.client.sound.NucleeperSound;
import io.github.bertie_mc.creatures.server.CommonProxy;
import io.github.bertie_mc.creatures.server.block.ACBlockRegistry;
import io.github.bertie_mc.creatures.server.entity.ACEntityRegistry;
import io.github.bertie_mc.creatures.server.entity.living.NucleeperEntity;
import java.util.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid = BertieCreatures.MODID, value = Dist.CLIENT)
public class ClientProxy extends CommonProxy {
    public int renderNukeFlashFor;
    private float nukeFlashAmount;
    public static final KeyMapping SPECIAL = new KeyMapping(
            "key.bertiecreatures.special", org.lwjgl.glfw.GLFW.GLFW_KEY_V, "key.categories.bertiecreatures");
    private final Map<Integer, NucleeperSound> sounds = new HashMap<>();
    public final Set<UUID> blocked = new HashSet<>();

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        BertieCreatures.PROXY = new ClientProxy();
        event.enqueueWork(() -> {
            Sheets.addWoodType(ACBlockRegistry.THORNWOOD_WOOD_TYPE);
            for (var block : ACBlockRegistry.DEF_REG.getEntries()) {
                String path = block.getId().getPath();
                if (path.contains("sapling")
                        || path.contains("branch")
                        || path.endsWith("door")
                        || path.endsWith("egg")) ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout());
            }
        });
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        event.register(SPECIAL);
    }

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent event) throws java.io.IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                BertieCreatures.MODID, "rendertype_bubbled"),
                        com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY),
                shader -> io.github.bertie_mc.creatures.client.render.ACRenderTypes.bubbledShader = shader);
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (var type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
            if (event.getRenderer(type) instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer renderer)
                renderer.addLayer(
                        new io.github.bertie_mc.creatures.client.render.entity.layer.ACPotionEffectLayer(renderer));
        }
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer renderer)
                renderer.addLayer(
                        new io.github.bertie_mc.creatures.client.render.entity.layer.ACPotionEffectLayer(renderer));
        }
    }

    @SubscribeEvent
    public static void afterLevel(RenderLevelStageEvent event) {
        var client = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL
                && client.options.getCameraType().isFirstPerson()
                && client.getCameraEntity() instanceof net.minecraft.world.entity.LivingEntity living
                && living.hasEffect(io.github.bertie_mc.creatures.server.potion.ACEffectRegistry.BUBBLED)) {
            io.github.bertie_mc.creatures.client.render.entity.layer.ACPotionEffectLayer.renderBubbledFirstPerson(
                    new com.mojang.blaze3d.vertex.PoseStack());
        }
    }

    @SubscribeEvent
    public static void clientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (BertieCreatures.PROXY instanceof ClientProxy proxy
                && !Minecraft.getInstance().isPaused()) {
            if (proxy.renderNukeFlashFor > 0) {
                proxy.renderNukeFlashFor--;
                proxy.nukeFlashAmount = Math.min(1, proxy.nukeFlashAmount + 0.4F);
            } else proxy.nukeFlashAmount = Math.max(0, proxy.nukeFlashAmount - 0.05F);
        }
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Post event) {
        if (BertieCreatures.PROXY instanceof ClientProxy proxy
                && proxy.nukeFlashAmount > 0
                && BertieCreatures.CLIENT_CONFIG.nuclearBombFlash.get()) {
            var gui = event.getGuiGraphics();
            gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), ((int) (proxy.nukeFlashAmount * 255) << 24) | 0xffffff);
        }
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ACEntityRegistry.DEEP_ONE_MAGE.get(), DeepOneMageRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.HULLBREAKER.get(), HullbreakerRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.VESPER.get(), VesperRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.NUCLEEPER.get(), NucleeperRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.LUXTRUCTOSAURUS.get(), LuxtructosaurusRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.TREMORSAURUS.get(), TremorsaurusRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.GROTTOCERATOPS.get(), GrottoceratopsRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.ATLATITAN.get(), AtlatitanRenderer::new);
        event.registerEntityRenderer(
                ACEntityRegistry.BOAT.get(), context -> new AlexsCavesBoatRenderer<>(context, false));
        event.registerEntityRenderer(
                ACEntityRegistry.CHEST_BOAT.get(), context -> new AlexsCavesBoatRenderer<>(context, true));
        event.registerEntityRenderer(ACEntityRegistry.TEPHRA.get(), TephraRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.WATER_BOLT.get(), WaterBoltRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.WAVE.get(), WaveRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.CRUSHED_BLOCK.get(), CrushedBlockRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.INK_BOMB.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ACEntityRegistry.NUCLEAR_EXPLOSION.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ACParticleRegistry.WATER_TREMOR.get(), WaterTremorParticle.Factory::new);
        event.registerSpecial(
                ACParticleRegistry.DINOSAUR_TRANSFORMATION_AMBER.get(), new DinosaurTransformParticle.AmberFactory());
        event.registerSpecial(
                ACParticleRegistry.DINOSAUR_TRANSFORMATION_TECTONIC.get(),
                new DinosaurTransformParticle.TectonicFactory());
        event.registerSpecial(ACParticleRegistry.STUN_STAR.get(), new StunStarParticle.Factory());
        event.registerSpriteSet(ACParticleRegistry.TEPHRA.get(), TephraParticle.Factory::new);
        event.registerSpriteSet(ACParticleRegistry.TEPHRA_SMALL.get(), TephraParticle.SmallFactory::new);
        event.registerSpriteSet(ACParticleRegistry.TEPHRA_FLAME.get(), TephraParticle.FlameFactory::new);
        event.registerSpriteSet(
                ACParticleRegistry.LUXTRUCTOSAURUS_SPIT.get(), LuxtructosaurusSpitParticle.Factory::new);
        event.registerSpriteSet(ACParticleRegistry.LUXTRUCTOSAURUS_ASH.get(), LuxtructosaurusAshParticle.Factory::new);
        event.registerSpriteSet(ACParticleRegistry.HAPPINESS.get(), HappinessParticle.Factory::new);
        event.registerSpriteSet(ACParticleRegistry.RED_VENT_SMOKE.get(), VentSmokeParticle.RedFactory::new);
        event.registerSpecial(ACParticleRegistry.MUSHROOM_CLOUD.get(), new MushroomCloudParticle.Factory());
        event.registerSpriteSet(ACParticleRegistry.MUSHROOM_CLOUD_SMOKE.get(), SmallExplosionParticle.NukeFactory::new);
        event.registerSpriteSet(
                ACParticleRegistry.MUSHROOM_CLOUD_EXPLOSION.get(), SmallExplosionParticle.NukeFactory::new);
        event.registerSpriteSet(ACParticleRegistry.DEEP_ONE_MAGIC.get(), DeepOneMagicParticle.Factory::new);
        event.registerSpriteSet(ACParticleRegistry.WATER_FOAM.get(), WaterFoamParticle.Factory::new);
        event.registerSpecial(ACParticleRegistry.BIG_SPLASH.get(), new BigSplashParticle.Factory());
        event.registerSpriteSet(ACParticleRegistry.BIG_SPLASH_EFFECT.get(), BigSplashEffectParticle.Factory::new);
    }

    @Override
    public Player getClientSidePlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public float getPartialTicks() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    @Override
    public boolean isKeyDown(int key) {
        return key == 2
                ? SPECIAL.isDown()
                : key == 3 && Minecraft.getInstance().options.keyAttack.isDown();
    }

    @Override
    public boolean isFirstPersonPlayer(Entity entity) {
        return entity == Minecraft.getInstance().player
                && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    @Override
    public void playWorldSound(Object emitter, byte type) {
        if (type == 1 && emitter instanceof NucleeperEntity entity) {
            var previous = sounds.get(entity.getId());
            if (previous == null || previous.isStopped()) {
                var sound = new NucleeperSound(entity);
                sounds.put(entity.getId(), sound);
                Minecraft.getInstance().getSoundManager().play(sound);
            }
        }
    }

    @SubscribeEvent
    public static void beforeLivingRender(RenderLivingEvent.Pre<?, ?> event) {
        if (BertieCreatures.PROXY instanceof ClientProxy proxy
                && proxy.blocked.remove(event.getEntity().getUUID())
                && !proxy.isFirstPersonPlayer(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (BertieCreatures.PROXY instanceof ClientProxy proxy) {
            for (var sound : proxy.sounds.values())
                Minecraft.getInstance().getSoundManager().stop(sound);
            proxy.sounds.clear();
            proxy.blocked.clear();
            proxy.renderNukeFlashFor = 0;
            proxy.nukeFlashAmount = 0;
        }
    }

    @Override
    public void clearSoundCacheFor(Entity entity) {
        var sound = sounds.remove(entity.getId());
        if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound);
    }

    @Override
    public void blockRenderingEntity(UUID id) {
        blocked.add(id);
    }

    @Override
    public void releaseRenderingEntity(UUID id) {
        blocked.remove(id);
    }
}
