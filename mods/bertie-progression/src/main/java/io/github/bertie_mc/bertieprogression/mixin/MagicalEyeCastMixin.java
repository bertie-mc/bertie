package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.ender.MagicalEyeChallenge;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.api.spells.AbstractSpell", remap = false)
public abstract class MagicalEyeCastMixin {
    // This invocation occurs only on a successful, paid cast, before onCast and regeneration.
    @Redirect(
            method = "castSpell",
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;setMana(F)V"),
            require = 1)
    private void bertie$paidMana(
            MagicData data,
            float mana,
            Level level,
            int spellLevel,
            ServerPlayer player,
            CastSource source,
            boolean cooldown) {
        float before = data.getMana();
        data.setMana(mana);
        MagicalEyeChallenge.onManaPaid(player, before, data.getMana());
    }
}
