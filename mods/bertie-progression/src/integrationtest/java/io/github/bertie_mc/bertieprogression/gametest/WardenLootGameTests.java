package io.github.bertie_mc.bertieprogression.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class WardenLootGameTests {
    @GameTest(template = "empty", batch = "warden_loot")
    public static void wardenLootUsesTheVanillaLootingRange(GameTestHelper helper) {
        var level = helper.getLevel();
        var carapace = BuiltInRegistries.ITEM.get(ResourceLocation.parse("deeperdarker:warden_carapace"));
        helper.assertTrue(carapace != Items.AIR, "Deeper and Darker must load in this fixture");
        var warden = EntityType.WARDEN.create(level);
        if (warden == null) {
            helper.fail("Warden must register");
            return;
        }
        var player = FakePlayerFactory.get(
                level, new GameProfile(UUID.fromString("191b92b8-7847-4395-b59c-58c7d31dfe0d"), "warden_loot_tester"));
        var looting =
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOOTING);
        var table = level.getServer()
                .reloadableRegistries()
                .getLootTable(
                        ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse("minecraft:entities/warden")));
        for (int n : new int[] {0, 1, 3}) {
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            if (n > 0) sword.enchant(looting, n);
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            var params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, warden)
                    .withParameter(LootContextParams.ORIGIN, warden.position())
                    .withParameter(
                            LootContextParams.DAMAGE_SOURCE,
                            level.damageSources().playerAttack(player))
                    .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                    .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                    .create(LootContextParamSets.ENTITY);
            int minimum = Integer.MAX_VALUE;
            int maximum = 0;
            var random = RandomSource.create(0x492BEC4L);
            for (int sample = 0; sample < 96; sample++) {
                var drops = table.getRandomItems(params, random);
                int count = drops.stream()
                        .filter(stack -> stack.is(carapace))
                        .mapToInt(ItemStack::getCount)
                        .sum();
                helper.assertTrue(count >= 1 && count <= n + 1, "Carapace count must be in 1..Looting+1");
                helper.assertTrue(
                        drops.stream().anyMatch(stack -> stack.is(Items.SCULK_CATALYST)), "Sculk Catalyst must remain");
                helper.assertTrue(
                        drops.stream().noneMatch(stack -> {
                            String id = BuiltInRegistries.ITEM
                                    .getKey(stack.getItem())
                                    .toString();
                            return id.equals("deeperdarker:heart_of_the_deep")
                                    || id.equals("ancient_forgemastery:primordial_soulstone");
                        }),
                        "Heart and Soulstone must never drop");
                minimum = Math.min(minimum, count);
                maximum = Math.max(maximum, count);
            }
            helper.assertTrue(
                    minimum == 1 && maximum == n + 1,
                    "Expected both bounds at Looting " + n + ", observed " + minimum + "–" + maximum);
        }
        helper.succeed();
    }

    private WardenLootGameTests() {}
}
