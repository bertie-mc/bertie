package io.github.bertie_mc.bertieprogression;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Drops content from every creative tab: single ids listed in the generated
 * {@code removed_items.json}, and whole tabs listed in {@code removed_tabs.json}.
 *
 * <p>That single act also removes them from EMI: the pack's {@code emi.css} sets
 * {@code index-source: creative}, so EMI builds its index from the creative tabs. One mechanism, no
 * second list to drift out of sync. Their recipes are separately overridden with
 * {@code neoforge:false} by the same generator pass, so a removed item is both uncraftable and
 * invisible.
 *
 * <p><b>Why a tab list as well as an id list.</b> Some content is not addressable by item id. A mod
 * that gives each of its themes a creative tab often fills those tabs with stacks of ONE shared
 * item that differ only in a data component, so removing that item by id empties every theme at
 * once. Emptying a single tab takes exactly that theme's stacks, and it keeps covering the theme's
 * blocks, items and spawn eggs without anyone maintaining a list of them.
 *
 * <p>{@code removed_tabs.json} is hand-written, unlike {@code removed_items.json}: it names whole
 * categories the pack has cut, not ids derived from a jar scan, so there is nothing to generate.
 *
 * <p>Both lists are read from the JAR ROOT rather than from a datapack tag on purpose: creative tab
 * contents are built client-side and can be rebuilt before any datapack or tag sync has happened,
 * so a tag-based list would apply unreliably. A classpath resource is always there.
 *
 * <p>What this does NOT do: unregister anything. The item still exists in the registry - it has to,
 * or every save holding one would break. It is unobtainable and unseen, not absent.
 */
public final class RemovedItems {

    private static Set<ResourceLocation> ids;
    private static Set<ResourceLocation> tabs;
    private static Set<ResourceLocation> potions;
    private static Set<ResourceLocation> effects;

    private static Set<ResourceLocation> ids() {
        if (ids == null) {
            ids = read("/removed_items.json");
        }
        return ids;
    }

    private static Set<ResourceLocation> tabs() {
        if (tabs == null) {
            tabs = read("/removed_tabs.json");
        }
        return tabs;
    }

    private static Set<ResourceLocation> read(String resource) {
        Set<ResourceLocation> parsed = new HashSet<>();
        try (InputStream in = RemovedItems.class.getResourceAsStream(resource)) {
            if (in != null) {
                List<String> raw = new Gson()
                        .fromJson(
                                new InputStreamReader(in, StandardCharsets.UTF_8),
                                new TypeToken<List<String>>() {}.getType());
                if (raw != null) {
                    for (String s : raw) {
                        ResourceLocation rl = ResourceLocation.tryParse(s);
                        if (rl != null) {
                            parsed.add(rl);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // A broken list must not take the game down - ship nothing removed instead.
            parsed.clear();
        }
        return parsed;
    }

    @SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        // Copy first: remove() mutates the very sets we are walking.
        List<ItemStack> entries = new ArrayList<>(event.getParentEntries());
        entries.addAll(event.getSearchEntries());

        if (tabs().contains(event.getTabKey().location())) {
            // Whole tab: take every stack, components and all, without inspecting item ids.
            for (ItemStack stack : entries) {
                if (!stack.isEmpty()) {
                    event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
            }
            return;
        }

        for (ItemStack stack : entries) {
            if (isRemovedPotion(stack)) {
                event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }

        Set<ResourceLocation> removed = ids();
        if (removed.isEmpty()) {
            return;
        }
        for (ItemStack stack : entries) {
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            if (removed.contains(BuiltInRegistries.ITEM.getKey(item))) {
                event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }

    /**
     * Component-aware removal covers every bottle and tipped-arrow form without removing their
     * shared vanilla item. Registry entries remain available for loading existing saves.
     */
    public static boolean isRemovedPotion(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        if (contents.potion().map(RemovedItems::isRemovedPotion).orElse(false)) return true;
        for (var effect : contents.getAllEffects()) {
            if (isRemovedEffect(effect.getEffect())) return true;
        }
        return false;
    }

    public static boolean isRemovedPotion(Holder<Potion> potion) {
        if (potions == null) potions = read("/removed_potions.json");
        if (potion.unwrapKey().map(key -> potions.contains(key.location())).orElse(false)) return true;
        return potion.value().getEffects().stream().anyMatch(effect -> isRemovedEffect(effect.getEffect()));
    }

    public static boolean isRemovedEffect(Holder<MobEffect> effect) {
        return effect.unwrapKey()
                .map(key -> removedEffectIds().contains(key.location()))
                .orElse(false);
    }

    public static Set<ResourceLocation> removedEffectIds() {
        if (effects == null) effects = Set.copyOf(read("/removed_effects.json"));
        return effects;
    }

    private RemovedItems() {}
}
