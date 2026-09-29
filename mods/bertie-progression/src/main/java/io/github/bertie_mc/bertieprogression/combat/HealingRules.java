package io.github.bertie_mc.bertieprogression.combat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

public final class HealingRules {
    private record Provider(String mod, String type, String method, String receiver) {}

    private static final List<Provider> SOURCES = List.of(
            new Provider("malstone", "com.ytgld.malstone.items.white.HugeSouls", "lLivingHealEvent", null),
            new Provider(
                    "enigmaticlegacyplus",
                    "auviotre.enigmatic.legacy.contents.effect.Poison",
                    "onLivingHeal",
                    "enigmaticlegacyplus:poison"),
            new Provider(
                    "enigmaticlegacyplus",
                    "auviotre.enigmatic.legacy.contents.item.food.ForbiddenFruit$Events",
                    "onHeal",
                    null),
            new Provider(
                    "enigmaticlegacyplus",
                    "auviotre.enigmatic.legacy.contents.item.scrolls.CursedScroll$Events",
                    "onHeal",
                    null),
            new Provider(
                    "enigmaticlegacyplus",
                    "auviotre.enigmatic.legacy.contents.item.scrolls.CursedXpScroll$Events",
                    "onLivingHeal",
                    null),
            new Provider("irons_spellbooks", "io.redspace.ironsspellbooks.effect.BlightEffect", "reduceHealing", null),
            new Provider(
                    "irons_spellbooks",
                    "io.redspace.ironsspellbooks.effect.SoulBurnMobEffect",
                    "soulBurnReduceHealing",
                    null),
            new Provider("youkaisfeasts", "dev.xkmc.youkaishomecoming.events.EffectEventHandlers", "onHeal", null));
    private static final ThreadLocal<Boolean> READING_PROVIDER = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<ArrayDeque<LivingEntity>> HEALING = ThreadLocal.withInitial(ArrayDeque::new);
    private static volatile List<MethodHandle> providers;

    private HealingRules() {}

    public static boolean readingProvider() {
        return READING_PROVIDER.get();
    }

    public static boolean suppressNativeBleeding(LivingEntity entity) {
        return !HEALING.get().isEmpty() && HEALING.get().peek() == entity;
    }

    public static void begin(LivingEntity entity) {
        HEALING.get().push(entity);
    }

    public static void end() {
        ArrayDeque<LivingEntity> stack = HEALING.get();
        stack.pop();
        if (stack.isEmpty()) {
            HEALING.remove();
        }
    }

    public static float modifiedAmount(LivingEntity entity, float amount) {
        if (amount <= 0 || entity.level().isClientSide()) {
            return amount;
        }
        double change = attributeChange(entity, "apothic_attributes:healing_received")
                + attributeChange(entity, "malum:healing_received");
        if (CombatRegistry.effectLevel(entity, "simplymore:bleed") > 0) {
            change -= 0.5;
        }
        // Each audited handler only reads its condition/config and scales a heal. A unit probe
        // preserves those conditions while combining their percentages instead of multiplying them.
        READING_PROVIDER.set(true);
        try {
            for (MethodHandle provider : providers()) {
                LivingHealEvent probe = new LivingHealEvent(entity, 1.0F);
                provider.invokeExact(probe);
                change += probe.getAmount() - 1.0;
            }
        } catch (Throwable failure) {
            throw new IllegalStateException("Unable to evaluate a registered healing modifier", failure);
        } finally {
            READING_PROVIDER.remove();
        }
        return (float) (amount * Math.max(0, 1.0 + change));
    }

    private static double attributeChange(LivingEntity entity, String id) {
        AttributeInstance attribute = CombatRegistry.attribute(entity, id);
        if (attribute == null) {
            return 0;
        }
        return attribute.getBaseValue()
                - 1.0
                + attribute.getModifiers().stream()
                        .mapToDouble(modifier -> modifier.amount())
                        .sum();
    }

    private static List<MethodHandle> providers() {
        List<MethodHandle> cached = providers;
        if (cached != null) {
            return cached;
        }
        synchronized (HealingRules.class) {
            if (providers == null) {
                List<MethodHandle> found = new ArrayList<>();
                for (Provider source : SOURCES) {
                    if (!ModList.get().isLoaded(source.mod())) {
                        continue;
                    }
                    try {
                        Class<?> type = Class.forName(source.type());
                        Method method = type.getDeclaredMethod(source.method(), LivingHealEvent.class);
                        method.setAccessible(true);
                        MethodHandle handle = MethodHandles.lookup().unreflect(method);
                        if (source.receiver() != null) {
                            handle = handle.bindTo(CombatRegistry.effect(source.receiver())
                                    .orElseThrow()
                                    .value());
                        }
                        found.add(handle.asType(MethodType.methodType(void.class, LivingHealEvent.class)));
                    } catch (ReflectiveOperationException failure) {
                        throw new IllegalStateException("Unsupported healing provider: " + source.type(), failure);
                    }
                }
                providers = List.copyOf(found);
            }
            return providers;
        }
    }
}
