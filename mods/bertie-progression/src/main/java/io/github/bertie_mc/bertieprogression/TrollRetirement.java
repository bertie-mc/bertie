package io.github.bertie_mc.bertieprogression;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Prevents retired Troll variants from entering a server level, including saved mobs. */
public final class TrollRetirement {
    private static final ResourceLocation TROLL = ResourceLocation.parse("iceandfire:troll");

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && TROLL.equals(BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()))) {
            event.setCanceled(true);
            event.getEntity().discard();
        }
    }

    private TrollRetirement() {}
}
