package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.bertieprogression.combat.MagicOrigin;
import io.github.bertie_mc.bertieprogression.combat.MagicOrigins;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLevel.class)
public abstract class MagicEntityTickMixin {
    @WrapMethod(method = "tickNonPassenger")
    private void bertie$spellChildren(Entity entity, Operation<Void> original) {
        if (!((MagicOrigin) entity).bertie$isSpellEntity()) {
            original.call(entity);
            return;
        }
        MagicOrigins.beginEntity(entity);
        try {
            original.call(entity);
        } finally {
            MagicOrigins.end();
        }
    }
}
