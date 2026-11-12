package io.github.bertie_mc.bertieprogression.gametest;

import com.mojang.authlib.GameProfile;
import io.github.bertie_mc.bertieprogression.anvil.OverchargedCrystalCrafting;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression_tech_042")
@PrefixGameTestTemplate(false)
public final class TechChallengeGameTests {
    @GameTest(template = "empty")
    public static void successfulPaidSpellAtExactlyZeroConvertsOffhandEye(GameTestHelper helper) {
        var player = player(helper, "exact");
        var spell = SpellRegistry.FIREBOLT_SPELL.get();
        MagicData.getPlayerMagicData(player).setMana(spell.getManaCost(1));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.ENDER_EYE));
        spell.castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, false);
        helper.assertTrue(
                MagicData.getPlayerMagicData(player).getMana() == 0, "The native payment must leave exact zero");
        helper.assertTrue(
                player.getOffhandItem().is(item("endrem:magical_eye").getItem()),
                "The successful native cast must award the Magical Eye");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fractionalRemainderAndAlreadyEmptyManaDoNotQualify(GameTestHelper helper) {
        var player = player(helper, "fraction");
        var spell = SpellRegistry.FIREBOLT_SPELL.get();
        var data = MagicData.getPlayerMagicData(player);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.ENDER_EYE));
        data.setMana(spell.getManaCost(1) + 0.25F);
        spell.castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, false);
        helper.assertTrue(player.getOffhandItem().is(Items.ENDER_EYE), "A rounded zero display must not qualify");
        data.setMana(0);
        spell.castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, false);
        helper.assertTrue(player.getOffhandItem().is(Items.ENDER_EYE), "An already empty mana pool must not qualify");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullInventoryPreservesRemainingEyeAndDropsOneReward(GameTestHelper helper) {
        var player = player(helper, "full");
        for (int i = 0; i < player.getInventory().items.size(); i++)
            player.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.ENDER_EYE, 2));
        var spell = SpellRegistry.FIREBOLT_SPELL.get();
        MagicData.getPlayerMagicData(player).setMana(spell.getManaCost(1));
        spell.castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, false);
        helper.assertTrue(
                player.getOffhandItem().is(Items.ENDER_EYE)
                        && player.getOffhandItem().getCount() == 1,
                "Exactly one input eye must be consumed");
        long rewards =
                helper
                        .getLevel()
                        .getEntitiesOfClass(
                                ItemEntity.class, player.getBoundingBox().inflate(4))
                        .stream()
                        .filter(entity ->
                                entity.getItem().is(item("endrem:magical_eye").getItem()))
                        .mapToLong(entity -> entity.getItem().getCount())
                        .sum();
        helper.assertTrue(rewards == 1, "A full inventory must drop exactly one reward");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void crystalStructureIsAtomicAndDropsWithoutOvercompressor(GameTestHelper helper) {
        var level = helper.getLevel();
        var table = helper.absolutePos(new BlockPos(3, 5, 3));
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                level.setBlockAndUpdate(table.offset(x, 0, z), Blocks.CRAFTING_TABLE.defaultBlockState());
        var corner = table.offset(-1, -3, -1);
        for (int y = 0; y < 3; y++)
            for (int z = 0; z < 3; z++)
                for (int x = 0; x < 3; x++) {
                    String id = x == 1 && y == 1 && z == 1
                            ? "cognition:cognitive_crystal_block"
                            : x != 1 && y != 1 && z != 1
                                    ? "bertieprogression:mana_charged_fluorite_block"
                                    : "oritech:superconductor";
                    level.setBlockAndUpdate(
                            corner.offset(x, y, z),
                            BuiltInRegistries.BLOCK
                                    .get(ResourceLocation.parse(id))
                                    .defaultBlockState());
                }
        level.setBlockAndUpdate(corner, Blocks.STONE.defaultBlockState());
        helper.assertTrue(
                !OverchargedCrystalCrafting.tryCraft(level, table), "A wrong corner must reject the whole structure");
        helper.assertTrue(!level.isEmptyBlock(corner.offset(1, 1, 1)), "Rejection must consume nothing");
        level.setBlockAndUpdate(
                corner,
                BuiltInRegistries.BLOCK
                        .get(ResourceLocation.parse("bertieprogression:mana_charged_fluorite_block"))
                        .defaultBlockState());
        helper.assertTrue(
                OverchargedCrystalCrafting.tryCraft(level, table),
                "Regular Crafting Tables must support a direct item result");
        helper.assertTrue(
                !OverchargedCrystalCrafting.tryCraft(level, table), "The consumed structure must not yield twice");
        for (int y = 0; y < 3; y++)
            for (int z = 0; z < 3; z++)
                for (int x = 0; x < 3; x++)
                    helper.assertTrue(
                            level.isEmptyBlock(corner.offset(x, y, z)), "All 27 ingredients must be consumed");
        long crystals = level.getEntitiesOfClass(ItemEntity.class, new AABB(table).inflate(5)).stream()
                .filter(entity ->
                        entity.getItem().is(item("oritech:overcharged_crystal").getItem()))
                .mapToLong(entity -> entity.getItem().getCount())
                .sum();
        helper.assertTrue(crystals == 1, "The structure must drop one crystal item");
        helper.assertTrue(level.getBlockState(table).is(Blocks.CRAFTING_TABLE), "The crafting-table layer must remain");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "magic_" + name));
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return player;
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }
}
