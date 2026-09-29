package io.github.bertie_mc.bertieprogression.mixin.combat;

import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.socket.gem.bonus.special.MageSlayerBonus", remap = false)
public abstract class MageslayerHealingMixin {
    @Shadow
    @Final
    protected Map<Purity, Float> values;

    @Inject(method = "onHurt", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$convertAfterResistance(
            GemInstance gem,
            DamageSource source,
            LivingEntity target,
            float amount,
            CallbackInfoReturnable<Float> cir) {
        if (!DamageFamilies.isEnergy(source)) {
            cir.setReturnValue(amount);
            return;
        }
        var stack = ((DamageStackAccessor) target).bertie$damageContainers();
        if (stack == null || stack.isEmpty()) {
            cir.setReturnValue(amount);
            return;
        }
        var container = stack.peek();
        float fraction = values.get(gem.purity());
        EnergyLayers.state(container).deferDefense(0, () -> {
            float prevented = container.getNewDamage() * fraction;
            container.setNewDamage(container.getNewDamage() - prevented);
            if (prevented > 0) target.heal(prevented);
        });
        cir.setReturnValue(amount);
    }
}
