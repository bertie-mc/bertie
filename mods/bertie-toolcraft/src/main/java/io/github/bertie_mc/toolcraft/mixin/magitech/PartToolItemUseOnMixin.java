package io.github.bertie_mc.toolcraft.mixin.magitech;

import io.github.bertie_mc.toolcraft.ToolcraftPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.stln.magitech.item.tool.ToolType;
import net.stln.magitech.item.tool.toolitem.PartToolItem;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Gives Magitech's tools their right-click block interactions.
 *
 * <p>Magitech answers {@code canPerformAction} for the hoe, axe and shovel ability sets, but
 * never implements {@code useOn} - and advertising an ability is not performing it. Vanilla does
 * the actual work in {@code HoeItem.useOn} / {@code AxeItem.useOn} / {@code ShovelItem.useOn},
 * which {@code PartToolItem} does not inherit because it extends {@code Item} directly. The
 * result is that no Magitech tool can till, strip, scrape or flatten a path.
 *
 * <p>This adds the missing method. It is a new method on the target class rather than an
 * injection, since there is nothing to inject into.
 *
 * <p>Only the four kept tools respond, and each to its vanilla counterpart's abilities. The
 * pickaxe has no block interaction in vanilla and gets none here.
 */
@Mixin(PartToolItem.class)
public abstract class PartToolItemUseOnMixin {

    public abstract ToolType getToolType();

    public InteractionResult useOn(UseOnContext context) {
        ToolType type = this.getToolType();
        if (type == null) {
            return InteractionResult.PASS;
        }

        ItemAbility[] abilities = switch (type.getId()) {
            case ToolcraftPolicy.SCYTHE -> new ItemAbility[] {ItemAbilities.HOE_TILL};
            case ToolcraftPolicy.AXE -> new ItemAbility[] {
                ItemAbilities.AXE_STRIP, ItemAbilities.AXE_SCRAPE, ItemAbilities.AXE_WAX_OFF
            };
            case ToolcraftPolicy.SHOVEL -> new ItemAbility[] {ItemAbilities.SHOVEL_FLATTEN};
            default -> null;
        };
        if (abilities == null) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        for (ItemAbility ability : abilities) {
            // Neither tilling nor path-flattening may fire from underneath.
            if ((ability == ItemAbilities.HOE_TILL || ability == ItemAbilities.SHOVEL_FLATTEN)
                    && context.getClickedFace() == Direction.DOWN) {
                continue;
            }
            // Flattening needs headroom. Tilling deliberately does not get the same check: the
            // headroom rule belongs to individual tillables (grass, dirt, path) and is already
            // applied inside getToolModifiedState, while rooted dirt tills with a block above it.
            if (ability == ItemAbilities.SHOVEL_FLATTEN && !level.getBlockState(pos.above()).isAir()) {
                continue;
            }

            BlockState modified = state.getToolModifiedState(context, ability, false);
            if (modified == null) {
                continue;
            }

            Player player = context.getPlayer();
            level.playSound(player, pos, soundFor(ability), SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!level.isClientSide) {
                level.setBlock(pos, modified, Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, modified));
                if (player != null) {
                    ItemStack stack = context.getItemInHand();
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static SoundEvent soundFor(ItemAbility ability) {
        if (ability == ItemAbilities.HOE_TILL) {
            return SoundEvents.HOE_TILL;
        }
        if (ability == ItemAbilities.SHOVEL_FLATTEN) {
            return SoundEvents.SHOVEL_FLATTEN;
        }
        if (ability == ItemAbilities.AXE_SCRAPE) {
            return SoundEvents.AXE_SCRAPE;
        }
        if (ability == ItemAbilities.AXE_WAX_OFF) {
            return SoundEvents.AXE_WAX_OFF;
        }
        return SoundEvents.AXE_STRIP;
    }
}
