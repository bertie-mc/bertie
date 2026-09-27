package io.github.bertie_mc.enchantingrerollcost.clienttest;

import com.kasch_x.easyapothcompat.EasyApothConfig;
import com.kasch_x.easyapothcompat.client.RerollButton;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apothic_enchanting.table.RavenTableStats;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import io.github.bertie_mc.enchantingrerollcost.RerollPrice;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import io.github.bertie_mc.testing.client.context.IntegratedWorldContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class RerollClientTests {
    private RerollClientTests() {}

    @ClientTest
    public static void pricesAndPayments(ClientTestContext context) throws Exception {
        int oldLapis = EasyApothConfig.REROLL_LAPIS_COST.get();
        int oldXp = EasyApothConfig.REROLL_XP_COST.get();
        boolean oldLevels = EasyApothConfig.REROLL_TAKES_LEVELS.get();
        List<String> passed = new ArrayList<>();
        try (var world = context.worldBuilder().create()) {
            context.waitFor("player chunk", mc -> mc.player != null && mc.level.hasChunkAt(mc.player.blockPosition()));
            // The dynamic price must supersede all three legacy cost settings.
            EasyApothConfig.REROLL_LAPIS_COST.set(64);
            EasyApothConfig.REROLL_XP_COST.set(10000);
            EasyApothConfig.REROLL_TAKES_LEVELS.set(true);
            for (String table : List.of(
                    "minecraft:enchanting_table",
                    "apothic_enchanting:apothic_enchanting_table",
                    "apothic_enchanting:raven_enchanting_table")) {
                for (int power : new int[] {0, 30, 100}) {
                    int expected = power == 0 ? 6 : power == 30 ? 16 : 56;
                    openTable(context, world, table, power, 100);
                    checkPayment(context, world, expected, false, 2000, 16, true);
                    checkPayment(context, world, expected, false, 2000, 16, true);
                    checkPayment(context, world, expected, false, expected, 1, true);
                    checkPayment(context, world, expected, false, expected - 1, 1, false);
                    checkPayment(context, world, expected, false, 2000, 0, false);
                    checkPayment(context, world, expected, true, 0, 0, true);
                    closeTable(context, world);
                    passed.add(table + " power=" + power + " cost=" + expected + ": PASS");
                }
                openTable(context, world, table, 100, 30);
                checkPayment(context, world, 16, false, 2000, 16, true);
                closeTable(context, world);
                passed.add(table + " power=100 playerCap=30 cost=16: PASS");
            }
            Files.write(Path.of("reroll-cost-observations.txt"), passed);
            System.out.println("REROLL_COST_CHECKS_COMPLETE " + passed.size());
            // Stop the server before the driver's disconnect loop waits for its termination.
            world.server().runOnServer(server -> server.halt(false));
        } finally {
            EasyApothConfig.REROLL_LAPIS_COST.set(oldLapis);
            EasyApothConfig.REROLL_XP_COST.set(oldXp);
            EasyApothConfig.REROLL_TAKES_LEVELS.set(oldLevels);
        }
    }

    private static void openTable(
            ClientTestContext context, IntegratedWorldContext world, String table, int power, int cap) {
        BlockPos pos = world.server().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.getAttribute(Ench.Attributes.MAX_ETERNA).setBaseValue(cap);
            var level = player.serverLevel();
            var at = player.blockPosition().offset(0, 0, 3);
            for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS)
                level.setBlockAndUpdate(at.offset(offset), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(at.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(
                    at,
                    BuiltInRegistries.BLOCK.get(ResourceLocation.parse(table)).defaultBlockState());
            if (table.contains("raven")) {
                level.getBlockEntity(at).getData(RavenTableStats.TYPE).set(power, 0, 0);
            } else {
                var shelf = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(
                        power == 100 ? "apothic_enchanting:draconic_endshelf" : "apothic_enchanting:basic_bookshelf"));
                int count = power == 100 ? 5 : power / 2;
                for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
                    if (count-- <= 0) break;
                    level.setBlockAndUpdate(at.offset(offset), shelf.defaultBlockState());
                }
            }
            return at;
        });
        world.connection().waitForClientboundPackets();
        context.runOnClient(mc -> mc.gameMode.useItemOn(
                mc.player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false)));
        context.waitFor(
                "Apothic screen", mc -> mc.player.containerMenu instanceof ApothEnchantmentMenu && mc.screen != null);
    }

    private static void checkPayment(
            ClientTestContext context,
            IntegratedWorldContext world,
            int expected,
            boolean creative,
            int xp,
            int lapis,
            boolean allowed) {
        int seed = world.server().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
            player.experienceLevel = 0;
            player.experienceProgress = 0;
            player.totalExperience = 0;
            player.giveExperiencePoints(xp);
            player.containerMenu.getSlot(0).set(new ItemStack(Items.BOOK));
            player.containerMenu
                    .getSlot(1)
                    .set(lapis == 0 ? ItemStack.EMPTY : new ItemStack(Items.LAPIS_LAZULI, lapis));
            player.containerMenu.broadcastChanges();
            check(
                    RerollPrice.experienceCost((ApothEnchantmentMenu) player.containerMenu, player) == expected,
                    "Server price differs from expected " + expected);
            return player.getEnchantmentSeed();
        });
        world.connection().waitForClientboundPackets();
        context.waitFor(
                "client price and balances",
                mc -> RerollPrice.experienceCost((ApothEnchantmentMenu) mc.player.containerMenu, mc.player) == expected
                        && EnchantmentUtils.getExperience(mc.player) == xp
                        && mc.player.containerMenu.getSlot(1).getItem().getCount() == lapis
                        && mc.player.getAbilities().instabuild == creative);
        context.runOnClient(mc -> {
            RerollButton button = button(mc);
            var graphics = new PriceGraphics(mc);
            button.render(graphics, 0, 0, 0);
            check(graphics.text.contains(Integer.toString(expected)), "Displayed XP price: " + graphics.text);
            check(graphics.text.contains("1"), "Displayed lapis price: " + graphics.text);
            check(button.active == allowed, "Wrong button affordability at XP=" + xp + ", lapis=" + lapis);
            // Send even a disabled button to verify the server rejects unaffordable packets.
            button.onPress();
        });
        world.connection().waitForServerboundPackets();
        world.connection().waitForClientboundPackets();
        world.server().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            check(
                    (player.getEnchantmentSeed() != seed) == allowed,
                    "Wrong seed change: expectedCost=" + expected + ", allowed=" + allowed + ", xp="
                            + EnchantmentUtils.getExperience(player) + ", lapis="
                            + player.containerMenu.getSlot(1).getItem().getCount());
            check(
                    EnchantmentUtils.getExperience(player) == xp - (allowed && !creative ? expected : 0),
                    "Wrong XP debit");
            check(
                    player.containerMenu.getSlot(1).getItem().getCount() == lapis - (allowed && !creative ? 1 : 0),
                    "Wrong lapis debit");
            check(player.containerMenu.getSlot(0).getItem().is(Items.BOOK), "Reroll consumed or enchanted the book");
        });
    }

    private static void closeTable(ClientTestContext context, IntegratedWorldContext world) {
        context.runOnClient(mc -> mc.player.closeContainer());
        context.waitForScreen(null);
        world.connection().waitForServerboundPackets();
    }

    private static RerollButton button(Minecraft mc) {
        return mc.screen.children().stream()
                .filter(RerollButton.class::isInstance)
                .map(RerollButton.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing reroll button"));
    }

    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }

    private static final class PriceGraphics extends GuiGraphics {
        final List<String> text = new ArrayList<>();

        PriceGraphics(Minecraft mc) {
            super(mc, mc.renderBuffers().bufferSource());
        }

        @Override
        public int drawString(Font font, Component component, int x, int y, int color, boolean shadow) {
            text.add(component.getString());
            return super.drawString(font, component, x, y, color, shadow);
        }
    }
}
