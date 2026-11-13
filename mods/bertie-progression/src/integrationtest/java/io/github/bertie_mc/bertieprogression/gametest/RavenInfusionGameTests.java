package io.github.bertie_mc.bertieprogression.gametest;

import com.mojang.authlib.GameProfile;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression_raven_044")
@PrefixGameTestTemplate(false)
public final class RavenInfusionGameTests {
    @GameTest(template = "empty")
    public static void ravenInfusionMatchesItsSetupAndChargesExactly200Levels(GameTestHelper helper) throws Exception {
        var player = player(helper, "raven");
        var input = item("apothic_enchanting:apothic_enchanting_table");
        var recipe = helper.getLevel()
                .getRecipeManager()
                .byKey(ResourceLocation.parse("bertieprogression:infusion/raven_enchanting_table"))
                .orElseThrow()
                .value();
        var matches = recipe.getClass().getMethod("matches", ItemStack.class, float.class, float.class, float.class);
        helper.assertTrue((boolean) matches.invoke(recipe, input, 99F, 99.5F, 1.25F), "Exact shelf setup must match");
        helper.assertTrue(
                (boolean) matches.invoke(recipe, input, Math.nextUp(99F), Math.nextDown(99.5F), 1.25F),
                "Float summation order must not prevent the displayed setup");
        helper.assertTrue(
                !(boolean) matches.invoke(recipe, input, 99F, 99.6F, 1.25F), "A different Quanta setup must fail");
        var menu = menu(helper, player, input, 99, 99.5F, 1.25F);
        helper.assertTrue(menu.costs[2] == 200, "The displayed and required level must be 200");
        player.experienceLevel = 199;
        helper.assertTrue(!menu.clickMenuButton(player, 2), "199 levels must not pay for Raven Table");
        helper.assertTrue(
                player.experienceLevel == 199 && menu.getSlot(0).getItem().is(input.getItem()),
                "Rejected infusion must preserve levels and input");
        player.experienceLevel = 250;
        player.experienceProgress = 0.375F;
        helper.assertTrue(menu.clickMenuButton(player, 2), "Paid infusion must succeed");
        helper.assertTrue(
                menu.getSlot(0)
                        .getItem()
                        .is(item("apothic_enchanting:raven_enchanting_table").getItem()),
                "Native infusion must produce Raven Table");
        helper.assertTrue(
                player.experienceLevel == 50 && player.experienceProgress == 0.375F,
                "Payment must consume 200 actual levels and preserve fractional progress");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void otherInfusionsKeepTheirNativeExperienceCost(GameTestHelper helper) throws Exception {
        var player = player(helper, "ordinary");
        var baseline = player(helper, "baseline");
        player.experienceLevel = baseline.experienceLevel = 250;
        player.experienceProgress = baseline.experienceProgress = 0.375F;
        var menu = menu(helper, player, new ItemStack(Items.DRAGON_BREATH), 80, 20, 60);
        helper.assertTrue(menu.costs[2] == 80, "Other infusions must retain their level requirement");
        var misc = Class.forName("dev.shadowsoffire.apothic_enchanting.util.MiscUtil");
        int points =
                (int) misc.getMethod("getExpCostForSlot", int.class, int.class).invoke(null, 80, 2);
        Class.forName("dev.shadowsoffire.placebo.util.EnchantmentUtils")
                .getMethod("chargeExperience", net.minecraft.world.entity.player.Player.class, int.class)
                .invoke(null, baseline, points);
        helper.assertTrue(menu.clickMenuButton(player, 2), "Ordinary infusion must succeed");
        helper.assertTrue(
                menu.getSlot(0)
                        .getItem()
                        .is(item("apothic_enchanting:infused_breath").getItem()),
                "Native ordinary infusion output must remain");
        helper.assertTrue(
                player.experienceLevel == baseline.experienceLevel
                        && Math.abs(player.experienceProgress - baseline.experienceProgress) < 0.0001F,
                "Ordinary infusion must use the unchanged native payment");
        helper.succeed();
    }

    private static EnchantmentMenu menu(
            GameTestHelper helper, ServerPlayer player, ItemStack input, int eterna, float quanta, float arcana)
            throws Exception {
        var type = Class.forName("dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu");
        var menu = (EnchantmentMenu) type.getConstructor(int.class, Inventory.class, BlockPos.class)
                .newInstance(1, player.getInventory(), helper.absolutePos(new BlockPos(2, 2, 2)));
        menu.getSlot(0).set(input.copy());
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
        var access = EnchantmentMenu.class.getDeclaredField("access");
        access.setAccessible(true);
        access.set(menu, ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2))));
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        menu.costs[2] = eterna;
        menu.enchantClue[2] = registry.getId(registry.get(ResourceLocation.parse("apothic_enchanting:infusion")));
        var stats = Class.forName("dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableStats");
        var value = stats.getConstructor(
                        float.class, float.class, float.class, int.class, Set.class, boolean.class, boolean.class)
                .newInstance((float) eterna, quanta, arcana, 1, Set.of(), false, false);
        type.getMethod("setStats", stats).invoke(menu, value);
        return menu;
    }

    private static ServerPlayer player(GameTestHelper helper, String suffix) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "raven_" + suffix));
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        return player;
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }
}
