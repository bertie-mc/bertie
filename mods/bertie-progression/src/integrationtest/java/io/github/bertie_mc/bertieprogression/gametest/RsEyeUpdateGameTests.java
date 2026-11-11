package io.github.bertie_mc.bertieprogression.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.upgrade.UpgradeItem;
import com.refinedmods.refinedstorage.common.api.upgrade.UpgradeState;
import com.refinedmods.refinedstorage.common.constructordestructor.BlockBreakDestructorStrategyFactory;
import com.refinedmods.refinedstorage.common.upgrade.UpgradeDestinations;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import io.github.bertie_mc.bertieprogression.compat.RefinedFortune;
import io.github.bertie_mc.bertieprogression.ender.EyeAcquisitions;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression_rs_eyes")
@PrefixGameTestTemplate(false)
public final class RsEyeUpdateGameTests {
    @GameTest(template = "empty")
    public static void enchantedIngredientKeepsExactLevelAcrossNetworkAndAllowsOtherEnchantments(
            GameTestHelper helper) {
        var level = helper.getLevel();
        var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        Ingredient ingredient = Ingredient.CODEC
                .parse(
                        ops,
                        JsonParser.parseString(
                                "{\"type\":\"bertieprogression:enchanted_book\",\"enchantment\":\"minecraft:fortune\",\"level\":4}"))
                .getOrThrow();
        var fortune =
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        ItemStack correct = new ItemStack(Items.ENCHANTED_BOOK);
        var stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(fortune, 4);
        stored.set(silk, 1);
        correct.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        helper.assertTrue(ingredient.test(correct), "Additional book enchantments must remain valid");
        ItemStack wrong = correct.copy();
        var wrongEnchantments = new ItemEnchantments.Mutable(correct.get(DataComponents.STORED_ENCHANTMENTS));
        wrongEnchantments.set(fortune, 3);
        wrong.set(DataComponents.STORED_ENCHANTMENTS, wrongEnchantments.toImmutable());
        helper.assertTrue(
                !ingredient.test(wrong) && !ingredient.test(new ItemStack(Items.ENCHANTED_BOOK)),
                "Wrong-level and unenchanted books must fail");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            Ingredient decoded = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            helper.assertTrue(
                    decoded.test(correct) && !decoded.test(wrong), "Network round-trip must preserve matching");
            helper.assertTrue(
                    decoded.getItems()[0]
                                    .get(DataComponents.STORED_ENCHANTMENTS)
                                    .getLevel(fortune)
                            == 4,
                    "Recipe viewers must show the required book level");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void speedCraftAcceptsOnlySwiftnessAndConsumesTheBottle(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = (ShapedRecipe) level.getRecipeManager()
                .byKey(ResourceLocation.parse("refinedstorage:speed_upgrade"))
                .orElseThrow()
                .value();
        ItemStack potion = new ItemStack(Items.POTION);
        potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.SWIFTNESS));
        var stacks = new ArrayList<>(List.of(
                stack("refinedstorage:quartz_enriched_iron"),
                potion,
                stack("refinedstorage:quartz_enriched_iron"),
                stack("deeperdarker:lite"),
                stack("refinedstorage:upgrade"),
                stack("deeperdarker:lite"),
                stack("refinedstorage:quartz_enriched_iron"),
                stack("refinedstorage:quartz_enriched_iron"),
                stack("refinedstorage:quartz_enriched_iron")));
        CraftingInput input = CraftingInput.of(3, 3, stacks);
        helper.assertTrue(recipe.matches(input, level), "Swiftness must match the real crafting recipe");
        helper.assertTrue(
                recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),
                "The potion bottle must be consumed");
        potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.STRENGTH));
        helper.assertTrue(!recipe.matches(CraftingInput.of(3, 3, stacks), level), "Other potion effects must fail");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fortuneEightReachesNativeDestructorAndSilkRetainsPriority(GameTestHelper helper)
            throws Exception {
        var high = RefinedFortune.UPGRADES.get(8).get();
        helper.assertTrue(
                RefinedStorageApi.INSTANCE.getUpgradeRegistry().getByUpgradeItem(high).stream()
                        .anyMatch(mapping -> mapping.destination() == UpgradeDestinations.DESTRUCTOR),
                "Fortune VIII must be installable in the native Destructor");
        for (boolean silk : List.of(false, true)) {
            UpgradeState state = new UpgradeState() {
                @Override
                public long getRegulatedAmount(ResourceKey resource) {
                    return 0;
                }

                @Override
                public int getAmount(UpgradeItem item) {
                    return item == high
                                    || silk
                                            && item
                                                    == com.refinedmods.refinedstorage.common.content.Items.INSTANCE
                                                            .getSilkTouchUpgrade()
                            ? 1
                            : 0;
                }
            };
            var strategy = new BlockBreakDestructorStrategyFactory()
                    .create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), Direction.DOWN, state, false)
                    .orElseThrow();
            var field = strategy.getClass().getDeclaredField("tool");
            field.setAccessible(true);
            ItemStack tool = (ItemStack) field.get(strategy);
            var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            helper.assertTrue(
                    EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.FORTUNE), tool)
                            == (silk ? 0 : 8),
                    "Native mining must use Fortune VIII unless Silk Touch is installed");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wirelessCollisionConsumesOnlyFeedstockAndAnvilWithExactTierAndSpeed(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var quantum = block("mekanism:quantum_entangloporter");
        var machine = block("anvilcraft:space_overcompressor");
        for (int test = 0; test < 3; test++) {
            level.setBlockAndUpdate(pos, quantum.defaultBlockState());
            level.setBlockAndUpdate(pos.below(), machine.defaultBlockState());
            var anvil = FallingBlockEntity.fall(
                    level,
                    pos.above(3),
                    block(test == 0 ? "anvilcraft:royal_anvil" : "anvilcraft:ember_anvil")
                            .defaultBlockState());
            NeoForge.EVENT_BUS.post(new AnvilEvent.CollisionBlock(level, pos, anvil, test == 1 ? 63.99 : 64));
            helper.assertTrue(level.getBlockState(pos.below()).is(machine), "The processing block must remain");
            helper.assertTrue(
                    level.getBlockState(pos).isAir() == (test == 2) && anvil.isAlive() == (test != 2),
                    "Only T2 at 64 m/t may consume the feedstock and anvil");
            anvil.discard();
        }
        helper.assertTrue(
                drops(level, pos, "refinedstorage:wireless_transmitter") == 1, "Exactly one transmitter must result");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dragonsteelPairStillConsumesSupportAndRetainsAnvil(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, block("iceandfire:dragonsteel_fire_block").defaultBlockState());
        level.setBlockAndUpdate(
                pos.below(), block("anvilcraft:ember_metal_block").defaultBlockState());
        var anvil = FallingBlockEntity.fall(level, pos.above(3), Blocks.ANVIL.defaultBlockState());
        NeoForge.EVENT_BUS.post(new AnvilEvent.CollisionBlock(level, pos, anvil, 32));
        helper.assertTrue(
                level.isEmptyBlock(pos) && level.isEmptyBlock(pos.below()) && anvil.isAlive(),
                "The existing two-block Dragonsteel contract must remain");
        helper.assertTrue(
                drops(level, pos, "iceandfire:dragonsteel_fire_ingot") == 8, "The existing yield must remain eight");
        anvil.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 380)
    public static void adultPiglinConversionGuaranteesCursedEyeDrop(GameTestHelper helper) {
        piglin(helper, false);
    }

    @GameTest(template = "empty", timeoutTicks = 380)
    public static void babyPiglinConversionGuaranteesCursedEyeDrop(GameTestHelper helper) {
        piglin(helper, true);
    }

    private static void piglin(GameTestHelper helper, boolean baby) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var piglin = EntityType.PIGLIN.create(level);
        piglin.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        piglin.setBaby(baby);
        piglin.setPersistenceRequired();
        piglin.setNoGravity(true);
        piglin.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        level.addFreshEntity(piglin);
        helper.runAtTickTime(280, () -> {
            piglin.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ENDER_EYE));
            piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, 120);
        });
        helper.runAtTickTime(325, () -> {
            var converted = level.getEntitiesOfClass(ZombifiedPiglin.class, new AABB(pos).inflate(4));
            helper.assertTrue(
                    converted.size() == 1 && converted.getFirst().isBaby() == baby,
                    "Native zombification must complete");
            var zombie = converted.getFirst();
            helper.assertTrue(
                    zombie.getOffhandItem().is(stack("endrem:cursed_eye").getItem()), "The held eye must transform");
            zombie.hurt(level.damageSources().generic(), 1000);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(drops(level, pos, "endrem:cursed_eye") == 1, "The transformed eye must always drop");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void crypticRewardUsesRequiredLevelBoundary(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), profile("cryptic"));
        player.getInventory().clearContent();
        player.getRandom().setSeed(seed(0.03));
        EyeAcquisitions.enchanted(player, 79);
        helper.assertTrue(
                player.getInventory().countItem(stack("endrem:cryptic_eye").getItem()) == 0,
                "A lower-cost enchant must never roll a Cryptic Eye");
        EyeAcquisitions.enchanted(player, 80);
        helper.assertTrue(
                player.getInventory().countItem(stack("endrem:cryptic_eye").getItem()) == 1,
                "An eligible successful roll must award the eye");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyTropicalFishFeedingCanProduceAnExoticEye(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var dolphin = EntityType.DOLPHIN.create(level);
        dolphin.moveTo(pos.getX(), pos.getY(), pos.getZ());
        level.addFreshEntity(dolphin);
        var player = FakePlayerFactory.get(level, profile("dolphin"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD));
        dolphin.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(drops(level, pos, "endrem:exotic_eye") == 0, "Other fish must not award an eye");
        for (int n = 0; n < 64 && drops(level, pos, "endrem:exotic_eye") == 0; n++) {
            dolphin.getRandom().setSeed(seed(0.20) + n);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TROPICAL_FISH, 2));
            dolphin.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "Successful feeding must consume one fish");
        }
        helper.assertTrue(
                drops(level, pos, "endrem:exotic_eye") > 0, "The native feeding path must award an Exotic Eye");
        dolphin.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void replacementChestRewardsRunOnceAlongsideNativeLootInjection(GameTestHelper helper) {
        var level = helper.getLevel();
        var params = new LootParams.Builder(level)
                .withParameter(
                        LootContextParams.ORIGIN,
                        helper.absolutePos(BlockPos.ZERO).getCenter())
                .create(LootContextParamSets.CHEST);
        String[][] cases = {
            {"minecraft:chests/trial_chambers/reward_ominous", "trial_eye"},
            {"minecraft:chests/igloo_chest", "cold_eye"},
            {"minecraft:chests/buried_treasure", "exotic_eye"},
            {"minecraft:chests/abandoned_mineshaft", "lost_eye"}
        };
        for (String[] entry : cases) {
            var table = level.getServer()
                    .reloadableRegistries()
                    .getLootTable(net.minecraft.resources.ResourceKey.create(
                            Registries.LOOT_TABLE, ResourceLocation.parse(entry[0])));
            int obtained = 0;
            for (int n = 0; n < 2000; n++) {
                var loot = table.getRandomItems(params, RandomSource.create(n * 7919L));
                long count = loot.stream()
                        .filter(s -> s.is(stack("endrem:" + entry[1]).getItem()))
                        .mapToLong(ItemStack::getCount)
                        .sum();
                helper.assertTrue(count <= 1, "A reward generation must not stack old and new eye sources");
                obtained += (int) count;
            }
            helper.assertTrue(obtained > 0, "The replacement source must execute for " + entry[0]);
        }
        helper.succeed();
    }

    private static long seed(double chance) {
        for (long n = 0; ; n++) if (RandomSource.create(n).nextDouble() < chance) return n;
    }

    private static GameProfile profile(String name) {
        return new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), "eye_" + name);
    }

    private static net.minecraft.world.level.block.Block block(String name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
    }

    private static ItemStack stack(String name) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(name)));
    }

    private static long drops(ServerLevel level, BlockPos pos, String name) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).stream()
                .filter(entity -> entity.getItem().is(stack(name).getItem()))
                .mapToLong(entity -> entity.getItem().getCount())
                .sum();
    }
}
