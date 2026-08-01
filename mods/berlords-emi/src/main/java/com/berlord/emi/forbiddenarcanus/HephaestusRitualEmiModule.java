package com.berlord.emi.forbiddenarcanus;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.stal111.forbidden_arcanus.common.block.entity.forge.TierPredicate;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.Ritual;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.RitualInput;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.CreateItemResult;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.RitualResult;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.TransmuteInputResult;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.UpgradeTierResult;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;

/**
 * Hephaestus Forge rituals. Unlike every other Forbidden & Arcanus recipe these live in a
 * DATAPACK REGISTRY ({@code forbidden_arcanus:hephaestus_forge/ritual}), not the RecipeManager,
 * so they are read from the client level's registry access at plugin registration (EMI reloads
 * per world join, so the level is available). Layout: main ingredient first, then the pedestal
 * inputs (sum of amounts is at most 8 — one item per pedestal); result is either a created item
 * or a Forge tier upgrade (shown as the target-tier Forge block). Essences and the tier
 * requirement become info lines.
 */
public final class HephaestusRitualEmiModule {

    private static final ResourceKey<Registry<Ritual>> RITUAL_REGISTRY = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath("forbidden_arcanus", "hephaestus_forge/ritual"));

    private static final int MAX_TIER = 5;

    /**
     * With Bertie Forge Ink installed the forge's experience essence is fueled by (and renamed to)
     * Ink, so the ritual cost line mirrors that; without it the stock "XP" wording stays.
     */
    private static final boolean FORGE_INK_LOADED = net.neoforged.fml.ModList.get().isLoaded("forgeink");

    private HephaestusRitualEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Registry<Ritual> rituals = level.registryAccess().registry(RITUAL_REGISTRY).orElse(null);
        if (rituals == null) {
            return;
        }

        // One category per Hephaestus Forge tier. Each tier's category (whose workstation is that tier's
        // Forge block) holds exactly the rituals that tier can perform, decided by the ritual's own
        // TierPredicate: an "at least tier N" ritual appears for tiers N..5, a tier-exact ritual (the
        // Forge upgrades) only for its exact tier. So viewing the Tier 2 Forge shows tier 1 + tier 2
        // rituals, the Tier 5 Forge shows every tier-progressive ritual, etc.
        EmiRecipeCategory[] byTier = new EmiRecipeCategory[MAX_TIER + 1];
        for (int t = 1; t <= MAX_TIER; t++) {
            byTier[t] = Categories.machine(reg, "fa_hephaestus_ritual_tier_" + t,
                    "forbidden_arcanus:hephaestus_forge_tier_" + t, "Hephaestus Ritual (Tier " + t + ")");
        }

        rituals.entrySet().forEach(entry -> {
            ResourceLocation ritualId = entry.getKey().location();
            Ritual ritual = entry.getValue();
            try {
                MachineDescriptor d = describe(ritual);
                TierPredicate tier = ritual.requirements().tier();
                for (int t = 1; t <= MAX_TIER; t++) {
                    if (!tier.test(t)) {
                        continue;
                    }
                    ResourceLocation displayId = ResourceLocation.fromNamespaceAndPath("berlords_emi",
                            "fa_hephaestus_ritual_tier_" + t + "/" + ritualId.getNamespace() + "/" + ritualId.getPath());
                    reg.addRecipe(new GenericEmiRecipe(byTier[t], displayId, d));
                }
            } catch (Throwable ignored) {
                // one malformed ritual must not take down the categories
            }
        });
    }

    /** Build the shared descriptor (inputs, result, essences, tier line) for one ritual. */
    private static MachineDescriptor describe(Ritual ritual) {
        MachineDescriptor d = new MachineDescriptor();
        d.itemIn(EmiIngredient.of(ritual.mainIngredient()));
        for (RitualInput input : ritual.inputs()) {
            d.itemIn(NeoForgeEmiIngredient.of(new SizedIngredient(input.ingredient(), input.amount())));
        }

        RitualResult result = ritual.result();
        if (result instanceof UpgradeTierResult upgrade) {
            Item forge = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                    "forbidden_arcanus", "hephaestus_forge_tier_" + upgrade.resultTier()));
            d.itemOut(EmiStack.of(new ItemStack(forge)));
            d.info(Component.literal("Upgrades the Forge to Tier " + upgrade.resultTier()));
        } else {
            ItemStack out = resolveResultItem(result, ritual.mainIngredient());
            if (!out.isEmpty()) {
                d.itemOut(EmiStack.of(out));
            } else {
                // Defensive: never render inputs -> arrow -> nothing for an unknown result type.
                d.info(Component.literal("Transforms the input item"));
            }
        }

        var essences = ritual.requirements().essences();
        if (essences.aureal() > 0) {
            d.info(Component.literal(essences.aureal() + " Aureal"));
        }
        if (essences.blood() > 0) {
            d.info(Component.literal(essences.blood() + " Blood"));
        }
        if (essences.souls() > 0) {
            d.info(Component.literal(essences.souls() + " Souls"));
        }
        if (essences.experience() > 0) {
            d.info(Component.literal(essences.experience() + (FORGE_INK_LOADED ? " Ink" : " XP")));
        }
        int tierNum = ritual.requirements().tier().tier();
        if (tierNum > 0) {
            d.info(Component.literal("Tier " + tierNum));
        }
        return d;
    }

    /**
     * The output stack to display for a ritual result. Handles every {@link RitualResult}
     * implementation: {@link CreateItemResult} (a crafted item) and {@link TransmuteInputResult}
     * (the main ingredient is upgraded/transformed into a target item — the armour rituals such as
     * {@code tyr_helmet}/{@code draco_arcanus_helmet}, which used to render inputs -> arrow ->
     * nothing), plus any other/future type via the polymorphic
     * {@link RitualResult#getResultItem(ItemStack)} with a sample of the main ingredient.
     * {@link UpgradeTierResult} is handled by the caller (it renders the target-tier Forge block).
     */
    private static ItemStack resolveResultItem(RitualResult result, Ingredient mainIngredient) {
        if (result instanceof CreateItemResult item) {
            return item.result();
        }
        if (result instanceof TransmuteInputResult transmute) {
            return new ItemStack(transmute.result().value());
        }
        // Unknown/future result type: ask it directly, feeding a sample of the main ingredient in
        // case the result is derived from the input (as transmute is).
        try {
            return result.getResultItem(sampleStack(mainIngredient));
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack sampleStack(Ingredient ing) {
        ItemStack[] items = ing.getItems();
        return items.length > 0 ? items[0] : ItemStack.EMPTY;
    }
}
