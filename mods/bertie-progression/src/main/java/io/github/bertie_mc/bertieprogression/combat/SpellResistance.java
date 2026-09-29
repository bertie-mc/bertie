package io.github.bertie_mc.bertieprogression.combat;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class SpellResistance {
    private SpellResistance() {}

    public static ResourceLocation schoolOf(DamageSource source) {
        return source instanceof SpellDamageSource spell
                ? spell.spell().getSchoolType().getId()
                : MagicOrigins.school(source);
    }

    public static float applyMissing(LivingEntity target, DamageSource source, float amount) {
        if (DamageFamilies.of(source) != DamageFamily.MAGIC) return amount;
        SchoolType school;
        if (source instanceof SpellDamageSource spell) {
            if (SpellDamageContext.contains(source)) return amount;
            school = spell.spell().getSchoolType();
        } else {
            var id = MagicOrigins.school(source);
            if (id == null) return amount;
            school = SchoolRegistry.getSchool(id);
        }
        SpellDamageContext.begin(source);
        try {
            return amount * DamageSources.getResist(target, school);
        } finally {
            SpellDamageContext.end();
        }
    }
}
