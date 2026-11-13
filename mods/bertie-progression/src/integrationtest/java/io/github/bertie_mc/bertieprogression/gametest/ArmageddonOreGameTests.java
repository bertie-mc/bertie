package io.github.bertie_mc.bertieprogression.gametest;

import com.mojang.authlib.GameProfile;
import com.refinedmods.refinedstorage.common.Platform;
import com.simibubi.create.foundation.utility.BlockHelper;
import io.github.bertie_mc.bertieprogression.ore.ArmageddonOreRules;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mekanism.common.item.gear.ItemAtomicDisassembler;
import mekanism.common.tags.MekanismTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class ArmageddonOreGameTests {
    private static final List<String> ORES = List.of("bloody_ore", "voiderite_ore", "elvenite_ore");

    private static BlockState state(String ore) {
        return BuiltInRegistries.BLOCK
                .get(ResourceLocation.fromNamespaceAndPath("armageddon_mod", ore))
                .defaultBlockState();
    }

    private static void grant(ServerPlayer player, ResourceLocation id) {
        var holder = player.server.getAdvancements().get(id);
        if (holder == null) throw new IllegalStateException("Missing native advancement " + id);
        holder.value()
                .criteria()
                .keySet()
                .forEach(criterion -> player.getAdvancements().award(holder, criterion));
    }

    static ServerPlayer player(GameTestHelper helper) {
        var level = helper.getLevel();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "ore_gate_test"), false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        player.connection =
                new ServerGamePacketListenerImpl(
                        level.getServer(), new Connection(PacketFlow.SERVERBOUND), player, cookie) {
                    @Override
                    public void send(Packet<?> packet) {}
                };
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void noPlayerAndFakePlayerCannotReceiveOrdinaryOrRawLoot(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var tool = new ItemStack(Items.NETHERITE_PICKAXE);
        for (String ore : ORES) {
            var state = state(ore);
            helper.assertTrue(!state.isAir(), "native ore must be registered: " + ore);
            helper.assertTrue(
                    Block.getDrops(state, level, pos, null, null, tool).isEmpty(),
                    "machine loot must be empty: " + ore);
            var fake = FakePlayerFactory.getMinecraft(level);
            helper.assertTrue(
                    Block.getDrops(state, level, pos, null, fake, tool).isEmpty(),
                    "fake-player loot must be empty: " + ore);
            var params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.BLOCK_STATE, state)
                    .withParameter(LootContextParams.TOOL, tool)
                    .create(LootContextParamSets.BLOCK);
            var table = level.getServer()
                    .reloadableRegistries()
                    .getLootTable(state.getBlock().getLootTable());
            var raw = new ArrayList<ItemStack>();
            table.getRandomItemsRaw(params, raw::add);
            helper.assertTrue(raw.isEmpty(), "raw loot API must not bypass the gate: " + ore);
            helper.assertTrue(
                    state.is(MekanismTags.Blocks.MINER_BLACKLIST), "Digital Miner must reject the ore: " + ore);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeBossGateAndPickaxeSuitabilityRemainIndependent(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        grant(player, ResourceLocation.parse("armageddon_mod:the_netherite_keeper"));
        var infinity = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("avaritia:infinity_pickaxe")));
        List<ItemStack> tools =
                List.of(new ItemStack(Items.NETHERITE_PICKAXE), infinity, ItemAtomicDisassembler.fullyChargedStack());
        for (String ore : ORES) {
            var state = state(ore);
            helper.assertTrue(!ArmageddonOreRules.allows(state, player), "locked ore must reject real players: " + ore);
            for (var tool : tools) {
                helper.assertTrue(!tool.isEmpty(), "the native tool must exist");
                helper.assertTrue(
                        tool.isCorrectToolForDrops(state), "shared pickaxe tags must preserve suitability: " + ore);
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                helper.assertTrue(
                        Block.getDrops(state, level, pos, null, player, tool).isEmpty(),
                        "locked tool loot must be empty");
                helper.assertTrue(
                        CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state)
                                .isCanceled(),
                        "the native/pack break gate must reject locked mining");
            }
            grant(player, ArmageddonOreRules.requiredAdvancement(ArmageddonOreRules.blockId(state)));
            helper.assertTrue(
                    ArmageddonOreRules.allows(state, player), "matching native boss advancement must unlock ore");
            for (var tool : tools) {
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                helper.assertTrue(
                        !Block.getDrops(state, level, pos, null, player, tool).isEmpty(),
                        "eligible manual mining must retain native drops");
            }
            helper.assertTrue(
                    Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY)
                            .isEmpty(),
                    "an owner entity without a mining tool is not a player harvest");
            var explosion = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.BLOCK_STATE, state)
                    .withParameter(LootContextParams.TOOL, tools.getFirst())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withParameter(LootContextParams.EXPLOSION_RADIUS, 2.0F)
                    .create(LootContextParamSets.BLOCK);
            var table = level.getServer()
                    .reloadableRegistries()
                    .getLootTable(state.getBlock().getLootTable());
            helper.assertTrue(
                    table.getRandomItems(explosion).isEmpty(),
                    "player-owned explosions must not bypass manual harvesting");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void createDrillExtractionCannotReceiveOreDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (String ore : ORES) {
            level.setBlock(pos, state(ore), 3);
            var drops = new ArrayList<ItemStack>();
            BlockHelper.destroyBlock(level, pos, 1.0F, drops::add);
            helper.assertTrue(drops.isEmpty(), "Create's block-breaking helper must receive no ore drops: " + ore);
            level.setBlock(pos, state(ore), 3);
            BlockHelper.destroyBlockAs(
                    level,
                    pos,
                    FakePlayerFactory.getMinecraft(level),
                    new ItemStack(Items.NETHERITE_PICKAXE),
                    1.0F,
                    drops::add);
            helper.assertTrue(drops.isEmpty(), "Create's fake-player extraction must receive no ore drops: " + ore);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void refinedStorageDestructorCannotUseItsOwnerProfileToBypassTheGate(GameTestHelper helper) {
        var level = helper.getLevel();
        var fake = Platform.INSTANCE.getFakePlayer(level, null);
        helper.assertTrue(
                fake instanceof net.neoforged.neoforge.common.util.FakePlayer,
                "RS must use its native fake-player path");
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (String ore : ORES) {
            var state = state(ore);
            grant((ServerPlayer) fake, ArmageddonOreRules.requiredAdvancement(ArmageddonOreRules.blockId(state)));
            level.setBlock(pos, state, 3);
            helper.assertTrue(
                    !Platform.INSTANCE.canBreakBlock(level, pos, state, fake),
                    "Destructor must reject ore even when the emulated profile has the boss unlock: " + ore);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void resetFunctionRevokesOnlyTheThreeOreUnlocks(GameTestHelper helper) {
        var player = player(helper);
        ResourceLocation netherite = ResourceLocation.parse("armageddon_mod:the_netherite_keeper");
        grant(player, netherite);
        for (String ore : ORES)
            grant(player, ArmageddonOreRules.requiredAdvancement(ArmageddonOreRules.blockId(state(ore))));
        player.server
                .getCommands()
                .performPrefixedCommand(
                        player.createCommandSourceStack().withPermission(4),
                        "function bertieprogression:reset_armageddon_test_unlocks");
        for (String ore : ORES)
            helper.assertTrue(!ArmageddonOreRules.allows(state(ore), player), "ore unlock must be reset");
        helper.assertTrue(
                player.getAdvancements()
                        .getOrStartProgress(player.server.getAdvancements().get(netherite))
                        .isDone(),
                "the independent Netherite unlock must remain complete");
        helper.succeed();
    }

    private ArmageddonOreGameTests() {}
}
