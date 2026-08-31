package io.github.bertie_mc.hephaestusarchitecture.item;

import io.github.bertie_mc.hephaestusarchitecture.structure.ForgeStructurePlacement;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Creative-only tool that instantly builds one tier's complete Hephaestus Forge layout, for test
 * worlds. Using it on a block builds the layout on top of that block (or in its place when the
 * block is replaceable, like grass), forge included, and the forge activates immediately.
 */
public class ForgeStructurePlacerItem extends Item {

    private final int tier;

    public ForgeStructurePlacerItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        boolean creative = player.getAbilities().instabuild;
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return creative ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (!creative) {
            player.displayClientMessage(
                    Component.translatable("item.hephaestusarchitecture.structure_placer.creative_only"), true);
            return InteractionResult.FAIL;
        }

        BlockPos origin = level.getBlockState(context.getClickedPos()).canBeReplaced()
                ? context.getClickedPos()
                : context.getClickedPos().relative(context.getClickedFace());
        BlockPos forgePos = ForgeStructurePlacement.place(level, origin, this.tier);

        level.playSound(null, forgePos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(
                Component.translatable("item.hephaestusarchitecture.structure_placer.placed", this.tier), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hephaestusarchitecture.structure_placer.creative_exclusive")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.hephaestusarchitecture.structure_placer.desc", this.tier)
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
