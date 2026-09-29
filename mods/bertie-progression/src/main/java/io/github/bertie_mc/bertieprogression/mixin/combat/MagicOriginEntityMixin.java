package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.MagicOrigin;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class MagicOriginEntityMixin implements MagicOrigin {
    @Unique
    private UUID bertie$caster;

    @Unique
    private ResourceLocation bertie$school;

    public boolean bertie$isSpellEntity() {
        return bertie$caster != null;
    }

    public UUID bertie$spellCaster() {
        return bertie$caster;
    }

    public ResourceLocation bertie$spellSchool() {
        return bertie$school;
    }

    public void bertie$spellOrigin(UUID caster, ResourceLocation school) {
        bertie$caster = caster;
        bertie$school = school;
    }
}
