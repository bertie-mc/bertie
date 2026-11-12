package io.github.bertie_mc.bertieprogression.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression_warden_fix")
@PrefixGameTestTemplate(false)
public final class WardenAcquisitionGameTests {
    @GameTest(template = "empty")
    public static void playerKillsRetainCarapaceButNeverDropBoneShard(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int looting : new int[] {0, 3}) {
            var pos = helper.absolutePos(new BlockPos(2 + looting * 4, 2, 2));
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "warden_" + looting));
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            var sword = new ItemStack(Items.DIAMOND_SWORD);
            if (looting > 0)
                sword.enchant(
                        level.registryAccess()
                                .lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.LOOTING),
                        looting);
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            var warden = EntityType.WARDEN.create(level);
            warden.setNoAi(true);
            warden.moveTo(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(warden);
            var damage = level.damageSources().playerAttack(player);
            warden.hurt(damage, 1);
            warden.die(damage);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3));
            helper.assertTrue(
                    drops.stream()
                            .anyMatch(e -> e.getItem()
                                    .is(item("deeperdarker:warden_carapace").getItem())),
                    "The existing Carapace drop must still execute");
            helper.assertTrue(
                    drops.stream()
                            .noneMatch(e -> e.getItem()
                                    .is(item("l2complements:warden_bone_shard").getItem())),
                    "A player kill must not add Bone Shards, including with Looting");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeBoneShardTooltipLosesOnlyItsObsoleteAcquisitionHint(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bone_tooltip"));
        var context = Item.TooltipContext.of(helper.getLevel());
        var bone = item("l2complements:warden_bone_shard").getTooltipLines(context, player, TooltipFlag.NORMAL);
        helper.assertTrue(
                bone.stream().noneMatch(c -> contains(c, "l2complements.tooltip.misc.player_kill")),
                "The obsolete native Warden drop hint must disappear");
        var guardian = item("l2complements:guardian_eye").getTooltipLines(context, player, TooltipFlag.NORMAL);
        helper.assertTrue(
                guardian.stream().anyMatch(c -> contains(c, "l2complements.tooltip.misc.strike_kill")),
                "Another material's native acquisition hint must remain");
        helper.succeed();
    }

    private static boolean contains(Component value, String key) {
        if (value.getContents() instanceof TranslatableContents text
                && text.getKey().equals(key)) return true;
        return value.getSiblings().stream().anyMatch(c -> contains(c, key));
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }
}
