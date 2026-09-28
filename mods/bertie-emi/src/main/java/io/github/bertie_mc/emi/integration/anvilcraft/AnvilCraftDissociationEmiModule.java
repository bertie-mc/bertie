package io.github.bertie_mc.emi.integration.anvilcraft;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Template Dissociation — destroying an enchanted Eight-to-One Smithing Template as a dropped item
 * breaks it back into armour trim templates, and into the Transcendium Upgrade Smithing Template
 * that nothing else in the mod produces.
 *
 * <p>There is no recipe file behind any of it: the whole thing is the template item's own
 * {@code onDestroyed} hook, which is why no viewer could show it and why the Transcendium template
 * looked as though it had no source at all.
 *
 * <p>Up to four templates are rolled from the enchantments the template carries, each roll mapping
 * one enchantment to one trim. Those mappings are the entries here, one apiece, so a trim can be
 * traced back to the enchantment that produces it. The rolls draw with replacement, so four
 * enchantments do not guarantee four different templates — and four different templates are what
 * the Transcendium bonus needs.
 */
final class AnvilCraftDissociationEmiModule {
    private AnvilCraftDissociationEmiModule() {}

    private static final String TEMPLATE = "anvilcraft:eight_to_one_smithing_template";
    private static final String TRANSCENDIUM = "anvilcraft:transcendium_upgrade_smithing_template";

    /** One enchantment on the template, and the trim the roll turns it into. */
    private record Mapping(ResourceKey<Enchantment> enchantment, String trim) {}

    private static final List<Mapping> MAPPINGS = List.of(
            new Mapping(Enchantments.PROTECTION, "ward"),
            new Mapping(Enchantments.FIRE_PROTECTION, "rib"),
            new Mapping(Enchantments.FIRE_ASPECT, "rib"),
            new Mapping(Enchantments.FLAME, "rib"),
            new Mapping(Enchantments.BLAST_PROTECTION, "dune"),
            new Mapping(Enchantments.PROJECTILE_PROTECTION, "wild"),
            new Mapping(Enchantments.SOUL_SPEED, "snout"),
            new Mapping(Enchantments.SWIFT_SNEAK, "silence"),
            new Mapping(Enchantments.MENDING, "vex"),
            new Mapping(Enchantments.INFINITY, "sentry"),
            new Mapping(Enchantments.DENSITY, "bolt"),
            new Mapping(Enchantments.BREACH, "bolt"),
            new Mapping(Enchantments.WIND_BURST, "flow"),
            new Mapping(Enchantments.FORTUNE, "spire"),
            new Mapping(Enchantments.LOOTING, "eye"),
            new Mapping(Enchantments.LUCK_OF_THE_SEA, "coast"),
            new Mapping(Enchantments.LURE, "coast"),
            new Mapping(Enchantments.DEPTH_STRIDER, "tide"),
            new Mapping(Enchantments.RESPIRATION, "tide"),
            new Mapping(Enchantments.AQUA_AFFINITY, "tide"),
            new Mapping(Enchantments.IMPALING, "tide"),
            new Mapping(Enchantments.RIPTIDE, "tide"));

    /** What a roll gives when the enchantment it picked is not one of the mapped ones. */
    private static final List<String> UNMAPPED = List.of("wayfinder", "raiser", "host", "shaper");

    static void register(EmiRegistry reg) {
        EmiStack template = Categories.stack(TEMPLATE);
        if (template.isEmpty()) {
            return;
        }
        EmiRecipeCategory cat =
                Categories.machineNoStation(reg, "anvilcraft_dissociation", TEMPLATE, "Template Dissociation");

        overview(reg, cat, template);
        for (Mapping mapping : MAPPINGS) {
            mapping(reg, cat, mapping);
        }
        unmapped(reg, cat, template);
    }

    /**
     * The bonus that makes the whole thing worth doing, and the only two facts a picture cannot
     * carry: how the template is destroyed, and what "all four different" means.
     */
    private static void overview(EmiRegistry reg, EmiRecipeCategory cat, EmiStack template) {
        EmiStack result = Categories.stack(TRANSCENDIUM);
        if (result.isEmpty()) {
            return;
        }
        MachineDescriptor d = new MachineDescriptor();
        d.itemIn(template);
        d.itemOut(result);
        d.info(Component.literal("Destroy the enchanted template as a dropped item: fire, lava or an explosion"));
        d.info(Component.literal("It rolls four trim templates from its enchantments, and adds this one when"
                + " all four came out different"));
        reg.addRecipe(new GenericEmiRecipe(cat, id("overview"), d));
    }

    /** One roll: the template carrying this enchantment, and the trim that roll produces. */
    private static void mapping(EmiRegistry reg, EmiRecipeCategory cat, Mapping mapping) {
        EmiStack in = enchantedTemplate(mapping.enchantment());
        EmiStack out = Categories.stack("minecraft:" + mapping.trim() + "_armor_trim_smithing_template");
        if (in.isEmpty() || out.isEmpty()) {
            return;
        }
        MachineDescriptor d = new MachineDescriptor();
        d.itemIn(in);
        d.itemOut(out);
        reg.addRecipe(new GenericEmiRecipe(
                cat, id("enchantment/" + mapping.enchantment().location().getPath()), d));
    }

    /** Every other enchantment rolls one of four at random, so each carries a quarter chance. */
    private static void unmapped(EmiRegistry reg, EmiRecipeCategory cat, EmiStack template) {
        MachineDescriptor d = new MachineDescriptor();
        d.itemIn(template);
        for (String trim : UNMAPPED) {
            EmiStack out = Categories.stack("minecraft:" + trim + "_armor_trim_smithing_template");
            if (!out.isEmpty()) {
                d.itemOut(out.setChance(1.0F / UNMAPPED.size()));
            }
        }
        if (d.itemOutputs.isEmpty()) {
            return;
        }
        d.info(Component.literal("A roll on any enchantment other than the ones listed here"));
        reg.addRecipe(new GenericEmiRecipe(cat, id("unmapped"), d));
    }

    /** The template as it has to be to produce that trim: carrying the enchantment the roll reads. */
    private static EmiStack enchantedTemplate(ResourceKey<Enchantment> enchantment) {
        EmiStack base = Categories.stack(TEMPLATE);
        if (base.isEmpty() || Minecraft.getInstance().level == null) {
            return EmiStack.EMPTY;
        }
        Holder<Enchantment> holder = Minecraft.getInstance()
                .level
                .registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(enchantment)
                .orElse(null);
        if (holder == null) {
            return EmiStack.EMPTY;
        }
        ItemStack stack = base.getItemStack().copy();
        stack.enchant(holder, 1);
        return EmiStack.of(stack);
    }

    /** The mechanic has no recipe file, so its EMI ids are ours to mint. */
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("bertieemi", "anvilcraft/dissociation/" + path);
    }
}
