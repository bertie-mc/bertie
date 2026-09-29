package io.github.bertie_mc.fletching;

import java.lang.reflect.Method;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

public final class CombatBridge {
    private static Method primordialFire;
    private static Method transferDamage, redirectedDamage;
    private static boolean checkedDamageRoutes;

    private CombatBridge() {}

    private static void damageRoutes() {
        if (checkedDamageRoutes) return;
        checkedDamageRoutes = true;
        if (!ModList.get().isLoaded("bertieprogression")) return;
        try {
            Class<?> routes = Class.forName("io.github.bertie_mc.bertieprogression.combat.DamageRoutes");
            transferDamage = routes.getMethod("transfer", DamageSource.class);
            redirectedDamage = routes.getMethod("redirected", DamageSource.class);
        } catch (ClassNotFoundException ignored) {
            // Older pack builds predate the shared transfer protocol.
        } catch (NoSuchMethodException error) {
            throw new IllegalStateException("Bertie combat transfer integration changed", error);
        }
    }

    public static boolean redirected(DamageSource source) {
        damageRoutes();
        try {
            return redirectedDamage != null && (boolean) redirectedDamage.invoke(null, source);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot inspect combat transfer", error);
        }
    }

    public static DamageSource transfer(DamageSource source) {
        damageRoutes();
        try {
            return transferDamage == null ? source : (DamageSource) transferDamage.invoke(null, source);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot preserve combat transfer properties", error);
        }
    }

    public static void primordial(LivingEntity target) {
        if (!ModList.get().isLoaded("pastel")) return;
        try {
            if (primordialFire == null)
                primordialFire = Class.forName("earth.terrarium.pastel.attachments.data.PrimordialFireData")
                        .getMethod("setPrimordialFireTicks", LivingEntity.class, long.class);
            primordialFire.invoke(null, target, 200L);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Pastel Primordial Fire integration changed", error);
        }
    }

    public static void bleed(LivingEntity target) {
        // The shared combat pipeline routes this effect's ticks to physical bleeding damage.
        BuiltInRegistries.MOB_EFFECT
                .getHolder(ResourceLocation.parse("simplymore:bleed"))
                .ifPresent(effect -> {
                    MobEffectInstance old = target.getEffect(effect);
                    target.addEffect(new MobEffectInstance(
                            effect,
                            old == null ? 200 : Math.max(200, old.getDuration()),
                            old == null ? 0 : Math.min(255, old.getAmplifier() + 1)));
                });
    }
}
