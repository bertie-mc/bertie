package io.github.bertie_mc.bertieprogression.mixin.combat;

import java.util.Stack;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface DamageStackAccessor {
    @Accessor("damageContainers")
    Stack<DamageContainer> bertie$damageContainers();
}
