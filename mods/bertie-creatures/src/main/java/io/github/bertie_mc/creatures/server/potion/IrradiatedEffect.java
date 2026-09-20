package io.github.bertie_mc.creatures.server.potion;

import io.github.bertie_mc.creatures.server.misc.ACDamageTypes;
import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class IrradiatedEffect extends MobEffect {

    public static final int BLUE_LEVEL = 4;

    protected IrradiatedEffect() {
        super(MobEffectCategory.HARMFUL, 0X77D60E);
    }

    public boolean applyEffectTick(LivingEntity entity, int tick) {
        int hazmat = 0;
        float damageScale = 1F - hazmat * 0.25F;
        if (entity instanceof Player player && hazmat == 0) {
            player.causeFoodExhaustion(0.4F);
        }
        if (entity.level().random.nextFloat() < damageScale + 0.1F) {
            entity.hurt(ACDamageTypes.causeRadiationDamage(entity.level().registryAccess()), damageScale);
        }
        return true;
    }

    public boolean shouldApplyEffectTickThisTick(int tick1, int level) {
        if (level <= 0) {
            return false;
        }
        int j = 200 / level;
        if (j > 1) {
            return tick1 % j == j / 2;
        } else {
            return true;
        }
    }

    public List<ItemStack> getCurativeItems() {
        return List.of();
    }
}
