package io.github.bertie_mc.bertieprogression;

import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Crushing Wheels sometimes leave an Exanimate Essence behind.
 *
 * <p>Exanimate Essence gates the Arcanetic Shaft and Cog, and Twilight Forest's own source for it is
 * a mob drop the pack does not otherwise route a player toward. Create's wheels are the deliberate
 * alternative: build the machine, feed it, and the essence comes out at a rate that scales with what
 * you are willing to put in it.
 *
 * <p>Keyed on the {@code create:crush} damage type rather than on the wheel block, so it fires for
 * anything the wheels actually kill and for nothing else. Other Create hazards - the saw, the
 * roller, run_over - each have their own damage type and are untouched.
 *
 * <p>Rates are 1% for passive mobs, 5% for hostile mobs and 50% for players. The player rate is
 * deliberately the outlier: feeding yourself to the wheels is the fast route and costs accordingly.
 */
public final class CrushingEssenceHandler {

    private CrushingEssenceHandler() {
    }

    /** Create's Crushing Wheel damage type. Not the saw, roller or run_over types. */
    private static final ResourceKey<net.minecraft.world.damagesource.DamageType> CRUSH =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("create:crush"));

    private static final ResourceLocation ESSENCE =
            ResourceLocation.parse("twilightforest:exanimate_essence");

    private static final float CHANCE_PASSIVE = 0.01F;
    private static final float CHANCE_HOSTILE = 0.05F;
    private static final float CHANCE_PLAYER = 0.50F;

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        if (!event.getSource().is(CRUSH)) {
            return;
        }
        if (victim.getRandom().nextFloat() >= chanceFor(victim)) {
            return;
        }
        // Twilight Forest is an optional dependency; without it there is no essence to drop and
        // this quietly does nothing rather than throwing on a missing id.
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ESSENCE);
        if (item.isEmpty()) {
            return;
        }
        event.getDrops().add(dropAt(victim, new ItemStack(item.get())));
    }

    private static float chanceFor(Entity victim) {
        if (victim instanceof Player) {
            return CHANCE_PLAYER;
        }
        // Enemy is the interface every hostile implements, which covers modded mobs too - a
        // MobCategory check would miss anything registered under a custom category.
        return victim instanceof Enemy ? CHANCE_HOSTILE : CHANCE_PASSIVE;
    }

    private static ItemEntity dropAt(LivingEntity victim, ItemStack stack) {
        ItemEntity entity = new ItemEntity(victim.level(), victim.getX(), victim.getY(),
                victim.getZ(), stack);
        entity.setDefaultPickUpDelay();
        return entity;
    }
}
