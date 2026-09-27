package io.github.bertie_mc.bertieprogression;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Doubles how fast Tiny Skeletons' baby wither skeleton moves. A mob's ground speed grows with the
 * square of its movement-speed attribute, so the attribute is raised by a factor of the square root
 * of two. The modifier is transient and reapplied on every join, which also covers babies already
 * saved in a world.
 */
public final class BabyWitherSkeletonSpeed {
    private static final ResourceLocation BABY_WITHER_SKELETON =
            ResourceLocation.fromNamespaceAndPath("tinyskeletons", "baby_wither_skeleton");
    private static final AttributeModifier DOUBLE_SPEED = new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "baby_wither_skeleton_speed"),
            Math.sqrt(2.0) - 1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof LivingEntity mob)
                || !BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).equals(BABY_WITHER_SKELETON)) {
            return;
        }
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(DOUBLE_SPEED);
        }
    }

    private BabyWitherSkeletonSpeed() {}
}
