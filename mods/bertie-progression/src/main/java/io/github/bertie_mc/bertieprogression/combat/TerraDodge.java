package io.github.bertie_mc.bertieprogression.combat;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.confluence.terra_curio.common.init.TCEffects;
import org.confluence.terra_curio.common.init.TCItems;
import org.confluence.terra_curio.common.init.TCTags;
import org.confluence.terra_curio.util.TCUtils;

public final class TerraDodge {
    private TerraDodge() {}

    public static void offers(LivingIncomingDamageEvent event, List<DodgeRules.Offer> offers) {
        var target = event.getEntity();
        if (event.getSource().getEntity() != null
                && !event.getSource().is(TCTags.HARMFUL_EFFECT)
                && TCUtils.hasType(target, TCItems.BRAIN$OF$CONFUSION)
                && !target.hasEffect(TCEffects.CEREBRAL_MINDTRICK)) {
            DodgeRules.offer(offers, 0.1667, () -> {
                TCUtils.applyBrainOfConfusion(target, target.getRandom(), event.getSource(), event.getAmount());
                target.addEffect(new MobEffectInstance(TCEffects.CEREBRAL_MINDTRICK, 80));
                DodgeRules.feedback(target);
            });
        }
    }
}
