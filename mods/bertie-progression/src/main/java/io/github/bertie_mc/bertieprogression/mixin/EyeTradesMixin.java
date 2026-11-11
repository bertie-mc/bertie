package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.ender.EyeAcquisitions;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Villager.class, WanderingTrader.class})
public abstract class EyeTradesMixin {
    @Inject(method = "updateTrades", at = @At("RETURN"))
    private void bertie$evilEyeOffers(CallbackInfo ci) {
        if ((Object) this instanceof Villager villager) EyeAcquisitions.clericTrades(villager);
        else if ((Object) this instanceof WanderingTrader trader) EyeAcquisitions.wanderingTrades(trader);
    }
}
