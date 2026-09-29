package io.github.bertie_mc.bertieprogression.combat;

import java.util.ArrayDeque;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class SpellDamageContext {
    private static final ThreadLocal<ArrayDeque<DamageSource>> SOURCES = ThreadLocal.withInitial(ArrayDeque::new);

    private SpellDamageContext() {}

    public static boolean contains(DamageSource source) {
        return SOURCES.get().peek() == source;
    }

    public static DamageSource current() {
        return SOURCES.get().peek();
    }

    public static void begin(DamageSource source) {
        SOURCES.get().push(source);
    }

    public static void end() {
        ArrayDeque<DamageSource> stack = SOURCES.get();
        stack.pop();
        if (stack.isEmpty()) {
            SOURCES.remove();
        }
    }

    public static double penetrate(double resistance) {
        ArrayDeque<DamageSource> stack = SOURCES.get();
        if (resistance <= 1 || stack.isEmpty() || !(stack.peek().getEntity() instanceof LivingEntity attacker)) {
            return resistance;
        }
        double penetration = Math.max(0, CombatRegistry.value(attacker, "aces_spell_utils:spell_res_penetration", 0));
        return Math.max(1.0, resistance - penetration);
    }
}
