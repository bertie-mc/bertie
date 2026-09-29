package io.github.bertie_mc.fletching;

import com.fletchery.mod.entity.CustomArrowEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ArrowDamage {
    public static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();
    public static final ThreadLocal<Boolean> SHARING = ThreadLocal.withInitial(() -> false);

    public static final class Context {
        public final CompoundTag data;
        public final ArrowProfile profile;
        public final Entity owner;
        public float raw;
        public boolean damaged;

        public Context(CompoundTag data, Entity owner, float raw) {
            this.data = data;
            profile = ArrowProfile.read(data);
            this.owner = owner;
            this.raw = raw;
        }
    }

    private ArrowDamage() {}

    public static <T> T with(Context context, Supplier<T> action) {
        Context previous = CURRENT.get();
        CURRENT.set(context);
        try {
            return action.get();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static DamageSource source(ServerLevel level, String type, Entity direct, Entity owner) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ResourceKey.create(
                                Registries.DAMAGE_TYPE,
                                ResourceLocation.fromNamespaceAndPath(BertieFletching.ID, type))),
                direct,
                owner);
    }

    public static float raw(CustomArrowEntity arrow, LivingEntity target, float vanilla) {
        FlightState state = ((ArrowRuntime) arrow).bertie$flight();
        float value =
                state.fixedDamage >= 0 ? (float) state.fixedDamage : vanilla + state.profile.bonus(target, state.peak);
        return Math.max(0, value);
    }

    public static float estimate(CustomArrowEntity arrow, LivingEntity target) {
        return raw(arrow, target, (float) Math.ceil(arrow.getDeltaMovement().length() * arrow.getBaseDamage()));
    }

    public static boolean secondary(CompoundTag data, Entity owner, LivingEntity target, float raw, String kind) {
        if (!(target.level() instanceof ServerLevel level) || raw <= 0 || !target.isAlive()) return false;
        Context ctx = new Context(data, owner, raw);
        return with(ctx, () -> target.hurt(source(level, kind, null, owner), raw));
    }

    public static void execute(CompoundTag data, Entity owner, LivingEntity target) {
        if (target.isAlive() && target.getHealth() < target.getMaxHealth() * .3f)
            secondary(
                    data, owner, target, Math.max(.001f, target.getHealth() + target.getAbsorptionAmount()), "execute");
    }

    public static LivingEntity living(Entity target) {
        if (target instanceof LivingEntity living) return living;
        if (target instanceof net.neoforged.neoforge.entity.PartEntity<?> part
                && part.getParent() instanceof LivingEntity living) return living;
        return null;
    }

    public static boolean eligible(Entity owner, LivingEntity target) {
        if (!target.isAlive() || target.isSpectator() || !target.isAttackable() || target == owner) return false;
        if (owner != null && owner.isAlliedTo(target)) return false;
        return !(owner instanceof Player player && target instanceof Player other) || player.canHarmPlayer(other);
    }

    public static double launchSpeed(double height) {
        double low = 0, high = 8;
        for (int step = 0; step < 40; step++) {
            double mid = (low + high) * .5, speed = mid, rise = 0;
            for (int tick = 0; tick < 200 && speed > 0; tick++) {
                rise += speed;
                speed = (speed - .08) * .98;
            }
            if (rise < height) low = mid;
            else high = mid;
        }
        return (low + high) * .5;
    }
}
