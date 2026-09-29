package io.github.bertie_mc.fletching.clienttest;

import com.fletchery.mod.client.render.LayeredBakedModelFactory;
import com.fletchery.mod.entity.CustomArrowEntity;
import com.fletchery.mod.registry.ModRegistries;
import io.github.bertie_mc.fletching.ArrowRecipe;
import io.github.bertie_mc.fletching.FletchingMenu;
import io.github.bertie_mc.fletching.PotionTank;
import io.github.bertie_mc.fletching.client.FletchingScreen;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import io.github.bertie_mc.testing.client.context.IntegratedWorldContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

public final class FletchingClientTests {
    private FletchingClientTests() {}

    private static ItemStack potion(Item container, int color) {
        ItemStack stack = new ItemStack(container);
        stack.set(
                DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.of(Potions.POISON), Optional.of(color), List.of()));
        return stack;
    }

    private static ItemStack arrows(int color) {
        PotionTank tank = new PotionTank();
        tank.fill(potion(Items.POTION, color));
        return ArrowRecipe.craft(
                new ItemStack(Items.PHANTOM_MEMBRANE),
                new ItemStack(Items.END_ROD),
                new ItemStack(Items.HEAVY_CORE),
                new ItemStack(Items.TNT),
                tank);
    }

    @ClientTest
    public static void alphaChannelColorsAndExistingArrowsRender(ClientTestContext context) {
        context.runOnClient(client -> {
            for (int color : new int[] {0xff8bafe0, 0x8bafe0, 0, 0xffffffff}) {
                ItemStack stack = arrows(color);
                var model = client.getItemRenderer().getModel(stack, null, null, 0);
                assertTextures(model);
                if (!model.getClass().getName().contains("CoatedModel"))
                    throw new AssertionError("Missing coating for " + Integer.toHexString(color));
                var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag();
                tag.putString("potionName", "#" + Integer.toHexString(color));
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                model = client.getItemRenderer().getModel(stack, null, null, 0);
                if (!model.getClass().getName().contains("CoatedModel"))
                    throw new AssertionError("Existing arrow lost its coating");
            }
        });
    }

    @ClientTest
    public static void tableGesturesPersistenceAndAllRenderPaths(ClientTestContext context) {
        try (var world = context.worldBuilder()
                .adjustSettings(settings -> settings.setName("Fletching regression"))
                .create()) {
            context.waitFor("player chunk available", client -> client.level.hasChunkAt(client.player.blockPosition()));
            BlockPos pos = world.server().computeOnServer(server -> {
                var player = world.connection().serverPlayer();
                var target = player.blockPosition().offset(1, 0, 0);
                player.getInventory().clearContent();
                player.getInventory().setItem(9, new ItemStack(Items.PHANTOM_MEMBRANE, 32));
                player.getInventory().setItem(10, new ItemStack(Items.END_ROD, 32));
                player.getInventory().setItem(11, new ItemStack(Items.HEAVY_CORE, 32));
                player.getInventory().setItem(12, new ItemStack(Items.TNT, 32));
                player.getInventory().setItem(13, potion(Items.POTION, 0xff8bafe0));
                player.getInventory().setItem(14, potion(Items.SPLASH_POTION, 0xff8bafe0));
                player.getInventory().setItem(15, potion(Items.LINGERING_POTION, 0xff8bafe0));
                player.getInventory().setItem(16, potion(Items.POTION, 0xffcc1122));
                player.serverLevel().setBlockAndUpdate(target, Blocks.FLETCHING_TABLE.defaultBlockState());
                return target;
            });
            context.waitTick();
            world.connection().waitForClientboundPackets();
            open(context, pos);
            for (int i = 5; i < 9; i++) clickSlot(context, i, true, 0);
            context.waitFor(
                    "arrow ingredients arrive", client -> menu(client).preview().getCount() == 8);
            clickSlot(context, 9, true, 0);
            context.waitFor(
                    "shift-fill returns bottle",
                    client -> menu(client).tankBatches() == 1
                            && menu(client).getSlot(9).getItem().is(Items.GLASS_BOTTLE));
            ItemStack combined =
                    context.computeOnClient(client -> menu(client).preview().copy());
            pointAtTank(context);
            context.takeScreenshot("fletching-tank-filled-tooltip");

            clickSlot(context, 4, false, 0);
            context.waitFor(
                    "left-click takes eight",
                    client -> menu(client).getCarried().getCount() == 8
                            && menu(client).tankBatches() == 0);
            clickSlot(context, 13, false, 0);
            clickSlot(context, 10, true, 0);
            context.waitFor("second bottle", client -> menu(client).tankBatches() == 1);
            clickSlot(context, 4, false, 1);
            context.waitFor(
                    "right-click takes a whole batch",
                    client -> menu(client).getCarried().getCount() == 8
                            && menu(client).tankBatches() == 0);
            clickSlot(context, 13, false, 0);
            context.waitFor(
                    "two crafts stack to sixteen",
                    client -> menu(client).getSlot(13).getItem().getCount() == 16);

            clickSlot(context, 11, true, 0);
            context.waitFor("lingering fill", client -> menu(client).tankBatches() == 1);
            restockPotion(context, world);
            clickSlot(context, 9, false, 0);
            context.waitFor(
                    "potion on cursor",
                    client -> PotionTank.isPotion(menu(client).getCarried()));
            pointAtTank(context);
            context.input().pressMouse(0);
            context.waitFor(
                    "tank click returns bottle on cursor",
                    client -> menu(client).getCarried().is(Items.GLASS_BOTTLE)
                            && menu(client).tankBatches() == 2);
            clickSlot(context, 9, false, 0);
            clickSlot(context, 12, true, 0);
            world.connection().waitForServerboundPackets();
            world.connection().waitForClientboundPackets();
            context.runOnClient(client -> {
                if (menu(client).tankBatches() != 2
                        || !PotionTank.isPotion(menu(client).getSlot(12).getItem()))
                    throw new AssertionError("Different potion mixed into tank");
            });
            context.input().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);
            context.waitForScreen(null);
            open(context, pos);
            context.waitFor("contents survive reopening", client -> menu(client).tankBatches() == 2);
            for (int batches = 3; batches <= 8; batches++) {
                restockPotion(context, world);
                clickSlot(context, 9, true, 0);
                final int expected = batches;
                context.waitFor("tank top-up", client -> menu(client).tankBatches() == expected);
            }
            restockPotion(context, world);
            clickSlot(context, 9, true, 0);
            world.connection().waitForServerboundPackets();
            world.connection().waitForClientboundPackets();
            context.runOnClient(client -> {
                if (menu(client).tankBatches() != 8
                        || !PotionTank.isPotion(menu(client).getSlot(9).getItem()))
                    throw new AssertionError("Full tank consumed a potion");
            });
            context.takeScreenshot("fletching-tank-full");
            clickSlot(context, 4, true, 0);
            context.waitFor(
                    "shift-craft drains exactly the coated batches",
                    client -> menu(client).tankBatches() == 0
                            && client.player.getInventory().countItem(ModRegistries.CUSTOM_ARROW.get()) == 80);
            context.runOnClient(client -> {
                if (menu(client).getSlot(0).getItem().getCount() != 22)
                    throw new AssertionError("Shift-craft continued after coating ran out");
            });
            context.takeScreenshot("fletching-eight-potions-sixty-four-arrows");
            context.input().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);
            context.waitForScreen(null);

            verifyRenderPaths(context, combined);
            context.setScreen(() -> new ArrowGallery(combined));
            context.takeScreenshot("fletching-combined-arrow-gallery");
            context.setScreen(() -> null);
        }
    }

    @ClientTest
    public static void zResourceReloadKeepsCoatingTextures(ClientTestContext context) {
        var reload = context.computeOnClient(client -> client.reloadResourcePacks());
        context.waitFor("resource reload", client -> reload.isDone() && client.getOverlay() == null, 1200);
        reload.join();
        alphaChannelColorsAndExistingArrowsRender(context);
    }

    private static void assertTextures(net.minecraft.client.resources.model.BakedModel model) {
        var quads = model.getQuads(null, null, net.minecraft.util.RandomSource.create());
        if (quads.isEmpty()) throw new AssertionError("Missing arrow geometry");
        for (var quad : quads)
            if (quad.getSprite()
                    .contents()
                    .name()
                    .equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()))
                throw new AssertionError("Missing texture in coated model");
    }

    private static FletchingMenu menu(net.minecraft.client.Minecraft client) {
        return (FletchingMenu) client.player.containerMenu;
    }

    private static void open(ClientTestContext context, BlockPos pos) {
        context.runOnClient(client -> client.gameMode.useItemOn(
                client.player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(pos.getCenter(), Direction.UP, pos, false)));
        context.waitForScreen(FletchingScreen.class);
    }

    private static void clickSlot(ClientTestContext context, int index, boolean shift, int button) {
        double[] xy = context.computeOnClient(client -> {
            var screen = (FletchingScreen) client.screen;
            var slot = menu(client).getSlot(index);
            double scale = client.getWindow().getGuiScale();
            return new double[] {(screen.getGuiLeft() + slot.x + 8) * scale, (screen.getGuiTop() + slot.y + 8) * scale};
        });
        context.input().setCursorPos(xy[0], xy[1]);
        if (shift) context.input().holdShift();
        try {
            context.input().pressMouse(button);
        } finally {
            if (shift) context.input().releaseShift();
        }
    }

    private static void pointAtTank(ClientTestContext context) {
        double[] xy = context.computeOnClient(client -> {
            var screen = (FletchingScreen) client.screen;
            double scale = client.getWindow().getGuiScale();
            return new double[] {(screen.getGuiLeft() + 77) * scale, (screen.getGuiTop() + 30) * scale};
        });
        context.input().setCursorPos(xy[0], xy[1]);
    }

    private static void restockPotion(ClientTestContext context, IntegratedWorldContext world) {
        world.server().runOnServer(server -> {
            var player = world.connection().serverPlayer();
            player.getInventory().setItem(13, potion(Items.POTION, 0xff8bafe0));
            player.containerMenu.broadcastChanges();
        });
        world.connection().waitForClientboundPackets();
        context.waitFor(
                "potion restocked",
                client -> PotionTank.isPotion(menu(client).getSlot(9).getItem()));
    }

    private static void verifyRenderPaths(ClientTestContext context, ItemStack combined) {
        context.runOnClient(client -> {
            var tag = combined.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag();
            var fallback =
                    client.getItemRenderer().getModel(new ItemStack(Items.ARROW), client.level, client.player, 0);
            for (String type : List.of("ARROW", "BOW", "CROSSBOW"))
                for (int stage = 0; stage < 3; stage++) {
                    var model = LayeredBakedModelFactory.buildLayeredItemModel(
                            fallback,
                            type,
                            stage,
                            "phantom_membrane",
                            "end_rod",
                            "heavy_core",
                            "tnt",
                            tag.getString("potionName"));
                    if (!model.getClass().getName().contains("CoatedModel"))
                        throw new AssertionError("Uncoated " + type + " stage " + stage);
                    assertTextures(model);
                    if (model.getQuads(null, null, net.minecraft.util.RandomSource.create())
                            .isEmpty()) throw new AssertionError("Missing arrow geometry");
                }
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(combined.copyWithCount(1)));
            if (!client.getItemRenderer()
                    .getModel(crossbow, client.level, client.player, 0)
                    .getClass()
                    .getName()
                    .contains("CoatedModel")) throw new AssertionError("Charged crossbow lacks coating");
            var arrow = new CustomArrowEntity(ModRegistries.CUSTOM_ARROW_ENTITY.get(), client.level);
            arrow.setCustomProperties(tag);
            var renderer = client.getEntityRenderDispatcher().getRenderer(arrow);
            var texture = renderer.getTextureLocation(arrow);
            if (!texture.toString().contains("bertie_coating"))
                throw new AssertionError("Projectile lacks potion pixels: " + texture);
        });
    }

    private static final class ArrowGallery extends Screen {
        private final ItemStack combined;

        ArrowGallery(ItemStack combined) {
            super(Component.literal("Fletching visuals"));
            this.combined = combined;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xff242a30);
            graphics.drawString(font, "Effect arrow + potion pixels", 20, 20, 0xffffffff);
            graphics.pose().pushPose();
            graphics.pose().translate(30, 50, 0);
            graphics.pose().scale(6, 6, 6);
            graphics.renderItem(combined, 0, 0);
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(combined.copyWithCount(1)));
            graphics.renderItem(crossbow, 24, 0);
            graphics.pose().popPose();
        }
    }
}
