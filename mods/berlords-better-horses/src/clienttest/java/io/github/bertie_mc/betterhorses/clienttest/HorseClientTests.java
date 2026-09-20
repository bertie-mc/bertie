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
            world.server().runOnServer(server -> {
                Horse horse = (Horse) server.getPlayerList()
                        .getPlayers()
                        .getFirst()
                        .serverLevel()
                        .getEntity(horseId);
                ((HorseEquipment) horse).betterhorses$saddleInventory().setItem(0, ItemStack.EMPTY);
                ((HorseEquipment) horse).betterhorses$shoes().setItem(0, ItemStack.EMPTY);
            });
            context.waitFor(
                    "empty equipment slots",
                    client -> client.player.containerMenu.getSlot(0).getItem().isEmpty()
                            && client.player.containerMenu.getSlot(2).getItem().isEmpty());
            context.takeScreenshot("horse-inventory-empty-slots");
            context.runOnClient(client -> client.player.closeContainer());
            context.waitForScreen(null);
            for (String saddle : new String[] {"passenger", "warrior", "wanderer"}) {
                world.server().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    Horse horse = (Horse) player.serverLevel().getEntity(horseId);
                    horse.setBodyArmorItem(ItemStack.EMPTY);
                    ((HorseEquipment) horse).betterhorses$shoes().setItem(0, BetterHorses.NETHERITE.toStack());
                    ((HorseEquipment) horse)
                            .betterhorses$saddleInventory()
                            .setItem(
                                    0,
                                    new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                                            BetterHorses.id(saddle + "_saddle"))));
                });
                world.connection().waitForClientboundEntityUpdates(EntityType.HORSE);
                world.server().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    ((Horse) player.serverLevel().getEntity(horseId)).openCustomInventoryScreen(player);
                });
                context.waitForScreen(BetterHorseScreen.class);
                context.takeScreenshot("horse-inventory-" + saddle);
                context.runOnClient(client -> client.player.closeContainer());
                context.waitForScreen(null);
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
                if (tooltip.stream().noneMatch(line -> line.getString().equals("Extra Space, Perfect Jump")))
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

    @ClientTest
    public static void mountedSwimming(ClientTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.waitFor(
                    "local chunk",
                    client -> client.player != null && client.level.hasChunkAt(client.player.blockPosition()));
            int[] origin = world.server().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var level = player.serverLevel();
                var base = player.blockPosition();
                for (int x = -4; x <= 4; x++)
                    for (int z = -5; z <= 145; z++)
                        for (int y = -4; y <= 5; y++) {
                            var block = y == -4 || y < 0 && (z < 55 || z >= 125)
                                    ? net.minecraft.world.level.block.Blocks.STONE
                                    : y < 0
                                            ? net.minecraft.world.level.block.Blocks.WATER
                                            : net.minecraft.world.level.block.Blocks.AIR;
                            level.setBlockAndUpdate(base.offset(x, y, z), block.defaultBlockState());
                        }
                Horse horse = EntityType.HORSE.create(level);
                horse.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
                horse.setTamed(true);
                horse.setAge(0);
                horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                        .setBaseValue(0.225);
                ((HorseEquipment) horse).betterhorses$saddleInventory().setItem(0, BetterHorses.WARRIOR.toStack());
                ((HorseEquipment) horse).betterhorses$shoes().setItem(0, BetterHorses.IRON.toStack());
                horse.setBodyArmorItem(BetterHorses.ARMOR.toStack());
                var appearance = horse.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                appearance.putInt(
                        "Variant",
                        Variant.CHESTNUT.getId()
                                | net.minecraft.world.entity.animal.horse.Markings.WHITE_DOTS.getId() << 8);
                horse.load(appearance);
                level.addFreshEntity(horse);
                player.startRiding(horse);
                return new int[] {horse.getId(), base.getY(), base.getZ()};
            });
            context.waitFor(
                    "mounted on runway",
                    client -> client.player.getVehicle() instanceof Horse horse
                            && horse.getId() == origin[0]
                            && horse.onGround());
            context.runOnClient(client -> {
                client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                client.player.setYRot(0);
                client.player.yRotO = 0;
                client.player.setXRot(0);
                client.player.xRotO = 0;
            });
            context.input().holdKey(options -> options.keyUp);
            context.waitTicks(30);
            double dryStart =
                    context.computeOnClient(client -> client.player.getVehicle().getZ());
            context.waitTicks(20);
            double drySpeed =
                    context.computeOnClient(client -> client.player.getVehicle().getZ()) - dryStart;
            context.waitFor(
                    "ride into deep water",
                    client -> client.player.getVehicle() instanceof Horse horse && horse.isInWater(),
                    600);
            context.waitFor(
                    "still-water chunks ahead of the horse",
                    client -> {
                        var horse = client.player.getVehicle();
                        if (horse == null) return false;
                        var start = net.minecraft.core.BlockPos.containing(horse.getX(), origin[1] - 1, horse.getZ());
                        for (int ahead = 0; ahead < 18; ahead++) {
                            var pos = start.offset(0, 0, ahead);
                            var fluid = client.level.getFluidState(pos);
                            if (!fluid.isSource()
                                    || fluid.getFlow(client.level, pos).horizontalDistanceSqr() > 0.0001) return false;
                        }
                        return true;
                    },
                    200);
            context.waitTicks(35);
            double waterStart =
                    context.computeOnClient(client -> client.player.getVehicle().getZ());
            context.waitTicks(20);
            double swimSpeed =
                    context.computeOnClient(client -> client.player.getVehicle().getZ()) - waterStart;
            if (Math.abs(swimSpeed / drySpeed - 0.5) > 0.025)
                throw new AssertionError("Swimming ratio=" + swimSpeed / drySpeed + ", dry=" + drySpeed + ", swim="
                        + swimSpeed
                        + context.computeOnClient(client -> {
                            var horse = client.player.getVehicle();
                            var pos = horse.blockPosition();
                            return ", position=" + horse.position() + ", current="
                                    + client.level.getFluidState(pos).getFlow(client.level, pos);
                        }));
            context.runOnClient(client -> {
                Horse horse = (Horse) client.player.getVehicle();
                if (!horse.isInWater()
                        || Math.abs(horse.getY() - (origin[1] - 0.8)) > 0.2
                        || horse.getAirSupply() < horse.getMaxAirSupply())
                    throw new AssertionError("Mounted horse must float with its head above water: y=" + horse.getY());
                client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                client.player.setXRot(15);
            });
            context.takeScreenshot("horse-swimming");
            context.waitFor(
                    "swim out onto shore",
                    client -> client.player.getVehicle() instanceof Horse horse
                            && horse.getZ() > origin[2] + 128
                            && horse.onGround()
                            && !horse.isInWater(),
                    800);
            context.input().releaseKey(options -> options.keyUp);
            world.connection().waitForServerboundPackets();
            world.server().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                if (!(player.getVehicle() instanceof Horse horse) || horse.getId() != origin[0])
                    throw new AssertionError("Server lost mounted rider while swimming");
                ((HorseEquipment) horse).betterhorses$shoes().setItem(0, BetterHorses.DIAMOND.toStack());
            });
            context.waitFor(
                    "waterwalking shoe sync",
                    client -> ((HorseEquipment) client.player.getVehicle()).betterhorses$tier() == ShoeTier.DIAMOND);
            context.waitTicks(10);
            context.runOnClient(client -> {
                client.player.setYRot(180);
                client.player.yRotO = 180;
            });
            context.input().holdKey(options -> options.keyUp);
            context.waitTicks(30);
            double walkingStart =
                    context.computeOnClient(client -> client.player.getVehicle().getZ());
            context.waitTicks(20);
            double walkingSpeed = walkingStart
                    - context.computeOnClient(
                            client -> client.player.getVehicle().getZ());
            double expectedWalkingSpeed = drySpeed
                    * (0.225 + 3 / HorsePhysics.BLOCKS_PER_SECOND_PER_ATTRIBUTE)
                    / (0.225 + 1 / HorsePhysics.BLOCKS_PER_SECOND_PER_ATTRIBUTE);
            if (Math.abs(walkingSpeed / expectedWalkingSpeed - 1) > 0.025)
                throw new AssertionError("Waterwalking should retain full ground speed: " + walkingSpeed + ", expected "
                        + expectedWalkingSpeed);
            context.runOnClient(client -> {
                Horse horse = (Horse) client.player.getVehicle();
                if (horse.isInWater() || !horse.onGround() || Math.abs(horse.getY() - (origin[1] - 1.0 / 9)) > 0.05)
                    throw new AssertionError("Waterwalking mount should remain on the surface: y=" + horse.getY()
                            + ", grounded=" + horse.onGround() + ", inWater=" + horse.isInWater());
            });
        } finally {
            context.input().releaseKey(options -> options.keyUp);
            context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        }
    }

    @ClientTest
    public static void equipmentTooltipsUseShift(ClientTestContext context) {
        var originalKey = context.computeOnClient(client -> client.options.keyShift.getKey());
        try (var world = context.worldBuilder().create()) {
            context.waitFor("tooltip world", client -> client.player != null && client.level != null);
            context.runOnClient(client -> {
                client.options.keyShift.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(
                        org.lwjgl.glfw.GLFW.GLFW_KEY_Z));
                net.minecraft.client.KeyMapping.resetMapping();
                for (var item : new net.minecraft.world.item.Item[] {
                    BetterHorses.IRON.get(),
                    BetterHorses.GOLD.get(),
                    BetterHorses.DIAMOND.get(),
                    BetterHorses.NETHERITE.get(),
                    BetterHorses.PASSENGER.get(),
                    BetterHorses.WARRIOR.get(),
                    BetterHorses.WANDERER.get()
                }) {
                    var lines = tooltip(client, new ItemStack(item));
                    assertTooltipLine(lines, "When Equipped:", net.minecraft.ChatFormatting.GRAY);
                    if (item instanceof HorseshoeItem shoes) {
                        assertTooltipLine(lines, "+" + shoes.tier.speed + " Speed", net.minecraft.ChatFormatting.BLUE);
                        if (shoes.tier.waterWalking())
                            assertTooltipLine(lines, "Waterwalking", net.minecraft.ChatFormatting.GOLD);
                        if (shoes.tier.lavaWalking())
                            assertTooltipLine(lines, "Lavawalking", net.minecraft.ChatFormatting.GOLD);
                    } else if (item == BetterHorses.PASSENGER.get())
                        assertTooltipLine(lines, "2 Seats", net.minecraft.ChatFormatting.BLUE);
                    else if (item == BetterHorses.WARRIOR.get())
                        assertTooltipLine(lines, "Lifelink", net.minecraft.ChatFormatting.BLUE);
                    else assertTooltipLine(lines, "Extra Space, Perfect Jump", net.minecraft.ChatFormatting.BLUE);
                    if (item instanceof SpecialSaddleItem
                            && !lines.getFirst().getString().contains("'s Saddle"))
                        throw new AssertionError("Saddle name needs possessive");
                }
            });
            context.setScreen(() -> new TooltipScreen(BetterHorses.NETHERITE.toStack()));
            context.takeScreenshot("horseshoes-tooltip");
            context.input().holdKey(org.lwjgl.glfw.GLFW.GLFW_KEY_Z);
            context.runOnClient(client -> {
                var lines = tooltip(client, BetterHorses.NETHERITE.toStack());
                if (lines.stream().anyMatch(line -> line.getString().contains("Allows walking")))
                    throw new AssertionError("Rebound crouch key must not expand tooltips");
                if (lines.stream().noneMatch(line -> line.getString().equals("Hold Shift for Details")))
                    throw new AssertionError("Hint must always show Shift");
            });
            context.input().releaseKey(org.lwjgl.glfw.GLFW.GLFW_KEY_Z);
            context.input().holdShift();
            context.runOnClient(client -> {
                String detail = String.join(
                        " ",
                        tooltip(client, BetterHorses.NETHERITE.toStack()).stream()
                                .map(Component::getString)
                                .toList());
                if (!detail.contains("Allows walking on water and powdered snow")
                        || !detail.contains("Allows walking on lava Protects from magma and campfires"))
                    throw new AssertionError("Shift must expand both walking abilities: " + detail);
                assertTooltipLine(
                        tooltip(client, BetterHorses.NETHERITE.toStack()),
                        "Allows walking on lava",
                        net.minecraft.ChatFormatting.GRAY);
                assertTooltipLine(
                        tooltip(client, BetterHorses.NETHERITE.toStack()),
                        "Protects from magma and campfires",
                        net.minecraft.ChatFormatting.GRAY);
            });
            context.takeScreenshot("horseshoes-tooltip-expanded");
            context.setScreen(() -> new TooltipScreen(BetterHorses.WARRIOR.toStack()));
            context.runOnClient(client -> {
                String detail = String.join(
                        " ",
                        tooltip(client, BetterHorses.WARRIOR.toStack()).stream()
                                .map(Component::getString)
                                .toList());
                if (!detail.contains("Transfers " + Math.round(BetterHorses.DAMAGE_TRANSFER.get() * 100)
                        + "% of horse damage to the rider"))
                    throw new AssertionError("Lifelink must explain configured damage transfer");
            });
            context.takeScreenshot("warrior-tooltip-expanded");
            context.input().releaseShift();
            context.takeScreenshot("warrior-tooltip");
            context.setScreen(() -> new TooltipScreen(BetterHorses.WANDERER.toStack()));
            context.takeScreenshot("traveller-tooltip");
            context.input().holdShift();
            context.runOnClient(client -> {
                var lines = tooltip(client, BetterHorses.WANDERER.toStack());
                assertTooltipLine(lines, "Has 15 inventory slots", net.minecraft.ChatFormatting.GRAY);
                assertTooltipLine(lines, "Jumps Instantly", net.minecraft.ChatFormatting.GRAY);
            });
            context.takeScreenshot("traveller-tooltip-expanded");
            context.input().releaseShift();
            context.setScreen(() -> new TooltipScreen(BetterHorses.PASSENGER.toStack()));
            context.takeScreenshot("passenger-tooltip");
            context.input().holdShift();
            context.runOnClient(client -> {
                String detail = String.join(
                        " ",
                        tooltip(client, BetterHorses.PASSENGER.toStack()).stream()
                                .map(Component::getString)
                                .toList());
                if (!detail.contains("Allows two players to ride the same horse"))
                    throw new AssertionError("Passenger saddle needs expanded details");
            });
            context.takeScreenshot("passenger-tooltip-expanded");
            context.input().releaseShift();
            context.setScreen(() -> null);
        } finally {
            context.input().releaseShift();
            context.input().releaseKey(org.lwjgl.glfw.GLFW.GLFW_KEY_Z);
            context.runOnClient(client -> {
                client.options.keyShift.setKey(originalKey);
                net.minecraft.client.KeyMapping.resetMapping();
            });
        }
    }

    private static java.util.List<Component> tooltip(net.minecraft.client.Minecraft client, ItemStack stack) {
        return stack.getTooltipLines(
                net.minecraft.world.item.Item.TooltipContext.of(client.level),
                client.player,
                net.minecraft.world.item.TooltipFlag.NORMAL);
    }

    private static void assertTooltipLine(
            java.util.List<Component> lines, String text, net.minecraft.ChatFormatting color) {
        if (lines.stream()
                .noneMatch(line -> line.getString().equals(text)
                        && line.getStyle().getColor() != null
                        && line.getStyle().getColor().getValue() == color.getColor()))
            throw new AssertionError("Missing styled tooltip line: " + text);
    }

    private static final class TooltipScreen extends Screen {
        private final ItemStack stack;

        TooltipScreen(ItemStack stack) {
            super(Component.literal("Equipment Tooltip"));
            this.stack = stack;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            graphics.fill(0, 0, width, height, 0xff20252b);
            graphics.renderItem(stack, 28, 30);
            graphics.renderTooltip(font, stack, 60, 40);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    static final class PreviewScreen extends Screen {
        private final Horse horse;

        PreviewScreen(Horse horse, String title) {
            super(Component.literal(title));
            this.horse = horse;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xff20252b);
            graphics.drawCenteredString(font, title, width / 2, 12, 0xffffff);
            renderHorse(graphics, width / 4, 120);
            renderHorse(graphics, width * 3 / 4, 240);
        }

        private void renderHorse(GuiGraphics graphics, int x, float yaw) {
            float body = horse.yBodyRot,
                    rotation = horse.getYRot(),
                    pitch = horse.getXRot(),
                    head = horse.yHeadRot,
                    oldHead = horse.yHeadRotO;
            try {
                horse.yBodyRot = yaw;
                horse.setYRot(yaw);
                horse.setXRot(0);
                horse.yHeadRot = horse.yHeadRotO = yaw;
                InventoryScreen.renderEntityInInventory(
                        graphics,
                        x,
                        height / 2.0F + 5,
                        68,
                        new org.joml.Vector3f(0, horse.getBbHeight() / 2 + 0.3F, 0),
                        new org.joml.Quaternionf().rotateZ((float) Math.PI).rotateX(0.18F),
                        new org.joml.Quaternionf().rotateX(0.18F),
                        horse);
            } finally {
                horse.yBodyRot = body;
                horse.setYRot(rotation);
                horse.setXRot(pitch);
                horse.yHeadRot = head;
                horse.yHeadRotO = oldHead;
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
