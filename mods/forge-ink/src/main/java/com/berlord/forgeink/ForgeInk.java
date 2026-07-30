package com.berlord.forgeink;

import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeLevel;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.essence.EssenceValue;
import com.stal111.forbidden_arcanus.core.init.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bertie Forge Ink — the Hephaestus Forge runs on Iron's Spells 'n Spellbooks ink
 * instead of experience:
 *
 * <ul>
 *   <li>Each ink carries Forbidden Arcanus' {@code ESSENCE_VALUE} data component
 *       (EXPERIENCE, {@value #BASE_ESSENCE_PER_INK} × 2^(tier−1): 40/80/160/320/640),
 *       so FA's own {@code EssenceDataInput}
 *       accepts and consumes it — no custom input registration needed.</li>
 *   <li>A forge of tier N accepts only the tier-N ink (Common=I … Legendary=V).
 *       Enforced by mixins into {@code HephaestusForgeBlockEntity#getInput} (server
 *       consumption + hopper/slot validity) and {@code HephaestusForgeMenu#canInput}
 *       (menu-side validation). The same mixins block every non-ink experience
 *       source: XP bottles, Xpetrified Orbs and enchanted-gear disenchanting.</li>
 *   <li>Former XP items get their essence component stripped again so their
 *       tooltips stop claiming they fill the forge.</li>
 *   <li>A force-enabled built-in resource pack renames the essence to "Ink" and
 *       swaps the XP slot's glass-bottle ghost icon for an ink-pot one.</li>
 * </ul>
 */
@Mod(ForgeInk.MOD_ID)
public class ForgeInk {

    public static final String MOD_ID = "forgeink";
    /** Tier-1 ink's essence; every following tier doubles it (40/80/160/320/640). */
    public static final int BASE_ESSENCE_PER_INK = 40;

    private static final Logger LOGGER = LoggerFactory.getLogger("ForgeInk");

    /** Index 0 = forge tier ONE … index 4 = forge tier FIVE. */
    private static final ResourceLocation[] INK_BY_TIER = {
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "common_ink"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "uncommon_ink"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "rare_ink"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "epic_ink"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "legendary_ink")
    };

    public ForgeInk(IEventBus modBus) {
        modBus.addListener(this::modifyDefaultComponents);
        modBus.addListener(this::addPackFinders);
    }

    private void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        // 1) FA gives vanilla XP bottles ESSENCE_VALUE(EXPERIENCE, 15) in its own
        //    listener — strip it again so bottles stop being an essence item anywhere
        //    (forge, utrem jars, JEI). Our mods.toml orders us AFTER forbidden_arcanus,
        //    so our patch wins. The Xpetrified Orb keeps its component (its right-click
        //    XP redeem reads it!); the EssenceDataInput mixin blocks it instead.
        event.modify(Items.EXPERIENCE_BOTTLE,
                builder -> builder.remove(ModDataComponents.ESSENCE_VALUE.get()));

        // 2) Tier-N ink fills 40 × 2^(N−1) essence (40/80/160/320/640) — in its own
        //    tier's forge only (see mixins).
        for (int i = 0; i < INK_BY_TIER.length; i++) {
            ResourceLocation id = INK_BY_TIER[i];
            Item ink = BuiltInRegistries.ITEM.get(id);
            if (ink == Items.AIR) {
                LOGGER.error("Ink item {} not found — is Iron's Spells 'n Spellbooks loaded?", id);
                continue;
            }
            int amount = BASE_ESSENCE_PER_INK << i;
            event.modify(ink, builder -> builder.set(
                    ModDataComponents.ESSENCE_VALUE.get(),
                    EssenceValue.of(EssenceType.EXPERIENCE, amount)));
        }
    }

    private void addPackFinders(AddPackFindersEvent event) {
        // Force-enabled pack at TOP so our lang key + forge GUI texture reliably
        // override FA's own resources (plain mod-jar overrides are load-order lottery).
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addPackFinders(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "resourcepacks/forgeink_overrides"),
                    PackType.CLIENT_RESOURCES,
                    Component.literal("Bertie Forge Ink"),
                    PackSource.BUILT_IN,
                    true,
                    Pack.Position.TOP);
        }
    }

    /** @return 1–5 for the five inks (Common→Legendary), or -1 for any other item. */
    public static int inkTier(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        for (int i = 0; i < INK_BY_TIER.length; i++) {
            if (INK_BY_TIER[i].equals(id)) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Whitelist for the forge's EXPERIENCE ("Ink") input: only the ink matching the
     * forge's tier passes. Everything else — wrong-tier inks, XP bottles, Xpetrified
     * Orbs, enchanted gear (disenchanting) — is rejected.
     */
    public static boolean allowExperienceInput(ItemStack stack, HephaestusForgeLevel forgeLevel) {
        return inkTier(stack.getItem()) == forgeLevel.getAsInt();
    }
}
