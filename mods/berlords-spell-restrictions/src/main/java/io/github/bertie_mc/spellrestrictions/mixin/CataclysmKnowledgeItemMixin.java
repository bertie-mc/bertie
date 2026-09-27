package io.github.bertie_mc.spellrestrictions.mixin;

import io.github.bertie_mc.spellrestrictions.Restrictions;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cataclysm: Spellbooks' knowledge items. Its weak ones, the Burning and Frozen Knowledge Fragments,
 * become crafting material; the Burning Manuscript and Frozen Tablet teach one unknown spell from
 * their list per use, announced like every other manuscript.
 */
@Pseudo
@Mixin(targets = "net.acetheeldritchking.cataclysm_spellbooks.items.KnowledgeItem", remap = false)
public abstract class CataclysmKnowledgeItemMixin {
    @Shadow
    private List<Supplier<AbstractSpell>> spells;

    @Shadow
    private boolean isWeak;

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void learnOneSpell(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        var stack = player.getItemInHand(hand);
        if (isWeak) {
            cir.setReturnValue(InteractionResultHolder.pass(stack));
            return;
        }
        boolean success = !(player instanceof ServerPlayer server) || Restrictions.learnOneOf(server, stack, spells);
        cir.setReturnValue(
                success
                        ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                        : InteractionResultHolder.fail(stack));
    }

    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void hideFragmentUse(
            ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag, CallbackInfo ci) {
        if (isWeak) ci.cancel();
    }
}
