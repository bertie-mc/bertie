package io.github.bertie_mc.betterhorses.clienttest;

import io.github.bertie_mc.betterhorses.*;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class HorseAppleClientTests {
    private HorseAppleClientTests() {}

    @ClientTest
    public static void appleRecipesAndAppearancePersistence(ClientTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.waitFor("apple test world", client -> client.player != null && client.level != null);
            world.server().runCommand("gamemode survival @a");
            UUID horseId = world.server().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                Horse horse = EntityType.HORSE.create(player.serverLevel());
                horse.moveTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
                horse.setTamed(true);
                horse.setAge(0);
                horse.setNoAi(true);
                horse.setOwnerUUID(player.getUUID());
                horse.setCustomName(Component.literal("Apple Tester"));
                CompoundTag appearance = horse.saveWithoutId(new CompoundTag());
                appearance.putInt("Variant", Variant.BLACK.getId() | Markings.WHITE_DOTS.getId() << 8);
                horse.load(appearance);
                horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
                horse.setHealth(27);
                HorseEquipment equipment = (HorseEquipment) horse;
                equipment.betterhorses$saddleInventory().setItem(0, BetterHorses.WANDERER.toStack());
                equipment.betterhorses$shoes().setItem(0, BetterHorses.DIAMOND.toStack());
                equipment.betterhorses$storage().setItem(14, new ItemStack(Items.DIAMOND, 7));
                horse.setBodyArmorItem(BetterHorses.ARMOR.toStack());
                player.serverLevel().addFreshEntity(horse);
                horse.setLeashedTo(player, true);
                player.startRiding(horse);
                double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
                double jump = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
                ItemStack apples = new ItemStack(BetterHorses.ZOMBIE_APPLE.get(), 2);
                feed(player, horse, apples);
                require(apples.getCount() == 1, "Zombie apple should consume once");
                require(
                        ((HorseAppearance) horse).betterhorses$appearance() == HorseAppearance.Style.ZOMBIE,
                        "Zombie appearance");
                feed(player, horse, apples);
                require(apples.getCount() == 1, "Repeated zombie apple should not be wasted");
                require(
                        horse.getType() == EntityType.HORSE
                                && horse.isTamed()
                                && horse.isVehicle()
                                && horse.getLeashHolder() == player
                                && horse.getOwnerUUID().equals(player.getUUID()),
                        "Appearance must preserve entity, rider, owner and lead");
                require(
                        horse.getHealth() == 27
                                && horse.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed
                                && horse.getAttributeValue(Attributes.JUMP_STRENGTH) == jump,
                        "Appearance must preserve health and attributes");
                checkEquipment(horse);
                Horse loaded = EntityType.HORSE.create(player.serverLevel());
                loaded.load(horse.saveWithoutId(new CompoundTag()));
                require(
                        ((HorseAppearance) loaded).betterhorses$appearance() == HorseAppearance.Style.ZOMBIE
                                && loaded.getVariant() == Variant.BLACK
                                && loaded.getMarkings() == Markings.WHITE_DOTS,
                        "NBT must preserve appearance and original coat");
                checkEquipment(loaded);
                checkRecipes(player);
                return horse.getUUID();
            });
            context.waitFor(
                    "zombie appearance sync",
                    client -> client.player.getVehicle() instanceof Horse horse
                            && horse.getUUID().equals(horseId)
                            && ((HorseAppearance) horse).betterhorses$appearance() == HorseAppearance.Style.ZOMBIE);
            context.runOnClient(client -> {
                Horse horse = (Horse) client.player.getVehicle();
                var renderer = client.getEntityRenderDispatcher().getRenderer(horse);
                require(
                        renderer.getTextureLocation(horse).getPath().endsWith("horse_zombie.png"),
                        "Zombie texture selection");
            });
            context.setScreen(() -> new HorseClientTests.PreviewScreen(
                    (Horse) net.minecraft.client.Minecraft.getInstance().player.getVehicle(),
                    "Zombie appearance with equipment"));
            context.takeScreenshot("horse-zombie-equipped");
            context.setScreen(() -> null);

            world.server().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                Horse horse = (Horse) player.getVehicle();
                feed(player, horse, BetterHorses.SKELETON_APPLE.toStack());
                require(
                        ((HorseAppearance) horse).betterhorses$appearance() == HorseAppearance.Style.SKELETON,
                        "Can switch directly to skeleton appearance");
                for (int i = 0; i < 24; i++) {
                    Variant before = horse.getVariant();
                    ItemStack breed = BetterHorses.BREED_APPLE.toStack();
                    feed(player, horse, breed);
                    require(
                            horse.getVariant() != before && breed.isEmpty(),
                            "Breed apple must select a different coat and consume once");
                    require(horse.getMarkings() == Markings.WHITE_DOTS, "Breed apple must preserve markings");
                    require(
                            ((HorseAppearance) horse).betterhorses$appearance() == HorseAppearance.Style.SKELETON,
                            "Coat color must not remove cosmetic appearance");
                }
                Variant coat = horse.getVariant();
                ItemStack enchanted = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 2);
                feed(player, horse, enchanted);
                require(
                        enchanted.getCount() == 1 && horse.getHealth() == 37 && horse.isInLove(),
                        "Restoring apple preserves vanilla healing and breeding, without double consumption");
                require(
                        ((HorseAppearance) horse).betterhorses$appearance() == HorseAppearance.Style.NORMAL
                                && horse.getVariant() == coat
                                && horse.getMarkings() == Markings.WHITE_DOTS,
                        "Restoration reveals retained coat and markings");
                player.setGameMode(GameType.CREATIVE);
                ItemStack creative = new ItemStack(BetterHorses.SKELETON_APPLE.get(), 2);
                feed(player, horse, creative);
                require(creative.getCount() == 2, "Creative must not consume apples");
                player.setGameMode(GameType.SURVIVAL);
                checkEquipment(horse);
            });
            ItemStack effigy = BetterHorses.EFFIGY.toStack();
            Vec3 release = world.server().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                Horse horse = (Horse) player.getVehicle();
                Vec3 position = horse.position().add(3, 0, 0);
                player.stopRiding();
                require(
                        BetterHorses.EFFIGY
                                .get()
                                .interactLivingEntity(effigy, player, horse, InteractionHand.MAIN_HAND)
                                .consumesAction(),
                        "Capture cosmetic horse");
                return position;
            });
            context.waitTicks(2);
            int restoredId = world.server().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                require(HorseEffigyItem.release(effigy, player.serverLevel(), release, 0), "Release cosmetic horse");
                Horse horse = (Horse) player.serverLevel().getEntity(horseId);
                require(
                        horse != null
                                && ((HorseAppearance) horse).betterhorses$appearance() == HorseAppearance.Style.SKELETON
                                && horse.getName().getString().equals("Apple Tester")
                                && horse.getOwnerUUID().equals(player.getUUID()),
                        "Effigy preserves appearance and identity");
                checkEquipment(horse);
                return horse.getId();
            });
            context.waitFor(
                    "released horse equipment",
                    client -> client.level.getEntity(restoredId) instanceof Horse horse
                            && horse.getBodyArmorItem().is(BetterHorses.ARMOR.get()));
            context.waitTicks(3);
            world.server().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                Horse horse = (Horse) player.serverLevel().getEntity(restoredId);
                horse.setBodyArmorItem(ItemStack.EMPTY);
                ((HorseEquipment) horse).betterhorses$saddleInventory().setItem(0, ItemStack.EMPTY);
                ((HorseEquipment) horse).betterhorses$shoes().setItem(0, ItemStack.EMPTY);
            });
            context.waitFor(
                    "unequipped appearance preview",
                    client -> client.level.getEntity(restoredId) instanceof Horse horse
                            && horse.getBodyArmorItem().isEmpty()
                            && ((HorseEquipment) horse)
                                    .betterhorses$syncedSaddle()
                                    .isEmpty()
                            && ((HorseEquipment) horse)
                                    .betterhorses$syncedShoes()
                                    .isEmpty());
            for (HorseAppearance.Style style : new HorseAppearance.Style[] {
                HorseAppearance.Style.SKELETON, HorseAppearance.Style.ZOMBIE, HorseAppearance.Style.NORMAL
            }) {
                world.server().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    Horse horse = (Horse) player.serverLevel().getEntity(restoredId);
                    feed(
                            player,
                            horse,
                            switch (style) {
                                case ZOMBIE -> BetterHorses.ZOMBIE_APPLE.toStack();
                                case SKELETON -> BetterHorses.SKELETON_APPLE.toStack();
                                case NORMAL -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
                            });
                });
                context.waitFor(
                        "appearance " + style,
                        client -> client.level.getEntity(restoredId) instanceof Horse horse
                                && ((HorseAppearance) horse).betterhorses$appearance() == style);
                context.setScreen(() -> new HorseClientTests.PreviewScreen(
                        (Horse) net.minecraft.client.Minecraft.getInstance()
                                .level
                                .getEntity(restoredId),
                        style.name()));
                context.takeScreenshot("horse-appearance-" + style.name().toLowerCase(Locale.ROOT));
                context.setScreen(() -> null);
            }
        }
    }

    @ClientTest
    public static void appleItemPreview(ClientTestContext context) {
        context.setScreen(ApplesScreen::new);
        context.takeScreenshot("horse-apples");
        context.setScreen(() -> null);
    }

    private static void feed(ServerPlayer player, Horse horse, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var event = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, horse);
        NeoForge.EVENT_BUS.post(event);
        require(event.isCanceled(), "Apple feeding must take priority over mounting/opening inventory");
    }

    private static void checkEquipment(Horse horse) {
        HorseEquipment equipment = (HorseEquipment) horse;
        require(
                equipment.betterhorses$traveller()
                        && equipment.betterhorses$tier() == ShoeTier.DIAMOND
                        && equipment.betterhorses$storage().getItem(14).getCount() == 7
                        && horse.getBodyArmorItem().is(BetterHorses.ARMOR.get()),
                "Equipment and cargo must remain intact");
    }

    private static void checkRecipes(ServerPlayer player) {
        Item[] ingredients = {Items.ROTTEN_FLESH, Items.BONE, Items.RED_DYE};
        Item[] results = {
            BetterHorses.ZOMBIE_APPLE.get(), BetterHorses.SKELETON_APPLE.get(), BetterHorses.BREED_APPLE.get()
        };
        for (int kind = 0; kind < ingredients.length; kind++) {
            List<ItemStack> grid = new ArrayList<>();
            for (int slot = 0; slot < 9; slot++) grid.add(new ItemStack(slot == 4 ? Items.APPLE : ingredients[kind]));
            if (kind == 2) {
                for (int slot = 0; slot < 9; slot++)
                    if (slot != 4) grid.set(slot, new ItemStack(DyeItem.byColor(DyeColor.values()[slot])));
            }
            var recipe = player.serverLevel()
                    .getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), player.serverLevel());
            require(recipe.isPresent(), "Apple recipe must match, including mixed dyes");
            ItemStack result =
                    recipe.orElseThrow().value().assemble(CraftingInput.of(3, 3, grid), player.registryAccess());
            require(result.is(results[kind]) && result.getCount() == 1, "Apple recipe output");
            grid.set(4, new ItemStack(Items.CARROT));
            require(
                    !recipe.orElseThrow().value().matches(CraftingInput.of(3, 3, grid), player.serverLevel()),
                    "Apple is required in the center");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class ApplesScreen extends Screen {
        ApplesScreen() {
            super(Component.literal("Horse Apples"));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            graphics.fill(0, 0, width, height, 0xff20252b);
            graphics.drawCenteredString(font, title, width / 2, 25, 0xffffff);
            ItemStack[] apples = {
                BetterHorses.ZOMBIE_APPLE.toStack(),
                BetterHorses.SKELETON_APPLE.toStack(),
                BetterHorses.BREED_APPLE.toStack()
            };
            for (int i = 0; i < apples.length; i++) {
                int x = width * (1 + i * 2) / 6;
                graphics.pose().pushPose();
                graphics.pose().translate(x - 32, height / 2 - 36, 0);
                graphics.pose().scale(4, 4, 1);
                graphics.renderItem(apples[i], 0, 0);
                graphics.pose().popPose();
                graphics.drawCenteredString(font, apples[i].getHoverName(), x, height / 2 + 42, 0xffffff);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
