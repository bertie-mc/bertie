package io.github.bertie_mc.bertieprogression.gametest;

import com.mojang.authlib.GameProfile;
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
