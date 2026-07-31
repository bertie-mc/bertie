package com.berlord.emi.bertie;

import com.berlord.bertie_progression.forge.BedRecipes;
import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * "Mallet Work" — bertie_progression's Opening-Mallet interactions: the data-driven Brick-Forge bed
 * recipes ({@link BedRecipes#RECIPES}) plus the three hardcoded world strikes (mud, paper
 * press, forge formation). Inputs are reconstructed from each predicate's {@code describe()}
 * string — the three formats ("ns:path", "#tag", "slag:dynamic_part{material,part}") are
 * owned by the same project. Only ever called when {@code bertie_progression} is loaded.
 */
public final class BertieProgressionEmiModule {
    private BertieProgressionEmiModule() {
    }

    private static final int WIDE = 160; // modest floor; long strike-instructions now word-wrap (framework)

    public static void register(EmiRegistry reg) {
        // Pack presentation policy (bertie_progression present = the bertie pack): wooden/bone Slag ARMOR is
        // replaced by Immersive Armors via carving overrides, so hide those part variants from EMI's
        // index. Component lookups go through the registry — no Slag classes touched (slag optional).
        reg.removeEmiStacks(BertieProgressionEmiModule::isReplacedSlagArmorPart);

        EmiRecipeCategory cat = Categories.machine(reg, "bertie_mallet", "slag:brick_forge", "Mallet Work");
        EmiStack mallet = Categories.stack("bertie_progression:opening_mallet");
        if (!mallet.isEmpty()) {
            reg.addWorkstation(cat, mallet);
        }

        for (BedRecipes.BedRecipe r : BedRecipes.RECIPES) {
            MachineDescriptor d = new MachineDescriptor().minWidth(WIDE);
            addInput(d, r.primary());
            addInput(d, r.secondary());
            addInput(d, r.tertiary());
            d.catalyst(mallet);
            d.itemOut(stackOf(r.resultId(), r.resultCount()));
            if (r.extraReturnId() != null) {
                d.itemOut(stackOf(r.extraReturnId(), r.extraReturnCount()));
            }
            d.info(Component.literal((r.sneak() ? "SNEAK-strike" : "Strike")
                    + " the placed Brick Forge holding input 1; carry the rest"));
            reg.addRecipe(new GenericEmiRecipe(cat, displayId("bed/" + r.id()), d));
        }

        // --- hardcoded world strikes (ForgeBedHandler) ---
        MachineDescriptor mud = new MachineDescriptor().minWidth(WIDE);
        mud.itemIn(EmiStack.of(Items.DIRT));
        mud.catalyst(mallet);
        mud.itemOut(EmiStack.of(Items.MUD));
        mud.info(Component.literal("Strike placed Dirt that touches water"));
        reg.addRecipe(new GenericEmiRecipe(cat, displayId("world/mud"), mud));

        MachineDescriptor paper = new MachineDescriptor().minWidth(WIDE);
        paper.itemIn(EmiStack.of(Items.SUGAR_CANE, 3));
        paper.catalyst(mallet);
        paper.catalyst(sized("berlords_carving:wood_slate", 2));
        paper.itemOut(EmiStack.of(Items.PAPER, 3));
        paper.info(Component.literal("Strike a block touching water; the two slates are kept"));
        reg.addRecipe(new GenericEmiRecipe(cat, displayId("world/paper"), paper));

        MachineDescriptor forge = new MachineDescriptor().minWidth(WIDE);
        forge.itemIn(EmiStack.of(Items.MUD_BRICKS, 8));
        forge.itemIn(EmiStack.of(Items.CAMPFIRE));
        forge.itemIn(tag("bertie_progression:stripped_logs", 4));
        forge.catalyst(mallet);
        forge.itemOut(stackOf("slag:brick_forge", 1));
        forge.info(Component.literal("Build: 3x3 Mud Brick ring around an UNLIT campfire, "
                + "4 stripped logs above the corners - strike a ring brick"));
        reg.addRecipe(new GenericEmiRecipe(cat, displayId("world/brick_forge"), forge));
    }

    /** True for a {@code slag:dynamic_part} whose material is wooden/bone and part is an armor piece. */
    private static boolean isReplacedSlagArmorPart(EmiStack stack) {
        try {
            net.minecraft.world.item.ItemStack s = stack.getItemStack();
            if (s.isEmpty() || !BuiltInRegistries.ITEM.getKey(s.getItem())
                    .equals(ResourceLocation.parse("slag:dynamic_part"))) {
                return false;
            }
            String material = componentString(s, "slag:material_type");
            if (!"slag:wooden".equals(material) && !"slag:bone".equals(material)) {
                return false;
            }
            String part = componentString(s, "slag:part_type");
            return "slag:helmet".equals(part) || "slag:chestplate".equals(part)
                    || "slag:leggings".equals(part) || "slag:boots".equals(part);
        } catch (Throwable t) {
            return false; // never let pack policy break the whole plugin
        }
    }

    private static String componentString(net.minecraft.world.item.ItemStack s, String typeId) {
        var type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(typeId));
        if (type == null) {
            return null;
        }
        Object val = s.get(type);
        return val == null ? null : String.valueOf(val);
    }

    private static void addInput(MachineDescriptor d, BedRecipes.Input in) {
        if (in == null) {
            return;
        }
        String desc = in.what().describe();
        if (desc.startsWith("#")) {
            d.itemIn(tag(desc.substring(1), in.count()));
        } else if (desc.contains("{")) {
            // "slag:dynamic_part{material,part}" — show the base part item, note exactness below
            String[] mp = desc.substring(desc.indexOf('{') + 1, desc.indexOf('}')).split(",");
            d.itemIn(sized("slag:dynamic_part", in.count()));
            d.info(Component.literal("Needs the exact Slag-cast "
                    + Categories.capitalize(mp[0].replace("slag:", "")) + " "
                    + Categories.capitalize(mp[1].replace("slag:", ""))));
        } else {
            d.itemIn(sized(desc, in.count()));
        }
    }

    private static EmiIngredient sized(String itemId, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        return NeoForgeEmiIngredient.of(new SizedIngredient(Ingredient.of(item), count));
    }

    private static EmiIngredient tag(String tagId, int count) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
        return NeoForgeEmiIngredient.of(new SizedIngredient(Ingredient.of(key), count));
    }

    private static EmiStack stackOf(String id, int count) {
        return EmiStack.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)), count);
    }

    private static ResourceLocation displayId(String path) {
        return ResourceLocation.fromNamespaceAndPath("berlords_emi", "bertie_mallet/" + path);
    }
}
