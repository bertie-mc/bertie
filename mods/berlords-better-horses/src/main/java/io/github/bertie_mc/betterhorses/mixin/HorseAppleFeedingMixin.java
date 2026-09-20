package io.github.bertie_mc.betterhorses.mixin;

import io.github.bertie_mc.betterhorses.*;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public abstract class HorseAppleFeedingMixin {
    @Shadow
    private void eating() {
        throw new AssertionError();
    }

    @Inject(method = "handleEating", at = @At("HEAD"), cancellable = true)
    private void betterhorses$feedApple(Player player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Horse horse) || !(stack.getItem() instanceof HorseAppleItem apple)) return;
        boolean changed = horse.isAlive() && apple.canChange(horse);
        if (changed) {
            if (!horse.level().isClientSide) apple.apply(horse);
            eating();
            horse.gameEvent(GameEvent.EAT);
        }
        cir.setReturnValue(changed);
    }

    @Inject(method = "handleEating", at = @At("RETURN"), cancellable = true)
    private void betterhorses$restoreAppearance(Player player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Horse horse) || !horse.isAlive() || !stack.is(Items.ENCHANTED_GOLDEN_APPLE))
            return;
        HorseAppearance appearance = (HorseAppearance) horse;
        if (appearance.betterhorses$appearance() == HorseAppearance.Style.NORMAL) return;
        if (!horse.level().isClientSide) appearance.betterhorses$setAppearance(HorseAppearance.Style.NORMAL);
        // Vanilla feeding still supplies healing, temper, growth and breeding; fedFood consumes exactly once.
        if (!cir.getReturnValue()) {
            eating();
            horse.gameEvent(GameEvent.EAT);
        }
        cir.setReturnValue(true);
    }
}
