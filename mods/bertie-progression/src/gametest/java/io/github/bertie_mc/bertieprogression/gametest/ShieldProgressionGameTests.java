package io.github.bertie_mc.bertieprogression.gametest;

import com.mojang.authlib.GameProfile;
import io.github.bertie_mc.bertieprogression.RemovedItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class ShieldProgressionGameTests {
    @GameTest(template = "empty", batch = "shield_components")
    public static void nativeShieldComponentsHaveRequestedStats(GameTestHelper h) {
        h.assertTrue(stack("mythsandlegends:stony_shield").getMaxDamage() == 880, "Stony Shield durability");
        amount(h, "mythsandlegends:stony_shield", Attributes.ARMOR, 4);
        amount(h, "mythsandlegends:stony_shield", Attributes.ARMOR_TOUGHNESS, 2);
        amount(h, "block_factorys_bosses:enhanced_shield", Attributes.ARMOR, 2);
        amount(h, "block_factorys_bosses:dragon_guard_shield", Attributes.ARMOR, 4);
        amount(h, "block_factorys_bosses:dragon_guard_shield", Attributes.ARMOR_TOUGHNESS, 2);
        amount(h, "block_factorys_bosses:dragon_guard_shield", Attributes.MOVEMENT_SPEED, .05);
        amount(h, "alexsmobs:shield_of_the_deep", Attributes.ARMOR, 4);
        amount(h, "piglinproliferation:buckler", Attributes.MOVEMENT_SPEED, .10);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "shield_water")
    public static void deepShieldBreathesAndReflectsOnlyWetAttackers(GameTestHelper h) throws Exception {
        FakePlayer player = player(h);
        player.setItemSlot(EquipmentSlot.OFFHAND, stack("alexsmobs:shield_of_the_deep"));
        player.startUsingItem(InteractionHand.OFF_HAND);
        var remaining = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("useItemRemaining");
        remaining.setAccessible(true);
        remaining.setInt(player, player.getUseItem().getUseDuration(player) - 8);
        h.assertTrue(player.isBlocking(), "the fixture must have finished raising its shield");
        player.setAirSupply(40);
        h.getLevel().tickNonPassenger(player);
        h.assertTrue(player.getAirSupply() > 40, "raising the shield must replenish air during entity ticking");
        var attacker = h.spawn(EntityType.PIG, new BlockPos(2, 2, 3));
        attacker.setNoAi(true);
        attacker.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        attacker.setHealth(100);
        attacker.getAttribute(BuiltInRegistries.ATTRIBUTE
                        .getHolder(ResourceLocation.parse("apothic_attributes:crit_chance"))
                        .orElseThrow())
                .setBaseValue(0);
        h.setBlock(new BlockPos(2, 2, 3), Blocks.WATER);
        attacker.baseTick();
        h.assertTrue(attacker.isInWaterOrRain(), "fixture attacker must be wet");
        player.setYRot(0);
        player.invulnerableTime = 0;
        player.hurt(h.getLevel().damageSources().mobAttack(attacker), 10);
        h.assertTrue(Math.abs(attacker.getHealth() - 97) < .01, "a ten-damage block must reflect three damage");
        attacker.setHealth(100);
        attacker.invulnerableTime = 0;
        h.setBlock(new BlockPos(2, 2, 3), Blocks.AIR);
        attacker.baseTick();
        h.assertTrue(!attacker.isInWaterOrRain(), "fixture attacker must now be dry");
        player.invulnerableTime = 0;
        player.hurt(h.getLevel().damageSources().mobAttack(attacker), 10);
        h.assertTrue(attacker.getHealth() == 100, "dry attackers must not receive aquatic reflection");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "shield_bash")
    public static void bucklerBashUsesFractionalWeaponDamage(GameTestHelper h) throws Exception {
        FakePlayer player = player(h);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8.75);
        player.setItemSlot(EquipmentSlot.OFFHAND, stack("piglinproliferation:buckler"));
        var target = h.spawn(EntityType.PIG, new BlockPos(2, 2, 3));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.setHealth(100);
        Class.forName("tallestred.piglinproliferation.common.items.BucklerItem")
                .getMethod("bucklerBash", net.minecraft.world.entity.LivingEntity.class)
                .invoke(null, player);
        h.assertTrue(
                Math.abs(target.getHealth() - 91.25) < .01, "bash must retain the complete fractional weapon damage");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "removed_creative")
    public static void lateQuarkCreativeEntriesStayRemoved(GameTestHelper h) {
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.REDSTONE_BLOCKS);
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.DEFAULT_FLAGS, true, h.getLevel().registryAccess()));
        h.assertTrue(
                tab.getDisplayItems().stream()
                        .noneMatch(stack -> BuiltInRegistries.ITEM
                                .getKey(stack.getItem())
                                .equals(ResourceLocation.parse("quark:iron_rod"))),
                "Quark's late tab additions must run before item suppression");
        h.assertTrue(
                RemovedItems.isRemoved(stack("quark:iron_rod")), "the registry item must remain recognized as removed");
        h.succeed();
    }

    private static ItemStack stack(String id) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
        if (stack.isEmpty()) throw new AssertionError("Missing test item: " + id);
        return stack;
    }

    private static void amount(
            GameTestHelper h,
            String id,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double expected) {
        double actual =
                stack(id)
                        .getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                        .modifiers()
                        .stream()
                        .filter(entry -> entry.attribute().equals(attribute))
                        .mapToDouble(entry -> entry.modifier().amount())
                        .sum();
        h.assertTrue(Math.abs(actual - expected) < .0001, id + " attribute: " + actual);
    }

    private static FakePlayer player(GameTestHelper h) throws Exception {
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ShieldTest")) {
            @Override
            public boolean isInvulnerableTo(DamageSource source) {
                return false;
            }
        };
        player.setGameMode(GameType.SURVIVAL);
        var grace = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
        grace.setAccessible(true);
        grace.setInt(player, 0);
        player.setPos(h.absoluteVec(new Vec3(2.5, 2, 2.5)));
        return player;
    }
}
