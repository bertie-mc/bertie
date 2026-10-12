package io.github.bertie_mc.bertieprogression.warden;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.monster.warden.Warden;

/** The retaliation keeps sonic damage rules while always bypassing damage cooldown frames. */
public final class RetaliationDamageSource extends DamageSource {
    public RetaliationDamageSource(DamageSource normal, Warden warden) {
        super(normal.typeHolder(), warden);
    }

    @Override
    public boolean is(TagKey<DamageType> tag) {
        return tag.equals(DamageTypeTags.BYPASSES_COOLDOWN) || super.is(tag);
    }
}
