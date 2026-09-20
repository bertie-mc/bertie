package io.github.bertie_mc.creatures.clienttest;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import io.github.bertie_mc.creatures.BertieCreatures;
import io.github.bertie_mc.creatures.server.entity.ACEntityRegistry;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CreatureClientTests {
    private CreatureClientTests() {}

    @ClientTest
    public static void standaloneModelsAnimationsAndItems(ClientTestContext context) {
        if (ModList.get().isLoaded("alexscaves")) throw new AssertionError("Alex's Caves must be absent");
        List<EntityType<?>> types = List.of(
                ACEntityRegistry.DEEP_ONE_MAGE.get(), ACEntityRegistry.HULLBREAKER.get(),
                ACEntityRegistry.VESPER.get(), ACEntityRegistry.NUCLEEPER.get(),
                ACEntityRegistry.LUXTRUCTOSAURUS.get(), ACEntityRegistry.TREMORSAURUS.get(),
                ACEntityRegistry.GROTTOCERATOPS.get(), ACEntityRegistry.ATLATITAN.get());
        try (var world = context.worldBuilder().create()) {
            context.waitFor("connected player", client -> client.level != null && client.player != null);
            world.server().runCommand("gamerule mobGriefing false");
            world.server().runCommand("time set noon");
            int[] ids = world.server().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                int[] result = new int[types.size()];
                for (int i = 0; i < types.size(); i++) {
                    Mob mob = (Mob) types.get(i).create(player.serverLevel());
                    mob.moveTo(player.getX() + 8, player.getY() + 3, player.getZ() + i * 2);
                    mob.setNoAi(true);
                    mob.setNoGravity(true);
                    mob.setInvulnerable(true);
                    mob.setPersistenceRequired();
                    player.serverLevel().addFreshEntity(mob);
                    result[i] = mob.getId();
                }
                return result;
            });
            context.waitFor("all eight client entities", client -> {
                for (int id : ids) if (!(client.level.getEntity(id) instanceof Mob)) return false;
                return true;
            });
            context.runOnClient(client -> {
                for (int id : ids) {
                    var entity = client.level.getEntity(id);
                    var renderer = client.getEntityRenderDispatcher().getRenderer(entity);
                    var texture = renderer.getTextureLocation(entity);
                    if (client.getResourceManager().getResource(texture).isEmpty())
                        throw new AssertionError("Missing mob texture: " + texture);
                    if (entity.isMultipartEntity()) {
                        var parts = entity.getParts();
                        for (int i = 0; i < parts.length; i++)
                            if (parts[i].getId() != id + i + 1)
                                throw new AssertionError("Client multipart IDs disagree with the parent");
                    }
                }
                for (var item : BuiltInRegistries.ITEM) {
                    var key = BuiltInRegistries.ITEM.getKey(item);
                    if (!key.getNamespace().equals(BertieCreatures.MODID)) continue;
                    var model = client.getItemRenderer().getModel(new ItemStack(item), client.level, client.player, 0);
                    if (model == client.getModelManager().getMissingModel()
                            || model.getParticleIcon()
                                    .contents()
                                    .name()
                                    .equals(ResourceLocation.withDefaultNamespace("missingno")))
                        throw new AssertionError("Missing item model or texture: " + key);
                }
            });
            context.waitTicks(20);
            for (int i = 0; i < ids.length; i++) {
                int id = ids[i];
                String name = BuiltInRegistries.ENTITY_TYPE.getKey(types.get(i)).getPath();
                context.setScreen(() -> new PreviewScreen(
                        (Mob) net.minecraft.client.Minecraft.getInstance().level.getEntity(id)));
                context.takeScreenshot(name);
                context.runOnClient(client -> {
                    if (client.level.getEntity(id) instanceof IAnimatedEntity animated) {
                        for (var animation : animated.getAnimations()) {
                            if (animation == IAnimatedEntity.NO_ANIMATION || animation.getDuration() < 4) continue;
                            animated.setAnimation(animation);
                            animated.setAnimationTick(animation.getDuration() / 2);
                            break;
                        }
                    }
                });
                context.takeScreenshot(name + "-animation");
                context.setScreen(() -> null);
            }
            context.runOnClient(client -> ((Mob) client.level.getEntity(ids[2]))
                    .addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            io.github.bertie_mc.creatures.server.potion.ACEffectRegistry.BUBBLED, 200)));
            context.setScreen(() -> new PreviewScreen(
                    (Mob) net.minecraft.client.Minecraft.getInstance().level.getEntity(ids[2])));
            context.takeScreenshot("bubbled-vesper");
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                client.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        io.github.bertie_mc.creatures.server.potion.ACEffectRegistry.BUBBLED, 200));
                client.player.setYRot(0);
                client.player.setXRot(0);
            });
            context.takeScreenshot("bubble-first-person");
            context.runOnClient(client -> {
                client.player.removeEffect(io.github.bertie_mc.creatures.server.potion.ACEffectRegistry.BUBBLED);
                for (var particle :
                        io.github.bertie_mc.creatures.client.particle.ACParticleRegistry.DEF_REG.getEntries()) {
                    var created = client.particleEngine.createParticle(
                            (net.minecraft.core.particles.SimpleParticleType) particle.get(),
                            client.player.getX(),
                            client.player.getEyeY(),
                            client.player.getZ() + 6,
                            0,
                            0,
                            0);
                    if (created == null) throw new AssertionError("Missing particle provider: " + particle.getId());
                }
            });
            context.waitTicks(50);
            context.takeScreenshot("particles");
        } finally {
            context.setScreen(() -> null);
        }
    }

    private static final class PreviewScreen extends Screen {
        private final Mob mob;

        private PreviewScreen(Mob mob) {
            super(mob.getType().getDescription());
            this.mob = mob;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            graphics.fill(0, 0, width, height, 0xff263440);
            graphics.drawCenteredString(font, title, width / 2, 14, 0xffffff);
            renderMob(graphics, width / 4, 120);
            renderMob(graphics, width * 3 / 4, 240);
        }

        private void renderMob(GuiGraphics graphics, int x, float yaw) {
            float extent = Math.max(mob.getBbHeight(), mob.getBbWidth() * 2.8F);
            if (mob.isMultipartEntity()) {
                for (var part : mob.getParts()) extent = Math.max(extent, (float) part.distanceTo(mob) * 2.5F);
            }
            if (mob.getType() == ACEntityRegistry.VESPER.get()) extent = 5;
            int scale = Math.max(3, (int) (Math.min(width / 2.4F, height * 0.7F) / extent));
            InventoryScreen.renderEntityInInventory(
                    graphics,
                    x,
                    height / 2F + 12,
                    scale,
                    new Vector3f(0, mob.getBbHeight() / 2, 0),
                    new Quaternionf().rotateZ((float) Math.PI).rotateX(0.15F).rotateY((float) Math.toRadians(yaw)),
                    new Quaternionf().rotateX(0.15F),
                    mob);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
