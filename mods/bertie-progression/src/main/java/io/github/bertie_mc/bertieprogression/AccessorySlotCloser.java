package io.github.bertie_mc.bertieprogression;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Takes the accessory slot away from the player, since no slot file can.
 *
 * <p>TerraCurio's {@code accessory} slot is not sized by data alone: a Demon Heart grows it through
 * the Curios inventory, and that growth is stored on the player, so setting the slot's size to 0
 * left berlord with the four he had already earned. The slot is closed in this pack, its tag is
 * empty and nothing can go in it, so the four are removed here on the way in - the same API that
 * granted them, run in reverse.
 *
 * <p>The slot type itself is left registered. Deleting one is what crashed the server tick when
 * Sophisticated Backpacks asked what a backpack fits.
 */
public final class AccessorySlotCloser {

    private static final String SLOT = "accessory";

    private AccessorySlotCloser() {
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        CuriosApi.getCuriosInventory(event.getEntity()).ifPresent(inv ->
                inv.getStacksHandler(SLOT).ifPresent(handler -> {
                    int slots = handler.getSlots();
                    if (slots <= 0) {
                        return;
                    }
                    try {
                        handler.clearModifiers();
                        inv.shrinkSlotType(SLOT, slots);
                    } catch (RuntimeException e) {
                        // Better a stray slot than a failed login.
                    }
                }));
    }
}
