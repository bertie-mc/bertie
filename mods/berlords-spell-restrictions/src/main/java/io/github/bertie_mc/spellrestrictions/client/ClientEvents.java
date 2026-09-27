package io.github.bertie_mc.spellrestrictions.client;

import io.github.bertie_mc.spellrestrictions.SpellRestrictions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;

@EventBusSubscriber(modid = SpellRestrictions.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void registerSprites(RegisterSpriteSourceTypesEvent event) {
        event.register(SpellRestrictions.id("orb"), OrbSpriteSource.TYPE);
    }
}
