package io.github.bertie_mc.emi.integration.crabbersdelight;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Crabber's Delight — the Crab Trap. What a trap catches is decided by the bait, and the mod already
 * publishes that mapping as item tags for its JEI plugin to read: every item in
 * {@code crabbersdelight:crab_trap_bait} has a matching
 * {@code crabbersdelight:jei_display_results/<namespace>/<path>} tag listing its catch. Reading the
 * same tags needs nothing from the mod's own code.
 *
 * <p>One entry per bait-and-catch pair rather than one entry per bait with a row of outputs: the trap
 * yields one of them, so a shared output row would read as producing the whole list at once, and a
 * pair is what makes each catch findable from the item itself.
 */
public final class CrabbersDelightEmiModule {

    private static final String MOD = "crabbersdelight";

    private static final TagKey<Item> BAIT =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD, "crab_trap_bait"));

    private CrabbersDelightEmiModule() {}

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory trap = Categories.machine(reg, "crabbersdelight_crab_trap", MOD + ":crab_trap", "Crab Trap");
        BuiltInRegistries.ITEM.getTag(BAIT).ifPresent(baits -> {
            for (Holder<Item> bait : baits) {
                ResourceLocation baitId = BuiltInRegistries.ITEM.getKey(bait.value());
                // Air and an empty bucket are in the bait tag as the "no bait" case; neither is a
                // thing the player baits a trap with, and neither has a results tag.
                TagKey<Item> results = TagKey.create(
                        Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(
                                MOD, "jei_display_results/" + baitId.getNamespace() + "/" + baitId.getPath()));
                BuiltInRegistries.ITEM.getTag(results).ifPresent(catches -> {
                    for (Holder<Item> caught : catches) {
                        MachineDescriptor d = new MachineDescriptor();
                        d.itemIn(EmiStack.of(bait.value()));
                        d.itemOut(EmiStack.of(caught.value()));
                        reg.addRecipe(new GenericEmiRecipe(trap, id(baitId, caught), d));
                    }
                });
            }
        });
    }

    private static ResourceLocation id(ResourceLocation bait, Holder<Item> caught) {
        ResourceLocation caughtId = BuiltInRegistries.ITEM.getKey(caught.value());
        return ResourceLocation.fromNamespaceAndPath(
                "bertieemi",
                "crabbersdelight/crab_trap/" + bait.getNamespace() + "/" + bait.getPath() + "/"
                        + caughtId.getNamespace() + "/" + caughtId.getPath());
    }
}
