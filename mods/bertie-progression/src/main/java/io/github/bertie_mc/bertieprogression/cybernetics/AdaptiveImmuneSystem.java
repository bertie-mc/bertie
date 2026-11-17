package io.github.bertie_mc.bertieprogression.cybernetics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.block.entity.augmenter.api.CustomAugmentsCollection;

/** Retains installed Auto-Feeder identities while replacing its feeding effect. */
public final class AdaptiveImmuneSystem extends Augment {
    public static final ResourceLocation ID = ResourceLocation.parse("oritech:augment/autofeeder");
    private static final AdaptiveImmuneSystem INSTANCE = new AdaptiveImmuneSystem();

    private AdaptiveImmuneSystem() {
        super(ID, true);
    }

    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CustomAugmentsCollection.CUSTOM_AUGMENTS.put(ID, INSTANCE));
    }

    public static boolean protects(LivingEntity entity) {
        return entity instanceof Player player && ModList.get().isLoaded("oritech") && INSTANCE.isEnabled(player);
    }

    @Override
    public void activate(Player player) {}

    @Override
    public void deactivate(Player player) {}

    @Override
    public void refreshServer(Player player) {}

    @Override
    public int refreshInterval() {
        return Integer.MAX_VALUE;
    }
}
