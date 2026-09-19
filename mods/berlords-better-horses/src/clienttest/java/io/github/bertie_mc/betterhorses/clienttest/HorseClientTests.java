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
                if (menu.slots.size() != 54
                        || menu.hasStorage()
                        || !menu.getSlot(2).getItem().is(BetterHorses.DIAMOND.get()))
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

    @ClientTest
    public static void travellerStorageAndImmediateJump(ClientTestContext context) {
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
                horse.setAge(0);
                horse.setVariant(Variant.CHESTNUT);
                horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH)
                        .setBaseValue(0.5);
                HorseEquipment equipment = (HorseEquipment) horse;
                equipment.betterhorses$saddleInventory().setItem(0, BetterHorses.WANDERER.toStack());
                equipment.betterhorses$shoes().setItem(0, BetterHorses.DIAMOND.toStack());
                equipment.betterhorses$storage().setItem(0, new ItemStack(net.minecraft.world.item.Items.APPLE, 17));
                equipment.betterhorses$storage().setItem(14, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
                player.serverLevel().addFreshEntity(horse);
                player.startRiding(horse);
                return horse.getId();
            });
            context.waitFor(
                    "mounted traveller",
                    client -> client.player.getVehicle() instanceof Horse horse
                            && horse.getId() == horseId
                            && horse.onGround()
                            && ((HorseEquipment) horse).betterhorses$traveller());
            world.server().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                ((Horse) player.getVehicle()).openCustomInventoryScreen(player);
            });
            context.waitForScreen(BetterHorseScreen.class);
            context.waitFor("cargo sync", client -> {
                var menu = ((BetterHorseScreen) client.screen).getMenu();
                return menu.hasStorage()
                        && menu.getSlot(BetterHorseMenu.STORAGE_START).getItem().getCount() == 17
                        && menu.getSlot(BetterHorseMenu.STORAGE_END - 1)
                                        .getItem()
                                        .getCount()
                                == 3;
            });
            context.runOnClient(client -> {
                var menu = ((BetterHorseScreen) client.screen).getMenu();
                for (int i = BetterHorseMenu.STORAGE_START; i < BetterHorseMenu.STORAGE_END; i++)
                    if (!menu.getSlot(i).isActive()) throw new AssertionError("Storage slot is inactive: " + i);
                var tooltip = BetterHorses.WANDERER
                        .toStack()
                        .getTooltipLines(
                                net.minecraft.world.item.Item.TooltipContext.of(client.level),
                                client.player,
                                net.minecraft.world.item.TooltipFlag.NORMAL);
                if (tooltip.stream().noneMatch(line -> line.getString().equals("Extra space, perfect jump")))
                    throw new AssertionError("Traveller tooltip missing");
            });
            context.takeScreenshot("traveller-inventory");
            context.runOnClient(client -> client.gameMode.handleInventoryMouseClick(
                    client.player.containerMenu.containerId,
                    BetterHorseMenu.STORAGE_START,
                    0,
                    net.minecraft.world.inventory.ClickType.QUICK_MOVE,
                    client.player));
            world.connection().waitForServerboundPackets();
            context.waitFor(
                    "cargo transfers to player",
                    client -> client.player.getInventory().countItem(net.minecraft.world.item.Items.APPLE) == 17
                            && client.player
                                    .containerMenu
                                    .getSlot(BetterHorseMenu.STORAGE_START)
                                    .getItem()
                                    .isEmpty());
            world.server().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                if (!((HorseEquipment) player.getVehicle())
                                .betterhorses$storage()
                                .getItem(0)
                                .isEmpty()
                        || player.getInventory().countItem(net.minecraft.world.item.Items.APPLE) != 17)
                    throw new AssertionError("Server cargo transfer disagrees with client");
            });
            context.runOnClient(client -> client.player.closeContainer());
            context.waitForScreen(null);
            context.waitFor("settled horse before jump", client -> {
                var horse = client.player.getVehicle();
                return horse.onGround() && Math.abs(horse.getY() - Math.rint(horse.getY())) < 0.001;
            });
            double ground =
                    context.computeOnClient(client -> client.player.getVehicle().getY());
            double expectedApex = HorsePhysics.apex(0.5, 0.08) + 2;
            context.input().holdKey(options -> options.keyJump);
            context.waitTicks(2);
            context.runOnClient(client -> {
                Horse horse = (Horse) client.player.getVehicle();
                if (horse.getY() <= ground + 0.1)
                    throw new AssertionError("Jump waited for release instead of key press");
            });
            double apex = 0;
            boolean landed = false;
            for (int i = 0; i < 70; i++) {
                context.waitTick();
                double height = context.computeOnClient(
                        client -> client.player.getVehicle().getY() - ground);
                apex = Math.max(apex, height);
                boolean onGround = context.computeOnClient(
                        client -> client.player.getVehicle().onGround());
                if (landed && !onGround) throw new AssertionError("Holding jump repeated the jump");
                landed |= onGround;
            }
            if (!landed || Math.abs(apex - expectedApex) > 0.15)
                throw new AssertionError("Wrong full jump height: " + apex + ", expected " + expectedApex
                        + ", initial ground=" + ground + ", final state="
                        + context.computeOnClient(client -> {
                            Horse horse = (Horse) client.player.getVehicle();
                            return "y=" + horse.getY() + ", jump="
                                    + horse.getAttributeValue(
                                            net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH)
                                    + ", gravity=" + horse.getGravity();
                        }));
            context.input().releaseKey(options -> options.keyJump);
            context.waitTicks(3);
            context.runOnClient(client -> {
                if (!client.player.getVehicle().onGround()) throw new AssertionError("Release triggered a second jump");
            });
            context.input().holdKey(options -> options.keyJump);
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (client.player.getVehicle().getY() <= ground + 0.1)
                    throw new AssertionError("Second press did not jump");
            });
            context.input().releaseKey(options -> options.keyJump);
            context.waitFor(
                    "land after second jump",
                    client -> client.player.getVehicle().onGround());
            world.server().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                ((HorseEquipment) player.getVehicle())
                        .betterhorses$saddleInventory()
                        .setItem(0, new ItemStack(net.minecraft.world.item.Items.SADDLE));
            });
            context.waitFor(
                    "ordinary saddle sync",
                    client -> !((HorseEquipment) client.player.getVehicle()).betterhorses$traveller());
            context.input().holdKey(options -> options.keyJump);
            context.waitTicks(3);
            context.runOnClient(client -> {
                if (!client.player.getVehicle().onGround() || client.player.getJumpRidingScale() <= 0)
                    throw new AssertionError("Ordinary saddle no longer charges while held");
            });
            context.input().releaseKey(options -> options.keyJump);
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (client.player.getVehicle().onGround())
                    throw new AssertionError("Ordinary saddle must jump on release");
            });
        } finally {
            context.input().releaseKey(options -> options.keyJump);
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
