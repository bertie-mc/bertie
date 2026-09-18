package io.github.bertie_mc.emi.integration.ali;

import com.yanny.aci.api.RangeValue;
import com.yanny.aci.tooltip.TooltipBuilder;
import com.yanny.ali.api.AliEntrypoint;
import com.yanny.ali.api.IDataNode;
import com.yanny.ali.api.ILootModifier;
import com.yanny.ali.api.IOperation;
import com.yanny.ali.api.IPlugin;
import com.yanny.ali.api.IServerRegistry;
import com.yanny.ali.plugin.common.nodes.ItemNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Armageddon's boss kills, on the bosses' own pages in Advanced Loot Info. The mod is built in
 * MCreator, so a death runs a generated procedure that spawns the relic, the lore scroll and the
 * treasure bag directly — there is no loot table and no modifier, so the mob pages were empty.
 *
 * <p>What comes out of the bags is a separate question, answered by the Treasure Bag category in
 * {@code ArmageddonEmiModule}; this is only the kill itself.
 *
 * <p>Discovery mirrors EMI's: {@link AliEntrypoint} is a CLASS-retention (RuntimeInvisible)
 * annotation that ALI finds by scanning — do NOT give it {@code @Retention(RUNTIME)}. Items and
 * entities resolve by id, so nothing here loads an Armageddon class.
 */
@AliEntrypoint
public class ArmageddonDropsAliPlugin implements IPlugin {

    private static final String MOD = "armageddon_mod";

    /** A boss and everything its death spawns. Every one of these is a certainty, one apiece. */
    private record BossDrop(String entity, List<String> items) {}

    private static final List<BossDrop> DROPS = List.of(
            new BossDrop(
                    MOD + ":arion_tyrant_of_the_emerald_wrath_soldat",
                    List.of("arion_relic", "arion_lore_scroll", "arion_treasurebag")),
            new BossDrop(
                    MOD + ":bringer_of_doom_p_2",
                    List.of(
                            "bringer_of_doom_relic",
                            "bringer_of_doom_lore_scroll",
                            "the_bringer_of_doom_treasure_bag")),
            new BossDrop(
                    MOD + ":eldoraththe_ancient_builder",
                    List.of("eldorath_relic", "eldorath_lore_scroll", "eldorath_treasure_bag")),
            new BossDrop(
                    MOD + ":elvenite_paladin",
                    List.of("elvenite_paladin_relic", "elvenite_paladin_lore_scroll", "elvenite_paladin_treasure_bag")),
            new BossDrop(
                    MOD + ":the_gobelin_lord",
                    List.of("goblin_lord_relic", "gobelin_lord_lore_scroll", "goblin_lord_treasure_bag")),
            new BossDrop(
                    MOD + ":the_iron_colossus",
                    List.of("iron_colossus_relic", "iron_colossus_lore_scroll", "iron_colossus_treasure_bag")),
            new BossDrop(
                    MOD + ":nyxaris_the_veil_of_oblivion",
                    List.of(
                            "nyxaris_the_veil_of_oblivion_relic",
                            "nyxaris_lore_scroll",
                            "nyxaris_the_veil_of_oblivion_treasure_bag")),
            new BossDrop(
                    MOD + ":sanghor_lord_of_bloodp_2",
                    List.of(
                            "sanghor_lord_of_the_blood_relic",
                            "sanghor_lord_of_blood_lore_scroll",
                            "sanghor_lord_of_blood_treasure_bag")),
            new BossDrop(
                    MOD + ":the_discord",
                    List.of(
                            "the_calamities_relic_1",
                            "the_discord_lore_scroll",
                            "the_famine_lore_scroll",
                            "the_chaos_lore_scroll",
                            "the_calamities_treasure_bag")),
            new BossDrop(
                    MOD + ":vaedricthe_fallen_wanderer",
                    List.of("vaedric_the_fallen_wanderer_relic", "vaedric_lore_scroll", "vaedric_treasure_bag")),
            new BossDrop(
                    MOD + ":zoranththe_forgotten_one",
                    List.of("zoranth_relic", "zoranth_lore_scroll", "zoranth_treasure_bag")),
            new BossDrop(
                    MOD + ":zoranth_newborn_of_the_zenith",
                    List.of(
                            "zoranth_newborn_of_the_zenith_relic",
                            "zoranth_newborn_of_the_zenith_lore_scroll",
                            "zoranth_newborn_of_the_zenith_treasure_bag")),
            new BossDrop(MOD + ":little_sword_goblin", List.of("gilded_nugget")),
            // The two the mod hangs off vanilla bosses.
            new BossDrop("minecraft:elder_guardian", List.of("elder_guardian_treasurebag")),
            new BossDrop("minecraft:ender_dragon", List.of("ender_dragon_treasurebag")));

    @Override
    public String getModId() {
        return "bertieemi";
    }

    @Override
    public void registerServer(IServerRegistry registry) {
        if (!ModList.get().isLoaded(MOD)) {
            return;
        }
        registry.registerLootModifiers(utils -> build());
    }

    private static List<ILootModifier<?>> build() {
        List<ILootModifier<?>> modifiers = new ArrayList<>();
        for (BossDrop drop : DROPS) {
            try {
                EntityType<?> entity = entityType(drop.entity());
                if (entity == null) {
                    continue;
                }
                List<IOperation> operations = new ArrayList<>();
                for (String id : drop.items()) {
                    ItemStack stack = stack(MOD + ":" + id);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    IDataNode node = new ItemNode(
                            1.0F,
                            new RangeValue(1, 1),
                            stack,
                            TooltipBuilder.empty().build(),
                            List.of(),
                            List.of());
                    operations.add(new IOperation.AddOperation(any -> true, node));
                }
                if (!operations.isEmpty()) {
                    modifiers.add(new BossKill(entity, operations));
                }
            } catch (Throwable ignored) {
                // one boss must never take ALI's own pages down with it
            }
        }
        return modifiers;
    }

    /**
     * {@code ENTITY_TYPE} is a defaulted registry, so a plain get on a missing id answers pig and
     * would hang the drops on the wrong mob.
     */
    private static EntityType<?> entityType(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        return BuiltInRegistries.ENTITY_TYPE.containsKey(key) ? BuiltInRegistries.ENTITY_TYPE.get(key) : null;
    }

    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private record BossKill(EntityType<?> type, List<IOperation> operations) implements ILootModifier<Entity> {

        @Override
        public boolean predicate(Entity entity) {
            return entity != null && entity.getType() == type;
        }

        @Override
        public List<IOperation> getOperations() {
            return operations;
        }

        @Override
        public IType<Entity> getType() {
            return IType.ENTITY;
        }
    }
}
