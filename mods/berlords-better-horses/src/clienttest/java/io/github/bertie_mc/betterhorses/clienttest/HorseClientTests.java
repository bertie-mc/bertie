package io.github.bertie_mc.betterhorses.clienttest;

import io.github.bertie_mc.betterhorses.*;
import io.github.bertie_mc.betterhorses.client.BetterHorseScreen;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.item.ItemStack;

public final class HorseClientTests {
    private HorseClientTests() {}

    @ClientTest
    public static void equipmentSyncMenuAndAppearance(ClientTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.waitFor(
                    "player's local chunk",
                    client -> client.player != null
                            && client.level != null
                            && client.level.hasChunkAt(client.player.blockPosition()));
            int horseId = world.server().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                Horse horse = EntityType.HORSE.create(player.serverLevel());
                horse.moveTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
                horse.setTamed(true);
                horse.setNoAi(true);
                horse.setAge(0);
                horse.setVariant(Variant.CHESTNUT);
                ((HorseEquipment) horse).betterhorses$saddleInventory().setItem(0, BetterHorses.PASSENGER.toStack());
                ((HorseEquipment) horse).betterhorses$shoes().setItem(0, BetterHorses.DIAMOND.toStack());
                horse.setBodyArmorItem(BetterHorses.ARMOR.toStack());
                player.serverLevel().addFreshEntity(horse);
                return horse.getId();
            });
            context.waitFor(
                    "horse equipment sync",
                    client -> client.level.getEntity(horseId) instanceof Horse horse
                            && ((HorseEquipment) horse).betterhorses$tier() == ShoeTier.DIAMOND
                            && ((HorseEquipment) horse)
                                    .betterhorses$syncedSaddle()
                                    .is(BetterHorses.PASSENGER.get()));
            world.server().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                ((Horse) player.serverLevel().getEntity(horseId)).openCustomInventoryScreen(player);
            });
            context.waitForScreen(BetterHorseScreen.class);
            context.runOnClient(client -> {
                var menu = ((BetterHorseScreen) client.screen).getMenu();
                if (menu.slots.size() != 39 || !menu.getSlot(2).getItem().is(BetterHorses.DIAMOND.get()))
                    throw new AssertionError("Horse menu did not synchronize its third slot");
                for (String name : new String[] {
                    "horse_effigy",
                    "iron_horseshoes",
                    "gold_horseshoes",
                    "diamond_horseshoes",
                    "netherite_horseshoes",
                    "netherite_horse_armor",
                    "passenger_saddle",
                    "warrior_saddle",
                    "wanderer_saddle"
                }) {
                    var model = client.getItemRenderer()
                            .getModel(
                                    new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                                            BetterHorses.id(name))),
                                    client.level,
                                    client.player,
                                    0);
                    if (model.getParticleIcon()
                            .contents()
                            .name()
                            .equals(ResourceLocation.withDefaultNamespace("missingno")))
                        throw new AssertionError("Missing item texture: " + name);
                }
            });
            context.takeScreenshot("horse-inventory");
            context.runOnClient(client -> client.player.closeContainer());
            context.waitForScreen(null);
            for (String saddle : new String[] {"passenger", "warrior", "wanderer"}) {
                world.server().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    Horse horse = (Horse) player.serverLevel().getEntity(horseId);
                    horse.setBodyArmorItem(saddle.equals("warrior") ? BetterHorses.ARMOR.toStack() : ItemStack.EMPTY);
                    ((HorseEquipment) horse).betterhorses$shoes().setItem(0, BetterHorses.NETHERITE.toStack());
                    ((HorseEquipment) horse)
                            .betterhorses$saddleInventory()
                            .setItem(
                                    0,
                                    new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                                            BetterHorses.id(saddle + "_saddle"))));
                });
                world.connection().waitForClientboundEntityUpdates(EntityType.HORSE);
                context.setScreen(() -> new PreviewScreen(
                        (Horse) net.minecraft.client.Minecraft.getInstance()
                                .level
                                .getEntity(horseId),
                        saddle));
                context.takeScreenshot("horse-" + saddle);
                context.setScreen(() -> null);
            }
        }
    }

    private static final class PreviewScreen extends Screen {
        private final Horse horse;

        PreviewScreen(Horse horse, String title) {
            super(Component.literal(title));
            this.horse = horse;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xff20252b);
            graphics.drawCenteredString(font, title, width / 2, 12, 0xffffff);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    width / 2 - 140,
                    30,
                    width / 2 + 140,
                    height - 10,
                    85,
                    0.4F,
                    width / 2 + 220,
                    height / 2,
                    horse);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
