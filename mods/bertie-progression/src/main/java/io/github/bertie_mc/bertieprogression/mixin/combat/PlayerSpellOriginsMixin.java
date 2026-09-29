package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bertie_mc.bertieprogression.combat.MagicOrigins;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.api.spells.AbstractSpell", remap = false)
public abstract class PlayerSpellOriginsMixin {
    @WrapOperation(
            method = "*",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"),
            require = 1)
    private void bertie$onCast(
            AbstractSpell spell,
            Level level,
            int rank,
            LivingEntity caster,
            CastSource castSource,
            MagicData data,
            Operation<Void> original) {
        MagicOrigins.begin(caster, spell.getSchoolType().getId());
        try {
            original.call(spell, level, rank, caster, castSource, data);
        } finally {
            MagicOrigins.end();
        }
    }

    @WrapOperation(
            method = "*",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onServerPreCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"),
            require = 1)
    private void bertie$onServerPreCast(
            AbstractSpell spell, Level level, int rank, LivingEntity caster, MagicData data, Operation<Void> original) {
        MagicOrigins.begin(caster, spell.getSchoolType().getId());
        try {
            original.call(spell, level, rank, caster, data);
        } finally {
            MagicOrigins.end();
        }
    }
}
