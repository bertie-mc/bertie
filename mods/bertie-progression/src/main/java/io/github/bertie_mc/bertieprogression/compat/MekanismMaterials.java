package io.github.bertie_mc.bertieprogression.compat;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Optional chemical registrations; loaded only when Mekanism is present. */
public final class MekanismMaterials {
    private static final DeferredRegister<Chemical> CHEMICALS =
            DeferredRegister.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, BertieProgression.MODID);

    private static void infusion(String name, int color) {
        CHEMICALS.register(
                name,
                () ->
                        new Chemical(ChemicalBuilder.builder(
                                ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "chemical/" + name))) {
                            @Override
                            public int getColorRepresentation() {
                                // The icon is already coloured; non-textured displays still need its representative
                                // colour.
                                return color;
                            }
                        });
    }

    public static void register(IEventBus bus) {
        infusion("zinc", 0xB9E9C1);
        infusion("amethyst", 0xB38EF3);
        CHEMICALS.register(bus);
    }

    private MekanismMaterials() {}
}
