package io.github.bertie_mc.emi.integration.apothicenchanting;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.shadowsoffire.apothic_enchanting.table.EnchantingStatRegistry.Stats;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import dev.shadowsoffire.apothic_enchanting.table.infusion.KeepNBTInfusionRecipe;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiCategory;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.InfoPages;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apothic Enchanting. The mod's only recipe type is Infusion - an item left in an enchanting table
 * that transforms once the table's Eterna, Quanta and Arcana sit inside stated bounds - and it ships
 * a JEI plugin only, so an EMI pack sees none of it. Everything else the mod adds to the anvil runs
 * off {@code AnvilUpdateEvent} rather than a recipe and so is unreachable from any viewer at all;
 * those interactions are reproduced here in EMI's own Anvil Repairing category.
 *
 * <p>An anvil entry needs a worked example to show anything, so it uses a concrete enchanted stack (a
 * Sharpness V sword, a Curse of Binding helmet) plus an info line naming the real input. They opt out
 * of the recipe tree: the sample is an illustration, not a way to obtain a diamond helmet.
 */
public final class ApothicEnchantingEmiModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("bertieemi");

    private static final String NS = "bertieemi";

    /** The three tables Infusion works in - the vanilla one and the mod's two variants. */
    private static final List<String> TABLES = List.of(
            "minecraft:enchanting_table",
            "apothic_enchanting:apothic_enchanting_table",
            "apothic_enchanting:raven_enchanting_table");

    /** {@code c:storage_blocks/iron} - what the anvil repair actually accepts. */
    private static final TagKey<Item> IRON_BLOCKS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/iron"));

    private ApothicEnchantingEmiModule() {}

    public static void register(EmiRegistry reg) {
        safely("infusion", () -> infusion(reg));
        safely("anvil", () -> anvil(reg));
        safely("guide", () -> guide(reg));
    }

    /**
     * One failing block must not cost the others: the infusion category comes from the mod's own
     * classes and the anvil entries from the client registries, which fail for unrelated reasons.
     */
    private static void safely(String part, Runnable body) {
        try {
            body.run();
        } catch (Throwable t) {
            LOGGER.warn("bertieemi: Apothic Enchanting '{}' could not be registered, skipping it", part, t);
        }
    }

    // --- Infusion ---------------------------------------------------------------------------

    private static void infusion(EmiRegistry reg) {
        GenericEmiCategory infusion = new GenericEmiCategory(
                ResourceLocation.fromNamespaceAndPath(NS, "apothic_infusion"),
                Categories.stack("minecraft:enchanting_table"),
                Component.translatableWithFallback("recipes.apothic_enchanting.infusion", "Infusion Enchanting"));
        reg.addCategory(infusion);
        for (String table : TABLES) {
            EmiStack station = Categories.stack(table);
            if (!station.isEmpty()) {
                reg.addWorkstation(infusion, station);
            }
        }

        Recipes.forEach(reg.getRecipeManager(), InfusionRecipe.class, (id, recipe) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(recipe.getInput()));
            d.itemOut(EmiStack.of(recipe.getOutput()));
            Stats min = recipe.getRequirements();
            Stats max = recipe.getMaxRequirements();
            requirement(d, "Eterna", min.eterna(), max.eterna(), false);
            requirement(d, "Quanta", min.quanta(), max.quanta(), true);
            requirement(d, "Arcana", min.arcana(), max.arcana(), true);
            if (recipe instanceof KeepNBTInfusionRecipe) {
                d.info(Component.literal("Carries the input item's own data over"));
            }
            reg.addRecipe(new GenericEmiRecipe(infusion, id, d));
        });
    }

    /**
     * One line per stat the recipe actually constrains. A stat with neither a floor nor a ceiling
     * (the mod writes an unbounded maximum as -1) constrains nothing, and is left out rather than
     * printed as a zero the player would read as a requirement.
     */
    private static void requirement(MachineDescriptor d, String stat, float min, float max, boolean percent) {
        boolean capped = max > -1.0F;
        if (min <= 0.0F && !capped) {
            return;
        }
        String unit = percent ? "%" : "";
        String text;
        if (capped && max == min) {
            text = stat + ": exactly " + amount(min) + unit;
        } else if (capped && min <= 0.0F) {
            text = stat + ": at most " + amount(max) + unit;
        } else if (capped) {
            text = stat + ": " + amount(min) + unit + " to " + amount(max) + unit;
        } else {
            text = stat + ": " + amount(min) + unit + " or more";
        }
        d.info(Component.literal(text));
    }

    private static String amount(float value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    // --- Anvil interactions -----------------------------------------------------------------

    private static void anvil(EmiRegistry reg) {
        EmiRecipeCategory anvil = VanillaEmiRecipeCategories.ANVIL_REPAIRING;
        EmiStack sword = enchanted("minecraft:diamond_sword", Enchantments.SHARPNESS, 5);
        EmiStack book = enchanted("minecraft:enchanted_book", Enchantments.SHARPNESS, 5);

        tome(
                reg,
                anvil,
                sword,
                book,
                "scrap_tome",
                "Keeps half of the enchantments, rounded up",
                "The item is destroyed",
                "6 levels per enchantment kept");
        tome(
                reg,
                anvil,
                sword,
                book,
                "improved_scrap_tome",
                "Keeps every enchantment",
                "The item is destroyed",
                "10 levels per enchantment");
        tome(
                reg,
                anvil,
                sword,
                book,
                "extraction_tome",
                "Keeps every enchantment",
                "The item survives, stripped of its enchantments",
                "16 levels per enchantment");

        curses(reg, anvil);
        repair(reg, anvil, "minecraft:chipped_anvil", "minecraft:damaged_anvil");
        repair(reg, anvil, "minecraft:damaged_anvil", "minecraft:anvil");
    }

    /** Enchanted item + tome -> a book holding some or all of its enchantments. */
    private static void tome(
            EmiRegistry reg, EmiRecipeCategory anvil, EmiStack sword, EmiStack book, String tomeId, String... lines) {
        EmiStack tome = Categories.stack("apothic_enchanting:" + tomeId);
        if (tome.isEmpty() || sword.isEmpty() || book.isEmpty()) {
            return;
        }
        MachineDescriptor d = new MachineDescriptor();
        d.itemIn(sword);
        d.itemIn(tome);
        d.itemOut(book);
        d.info(Component.literal("Any enchanted item"));
        for (String line : lines) {
            d.info(Component.literal(line));
        }
        add(reg, anvil, tomeId, d);
    }

    /** Cursed item + Prismatic Cobweb -> the same item with every curse gone. */
    private static void curses(EmiRegistry reg, EmiRecipeCategory anvil) {
        EmiStack cursed = enchanted("minecraft:diamond_helmet", Enchantments.BINDING_CURSE, 1);
        EmiStack web = Categories.stack("apothic_enchanting:prismatic_web");
        EmiStack clean = Categories.stack("minecraft:diamond_helmet");
        if (cursed.isEmpty() || web.isEmpty() || clean.isEmpty()) {
            return;
        }
        MachineDescriptor d = new MachineDescriptor();
        d.itemIn(cursed);
        d.itemIn(web);
        d.itemOut(clean);
        d.info(Component.literal("Any cursed item; only the curses are removed"));
        d.info(Component.literal("30 levels"));
        add(reg, anvil, "prismatic_web", d);
    }

    /** A chipped or damaged anvil is one iron block away from the next state up. */
    private static void repair(EmiRegistry reg, EmiRecipeCategory anvil, String damagedId, String resultId) {
        EmiStack damaged = Categories.stack(damagedId);
        EmiStack result = Categories.stack(resultId);
        if (damaged.isEmpty() || result.isEmpty()) {
            return;
        }
        MachineDescriptor d = new MachineDescriptor();
        d.itemIn(damaged);
        d.itemIn(EmiIngredient.of(IRON_BLOCKS));
        d.itemOut(result);
        d.info(Component.literal("5 levels"));
        add(reg, anvil, "anvil_repair/" + damagedId.substring(damagedId.indexOf(':') + 1), d);
    }

    private static void add(EmiRegistry reg, EmiRecipeCategory anvil, String path, MachineDescriptor d) {
        reg.addRecipe(new IllustrationRecipe(
                anvil, ResourceLocation.fromNamespaceAndPath(NS, "apothic_enchanting/" + path), d));
    }

    /**
     * An anvil interaction shown through one worked example. EMI must not read the example as a way
     * to obtain its output, or the recipe tree would offer to craft a plain diamond helmet out of a
     * cursed one it cannot produce either.
     */
    private static final class IllustrationRecipe extends GenericEmiRecipe {
        private IllustrationRecipe(EmiRecipeCategory category, ResourceLocation id, MachineDescriptor d) {
            super(category, id, d);
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }
    }

    /** The sample stack for an anvil entry; empty if the item or the enchantment is absent. */
    private static EmiStack enchanted(String itemId, ResourceKey<Enchantment> enchantment, int level) {
        EmiStack base = Categories.stack(itemId);
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
        stack.enchant(holder, level);
        return EmiStack.of(stack);
    }

    // --- Guide ------------------------------------------------------------------------------

    /**
     * The two explanations the mod's own JEI plugin attaches to items, read from its lang file so
     * they follow the player's language and stay right if the mod rewords them. Neither repeats a
     * tooltip the block already carries.
     */
    private static void guide(EmiRegistry reg) {
        InfoPages.translated(
                reg,
                "apothic_enchanting/enchanting",
                TABLES,
                Component.translatableWithFallback(
                        "info.apothic_enchanting.enchanting",
                        "Enchanting tables can now exceed 30 levels. Some enchantments only show up past level 30."));
        InfoPages.translated(
                reg,
                "apothic_enchanting/library",
                List.of("apothic_enchanting:library", "apothic_enchanting:ender_library"),
                Component.translatableWithFallback(
                        "info.apothic_enchanting.library",
                        "Stores enchantments. Inserting an enchanted book deposits it; clicking a stored"
                                + " enchantment withdraws a level of it."));
    }
}
