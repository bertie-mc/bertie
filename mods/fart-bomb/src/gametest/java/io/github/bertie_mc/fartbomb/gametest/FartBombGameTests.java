package io.github.bertie_mc.fartbomb.gametest;

import io.github.bertie_mc.fartbomb.FartBomb;
import io.github.bertie_mc.fartbomb.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fartbomb")
@PrefixGameTestTemplate(false)
public final class FartBombGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final Holder<SoundEvent> FART = Holder.direct(SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("artifacts", "item.whoopee_cushion.fart")));

    private FartBombGameTests() {}

    @GameTest(template = EMPTY_TEMPLATE)
    public static void fieryBlocksRespectTagsAndLitState(GameTestHelper helper) {
        helper.assertTrue(
                Blocks.MAGMA_BLOCK.defaultBlockState().is(ModTags.IGNITES_BELOW),
                "magma should be in the below-player ignition tag");

        BlockPos relativeFeet = new BlockPos(1, 1, 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absoluteFeet = helper.absolutePos(relativeFeet);
        player.setPos(absoluteFeet.getX() + 0.5, absoluteFeet.getY(), absoluteFeet.getZ() + 0.5);

        helper.setBlock(relativeFeet, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false));
        helper.assertFalse(
                FartBomb.onFart(helper.getLevel(), FART, player),
                "an extinguished campfire should not ignite the player");

        helper.setBlock(relativeFeet, Blocks.CAMPFIRE.defaultBlockState());
        helper.assertTrue(FartBomb.onFart(helper.getLevel(), FART, player), "a lit campfire should ignite the player");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void burningPlayerDetonatesFart(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.igniteForSeconds(5);
        helper.assertTrue(
                FartBomb.onFart(helper.getLevel(), FART, player),
                "burning player's fart should detonate and replace the sound");
        helper.succeed();
    }
}
