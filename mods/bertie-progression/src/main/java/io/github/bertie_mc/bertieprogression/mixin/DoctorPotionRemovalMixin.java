package io.github.bertie_mc.bertieprogression.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.bertie_mc.bertieprogression.RemovedItems;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.benji.netherman.common.entity.DoctorEntity", remap = false)
public abstract class DoctorPotionRemovalMixin {
    @ModifyExpressionValue(
            method = "giveRandomPotion",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toList()Ljava/util/List;"),
            require = 1)
    private List<Holder.Reference<MobEffect>> bertie$excludeRemovedEffects(
            List<Holder.Reference<MobEffect>> available) {
        return available.stream()
                .filter(effect -> !RemovedItems.isRemovedEffect(effect))
                .toList();
    }
}
