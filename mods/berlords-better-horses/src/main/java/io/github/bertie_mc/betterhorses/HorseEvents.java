package io.github.bertie_mc.betterhorses;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class HorseEvents {
    private HorseEvents() {}

    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) return;
        Player player = event.getEntity();
        if (horse.isAlive()
                && (event.getItemStack().getItem() instanceof HorseAppleItem
                        || event.getItemStack().is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE)
                                && ((HorseAppearance) horse).betterhorses$appearance()
                                        != HorseAppearance.Style.NORMAL)) {
            event.setCancellationResult(horse.fedFood(player, event.getItemStack()));
            event.setCanceled(true);
            return;
        }
        if (event.getItemStack().getItem() instanceof HorseEffigyItem effigy) {
            InteractionResult result =
                    effigy.interactLivingEntity(event.getItemStack(), player, horse, event.getHand());
            event.setCancellationResult(result);
            event.setCanceled(true);
            return;
        }
        HorseEquipment equipment = (HorseEquipment) horse;
        if (horse.isTamed()
                && !horse.isBaby()
                && horse.isVehicle()
                && !player.isSecondaryUseActive()
                && equipment.betterhorses$syncedSaddle().is(BetterHorses.PASSENGER.get())
                && horse.getPassengers().size() < 2) {
            if (!horse.level().isClientSide && !player.startRiding(horse)) return;
            event.setCancellationResult(InteractionResult.sidedSuccess(horse.level().isClientSide));
            event.setCanceled(true);
        }
    }

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Horse horse
                && ((HorseEquipment) horse).betterhorses$tier() == ShoeTier.NETHERITE
                && (event.getSource().is(DamageTypes.HOT_FLOOR)
                        || event.getSource().is(DamageTypes.CAMPFIRE))) event.setCanceled(true);
    }

    public static void transferDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Horse horse
                && ((HorseEquipment) horse).betterhorses$syncedSaddle().is(BetterHorses.WARRIOR.get())
                && horse.getControllingPassenger() instanceof Player rider) {
            float transferred =
                    event.getNewDamage() * BetterHorses.DAMAGE_TRANSFER.get().floatValue();
            if (transferred > 0 && rider.hurt(event.getSource(), transferred))
                event.setNewDamage(event.getNewDamage() - transferred);
        }
    }

    public static boolean onLavaSurface(Horse horse) {
        if (!((HorseEquipment) horse).betterhorses$tier().lavaWalking()) return false;
        var pos = horse.blockPosition();
        var fluid = horse.level().getFluidState(pos);
        return fluid.is(FluidTags.LAVA) && horse.getY() >= pos.getY() + fluid.getHeight(horse.level(), pos) - 0.15;
    }
}
