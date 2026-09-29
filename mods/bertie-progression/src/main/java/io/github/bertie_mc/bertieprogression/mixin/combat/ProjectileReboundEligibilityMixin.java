package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.MagicOrigins;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class ProjectileReboundEligibilityMixin {
    @Inject(method = "is", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$spellProjectileCannotRebound(TagKey<EntityType<?>> tag, CallbackInfoReturnable<Boolean> cir) {
        if (!tag.location().getNamespace().equals("pastel")
                || !tag.location().getPath().equals("undeflectable")) return;
        EntityType<?> type = (EntityType<?>) (Object) this;
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace();
        if (namespace.equals("irons_spellbooks")
                || namespace.equals("fdbosses")
                || MagicOrigins.isActiveSpellType(type)) cir.setReturnValue(true);
    }
}
