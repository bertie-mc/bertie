package io.github.bertie_mc.bertieprogression.combat;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public interface MagicOrigin {
    boolean bertie$isSpellEntity();

    UUID bertie$spellCaster();

    ResourceLocation bertie$spellSchool();

    void bertie$spellOrigin(UUID caster, ResourceLocation school);
}
