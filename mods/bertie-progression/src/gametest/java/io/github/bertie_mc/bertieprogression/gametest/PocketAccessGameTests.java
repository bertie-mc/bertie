package io.github.bertie_mc.bertieprogression.gametest;

import com.mojang.authlib.GameProfile;
import io.github.bertie_mc.bertieprogression.ModAttachments;
import io.github.bertie_mc.bertieprogression.ModBlocks;
import io.github.bertie_mc.bertieprogression.ModItems;
import io.github.bertie_mc.bertieprogression.pocket.PocketTravelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pocketdimension.init.PocketDimensionModBlocks;
import net.pocketdimension.network.OpenKeyMessage;
import net.pocketdimension.network.PocketDimensionModVariables;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class PocketAccessGameTests {
    private static final BlockPos PLATFORM = new BlockPos(1, 1, 1);

    @GameTest(template = "empty")
    public static void handMiningDropsBlockAtBeaconSpeed(GameTestHelper helper) {
        helper.setBlock(PLATFORM, ModBlocks.POCKET_DIMENSION.get());
        FakePlayer player = player(helper);
        var state = helper.getBlockState(PLATFORM);
        var pos = helper.absolutePos(PLATFORM);
        helper.assertTrue(player.hasCorrectToolForDrops(state), "empty hands must harvest the block");
        helper.assertTrue(
                state.getDestroyProgress(player, helper.getLevel(), pos)
                        == Blocks.BEACON.defaultBlockState().getDestroyProgress(player, helper.getLevel(), pos),
                "hand mining must take as long as a beacon");
        helper.assertTrue(player.gameMode.destroyBlock(pos), "survival hand mining must break the block");
        helper.assertTrue(
                helper
                        .getLevel()
                        .getEntities(
                                EntityType.ITEM,
                                new AABB(pos).inflate(2),
                                item -> item.getItem().is(ModItems.POCKET_DIMENSION.get()))
                        .stream()
                        .anyMatch(item -> item.getItem().getCount() == 1),
                "mining must drop the usable block item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void essenceUnlockIsPersonalPersistentAndNotWasted(GameTestHelper helper) {
        FakePlayer player = player(helper);
        FakePlayer other = player(helper);
        ItemStack essence = new ItemStack(ModItems.POCKET_ESSENCE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, essence);
        ModItems.POCKET_ESSENCE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!player.getData(ModAttachments.POCKET_UNLOCKED), "starting to drink must not unlock travel");
        player.stopUsingItem();
        helper.assertTrue(essence.getCount() == 2, "canceling use must not consume essence");
        ModItems.POCKET_ESSENCE.get().finishUsingItem(essence, helper.getLevel(), player);
        helper.assertTrue(player.getData(ModAttachments.POCKET_UNLOCKED), "finishing must unlock travel");
        helper.assertTrue(!other.getData(ModAttachments.POCKET_UNLOCKED), "unlock must not affect another player");
        helper.assertTrue(essence.getCount() == 1, "exactly one essence must be consumed");
        ModItems.POCKET_ESSENCE.get().finishUsingItem(essence, helper.getLevel(), player);
        helper.assertTrue(essence.getCount() == 1, "unlocked players must not waste more essence");
        CompoundTag saved = new CompoundTag();
        player.saveWithoutId(saved);
        FakePlayer reloaded = player(helper);
        reloaded.load(saved);
        helper.assertTrue(reloaded.getData(ModAttachments.POCKET_UNLOCKED), "unlock must survive a save and reload");
        FakePlayer respawned = player(helper);
        respawned.restoreFrom(player, false);
        helper.assertTrue(respawned.getData(ModAttachments.POCKET_UNLOCKED), "unlock must survive death");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void lockedKeyDoesNotStartTravel(GameTestHelper helper) {
        FakePlayer player = player(helper);
        OpenKeyMessage.pressAction(player, 0, 0);
        helper.assertTrue(
                player.getData(PocketDimensionModVariables.PLAYER_VARIABLES).Cooldown == 0,
                "locked key must not start the original travel or cooldown");
        helper.runAfterDelay(65, () -> {
            helper.assertTrue(player.level() == helper.getLevel(), "locked key must not teleport later");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void essenceEnablesOriginalKeyTravel(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getServer().getLevel(PocketTravelHandler.DIMENSION) != null,
                "test world must load the pocket dimension");
        FakePlayer player = player(helper);
        ModItems.POCKET_ESSENCE
                .get()
                .finishUsingItem(new ItemStack(ModItems.POCKET_ESSENCE.get()), helper.getLevel(), player);
        OpenKeyMessage.pressAction(player, 0, 0);
        helper.assertTrue(
                player.getData(PocketDimensionModVariables.PLAYER_VARIABLES).Cooldown > 0,
                "unlocked key must start the original travel");
        helper.succeedWhen(() -> helper.assertEntityProperty(
                player,
                entity -> entity.level().dimension().equals(PocketTravelHandler.DIMENSION),
                "unlocked key must reach the pocket dimension"));
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void crouchingOnBlockMakesRoundTripWithoutEssence(GameTestHelper helper) {
        helper.setBlock(PLATFORM, ModBlocks.POCKET_DIMENSION.get());
        helper.assertTrue(
                helper.getLevel().getServer().getLevel(PocketTravelHandler.DIMENSION) != null,
                "test world must load the pocket dimension");
        FakePlayer player = player(helper);
        var returnPosition = player.position();
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                player.getData(PocketDimensionModVariables.PLAYER_VARIABLES).Cooldown <= 0,
                "standing without crouching must not trigger travel");
        player.setShiftKeyDown(true);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(!player.getData(ModAttachments.POCKET_UNLOCKED), "block entry must not grant the key unlock");
        helper.assertTrue(
                player.getData(PocketDimensionModVariables.PLAYER_VARIABLES).Cooldown > 0,
                "crouching on the block must start original travel");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertEntityProperty(
                        player,
                        entity -> entity.level().dimension().equals(PocketTravelHandler.DIMENSION),
                        "the block must reach the pocket dimension without essence"))
                .thenExecute(() -> {
                    BlockPos portalPos = player.blockPosition().below();
                    var portal = player.level().getBlockState(portalPos);
                    helper.assertTrue(
                            portal.is(PocketDimensionModBlocks.POCKET_BLOCK.get()),
                            "the original return portal must be present");
                    player.setShiftKeyDown(true);
                    portal.getBlock().stepOn(player.level(), portalPos, portal, player);
                })
                .thenWaitUntil(() -> helper.assertEntityProperty(
                        player,
                        entity -> entity.level() == helper.getLevel(),
                        "the original portal must return a player without essence"))
                .thenExecute(() -> helper.assertTrue(
                        player.position().distanceToSqr(returnPosition) < 0.01,
                        "return portal must preserve the block entry location"))
                .thenSucceed();
    }

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PocketTest"));
        // FakePlayer's default handler drops teleports as well as packets. Keep the real server
        // teleport implementation while discarding outbound packets for this headless test.
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection =
                new ServerGamePacketListenerImpl(
                        helper.getLevel().getServer(),
                        connection,
                        player,
                        CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                    @Override
                    public void send(Packet<?> packet) {}

                    @Override
                    public void send(Packet<?> packet, PacketSendListener listener) {}
                };
        player.setGameMode(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(PLATFORM);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        player.setOnGround(true);
        return player;
    }

    private PocketAccessGameTests() {}
}
