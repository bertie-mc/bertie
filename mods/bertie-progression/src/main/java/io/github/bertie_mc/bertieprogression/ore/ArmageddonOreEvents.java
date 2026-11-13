package io.github.bertie_mc.bertieprogression.ore;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = BertieProgression.MODID)
public final class ArmageddonOreEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakOre(BlockEvent.BreakEvent event) {
        if (!ArmageddonOreRules.allows(event.getState(), event.getPlayer())) event.setCanceled(true);
    }

    private ArmageddonOreEvents() {}
}
