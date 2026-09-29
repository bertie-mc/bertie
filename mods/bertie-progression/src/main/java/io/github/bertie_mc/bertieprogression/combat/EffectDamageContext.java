package io.github.bertie_mc.bertieprogression.combat;

import java.util.ArrayDeque;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class EffectDamageContext {
    private record Active(LivingEntity target, ResourceLocation effect, boolean harmful) {}

    private static final ThreadLocal<ArrayDeque<Active>> ACTIVE = ThreadLocal.withInitial(ArrayDeque::new);
    private static final Set<String> BURSTS =
            Set.of("apothic_attributes:detonation", "born_in_chaos_v1:living_bomb", "born_in_chaos_v1:detonation");

    private EffectDamageContext() {}

    public static void begin(LivingEntity target, Holder<MobEffect> effect) {
        ACTIVE.get()
                .push(new Active(
                        target,
                        effect.unwrapKey().map(ResourceKey::location).orElse(null),
                        effect.value().getCategory() == MobEffectCategory.HARMFUL));
    }

    public static void end() {
        var stack = ACTIVE.get();
        stack.pop();
        if (stack.isEmpty()) ACTIVE.remove();
    }

    public static boolean isDot(LivingEntity target) {
        Active active = ACTIVE.get().peek();
        return active != null
                && active.target() == target
                && active.harmful()
                && (active.effect() == null || !BURSTS.contains(active.effect().toString()));
    }

    public static DamageSource route(LivingEntity target, DamageSource original) {
        Active active = ACTIVE.get().peek();
        if (active == null || active.target() != target || active.effect() == null) return original;
        String damageType =
                switch (active.effect().toString()) {
                    case "simplymore:bleed" -> "bertieprogression:bleeding";
                    case "apothic_attributes:detonation" -> "apothic_attributes:detonation";
                    default -> null;
                };
        if (damageType == null) return original;
        var holder = target.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(damageType)));
        return new DamageSource(holder, original.getDirectEntity(), original.getEntity(), original.getSourcePosition());
    }
}
