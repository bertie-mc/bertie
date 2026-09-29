package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.CombatHit;
import io.github.bertie_mc.bertieprogression.combat.CombatHitState;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = DamageContainer.class, remap = false)
public abstract class DamageContainerStateMixin implements CombatHit {
    @Unique
    private final CombatHitState bertie$state = new CombatHitState();

    public CombatHitState bertie$combatState() {
        return bertie$state;
    }
}
