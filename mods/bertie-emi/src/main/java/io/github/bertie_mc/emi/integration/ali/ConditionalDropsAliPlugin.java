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
 * Publishes onto Advanced Loot Info's mob pages the drops that come from global loot modifiers whose
 * type ALI cannot read. A modifier declares its own serializer, and ALI can only decode the ones it
 * knows; the rest it lists as an unnamed GLM slot, which says a modifier applies but not what it
 * gives. Several mods in the pack write their mob drops that way.
 *
 * <p>These belong on the mob's own drop page rather than on an Info page of their own: the drop page
 * is where a player looks, it already shows every other drop the mob has, and it renders the count
 * and the conditions in the same shape as vanilla loot.
 *
 * <p>Each entry carries the condition the modifier actually tests, because that condition is the
 * whole of how the item is obtained — a Wither killed by a projectile, a spider cut with a Farmer's
 * Delight knife, a mob killed while it burns.
 *
 * <p>Discovery mirrors EMI's: {@link AliEntrypoint} is a CLASS-retention (RuntimeInvisible)
 * annotation that ALI finds by scanning — do NOT give it {@code @Retention(RUNTIME)}. Nothing here
 * touches a third-party class: items and entities are resolved by id, so a mod being absent costs
 * only its own entries.
 */
@AliEntrypoint
public class ConditionalDropsAliPlugin implements IPlugin {

    /** One drop: which mob gives it, what comes out, how much, and what the kill has to look like. */
    private record Drop(String mod, String entity, String item, float chance, int min, int max, String note) {}

    private static final String KNIFE = "Kill with a Farmer's Delight knife in hand";

    private static final String BURNING = "Kill it while it is on fire";

    private static final List<Drop> DROPS = List.of(
            // L2 Complements: a material per mob, each gated on how the mob dies.
            drop("l2complements", "piglin_brute", "l2complements:blackstone_core", null),
            drop("l2complements", "ghast", "l2complements:soul_flame", null),
            drop("l2complements", "warden", "l2complements:warden_bone_shard", "Killed by a player"),
            drop("l2complements", "wither", "l2complements:force_field", "Killed by a projectile"),
            drop("l2complements", "elder_guardian", "l2complements:guardian_eye", "Killed by lightning"),
            drop("l2complements", "drowned", "l2complements:hard_ice", "Killed by freezing"),
            drop("l2complements", "phantom", "l2complements:storm_core", "Killed by an explosion"),

            // Apothic Enchanting hard-codes this one against the warden's table.
            new Drop(
                    "apothic_enchanting",
                    "warden",
                    "apothic_enchanting:warden_tendril",
                    1.0F,
                    1,
                    2,
                    "The second is a 10% roll, +10 points per Looting level"),

            // Miner's Delight butchering: a knife, and a second form for a burning mob.
            drop("minersdelight", "bat", "minersdelight:bat_wing", KNIFE + "; half as often without one"),
            drop("minersdelight", "bat", "minersdelight:smoked_bat_wing", KNIFE + ", " + BURNING),
            drop("minersdelight", "bat", "minersdelight:baked_bat_wing", BURNING + " without a knife, half the time"),
            chance("minersdelight", "spider", "minersdelight:spider_leg", 0.4F, 0, 4, KNIFE + "; +Looting"),
            chance("minersdelight", "spider", "minersdelight:baked_spider_leg", 0.4F, 0, 4, KNIFE + ", " + BURNING),
            range("minersdelight", "spider", "minersdelight:arthropod", 1, 3, KNIFE),
            range("minersdelight", "cave_spider", "minersdelight:arthropod", 1, 3, KNIFE),
            range("minersdelight", "spider", "minersdelight:cooked_arthropod", 1, 3, KNIFE + ", " + BURNING),
            range("minersdelight", "cave_spider", "minersdelight:cooked_arthropod", 1, 3, KNIFE + ", " + BURNING),
            drop("minersdelight", "bee", "minersdelight:arthropod", KNIFE),
            drop("minersdelight", "silverfish", "minersdelight:arthropod", KNIFE),
            drop("minersdelight", "endermite", "minersdelight:arthropod", KNIFE),
            range("minersdelight", "squid", "minersdelight:tentacles", 1, 8, KNIFE),
            range("minersdelight", "glow_squid", "minersdelight:tentacles", 1, 8, KNIFE),
            range("minersdelight", "squid", "minersdelight:baked_tentacles", 1, 8, KNIFE + ", " + BURNING),
            range("minersdelight", "glow_squid", "minersdelight:baked_tentacles", 1, 8, KNIFE + ", " + BURNING),
            drop("minersdelight", "squid", "minersdelight:squid", KNIFE + "; also a 5% fishing catch"),
            drop("minersdelight", "glow_squid", "minersdelight:glow_squid", KNIFE + "; also a 1% fishing catch"),
            drop("minersdelight", "squid", "minersdelight:baked_squid", KNIFE + ", " + BURNING),
            drop("minersdelight", "glow_squid", "minersdelight:baked_squid", KNIFE + ", " + BURNING),
            chance(
                    "minersdelight",
                    "silverfish",
                    "minersdelight:silverfish_eggs",
                    0.2F,
                    1,
                    1,
                    KNIFE + "; 10% without one"),
            chance("minersdelight", "spider", "minecraft:cobweb", 0.2F, 1, 1, KNIFE),
            chance("minersdelight", "cave_spider", "minecraft:cobweb", 0.2F, 1, 1, KNIFE),

            // My Nether's Delight butchering, on the same pattern.
            chance("mynethersdelight", "hoglin", "mynethersdelight:hoglin_hide", 0.35F, 1, 1, KNIFE + "; +Looting"),
            chance("mynethersdelight", "strider", "mynethersdelight:strider_rock", 0.3F, 1, 1, KNIFE + "; +Looting"));

    private static Drop drop(String mod, String entity, String item, String note) {
        return new Drop(mod, entity, item, 1.0F, 1, 1, note);
    }

    private static Drop range(String mod, String entity, String item, int min, int max, String note) {
        return new Drop(mod, entity, item, 1.0F, min, max, note);
    }

    private static Drop chance(String mod, String entity, String item, float chance, int min, int max, String note) {
        return new Drop(mod, entity, item, chance, min, max, note);
    }

    @Override
    public String getModId() {
        return "bertieemi";
    }

    @Override
    public void registerServer(IServerRegistry registry) {
        registry.registerLootModifiers(utils -> build());
    }

    private static List<ILootModifier<?>> build() {
        List<ILootModifier<?>> modifiers = new ArrayList<>();
        for (Drop drop : DROPS) {
            try {
                if (!ModList.get().isLoaded(drop.mod())) {
                    continue;
                }
                EntityType<?> entity = entityType(drop.entity());
                ItemStack stack = stack(drop.item());
                if (entity == null || stack.isEmpty()) {
                    continue;
                }
                IDataNode node = new ItemNode(
                        drop.chance(),
                        new RangeValue(drop.min(), drop.max()),
                        stack,
                        drop.note() == null
                                ? TooltipBuilder.empty().build()
                                : TooltipBuilder.value(drop.note()).build(),
                        List.of(),
                        List.of());
                modifiers.add(new ConditionalDrop(entity, List.of(new IOperation.AddOperation(any -> true, node))));
            } catch (Throwable ignored) {
                // one entry must never take ALI's own pages down with it
            }
        }
        return modifiers;
    }

    /**
     * {@code ENTITY_TYPE} is a defaulted registry, so a plain get on a missing id answers pig and
     * would hang the drop on the wrong mob.
     */
    private static EntityType<?> entityType(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", path);
        return BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? BuiltInRegistries.ENTITY_TYPE.get(id) : null;
    }

    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** One extra drop on one entity type. */
    private record ConditionalDrop(EntityType<?> type, List<IOperation> operations) implements ILootModifier<Entity> {

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
