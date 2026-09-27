package io.github.bertie_mc.spellrestrictions.mixin;

import io.github.bertie_mc.spellrestrictions.Restrictions;
import io.redspace.ironsspellbooks.item.EldritchManuscript;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EldritchManuscript.class, remap = false)
public abstract class EldritchManuscriptMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void researchRandomSpell(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        var stack = player.getItemInHand(hand);
        boolean success =
                !(player instanceof ServerPlayer server) || Restrictions.research(server, stack, Restrictions.ELDRITCH);
        cir.setReturnValue(
                success
                        ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                        : InteractionResultHolder.fail(stack));
    }
}
