package io.github.bertie_mc.bertieprogression.combat;

import java.util.ArrayDeque;
import net.minecraft.world.damagesource.DamageSource;

public final class DamageRedirectContext {
    private static final ThreadLocal<ArrayDeque<DamageSource>> SOURCES = ThreadLocal.withInitial(ArrayDeque::new);

    private DamageRedirectContext() {}

    public static DamageSource current() {
        return SOURCES.get().peek();
    }

    public static void begin(DamageSource source) {
        SOURCES.get().push(source);
    }

    public static void end() {
        var stack = SOURCES.get();
        stack.pop();
        if (stack.isEmpty()) SOURCES.remove();
    }
}
