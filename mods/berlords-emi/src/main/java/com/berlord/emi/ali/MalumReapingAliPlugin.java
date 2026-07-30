package com.berlord.emi.ali;

import com.yanny.aci.api.RangeValue;
import com.yanny.aci.tooltip.TooltipBuilder;
import com.yanny.ali.api.AliEntrypoint;
import com.yanny.ali.api.IDataNode;
import com.yanny.ali.api.ILootModifier;
import com.yanny.ali.api.IOperation;
import com.yanny.ali.api.IPlugin;
import com.yanny.ali.api.IServerRegistry;
import com.yanny.ali.plugin.common.nodes.ItemNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Publishes Malum's <b>soul-reaping</b> drops onto Advanced Loot Info's mob-drop pages.
 *
 * <p>Malum does not use loot tables or global loot modifiers for these: it keeps a private registry
 * ({@code data/malum/reaping_data/*.json}) and spawns the items straight into the world from a
 * {@code LivingDeathEvent} handler. That makes them invisible to every loot viewer, ALI included, so
 * roughly 40 mobs' reaping drops (Astral Weave from ghasts/phantoms, Grim Talc from bogged, …) simply
 * never appeared anywhere in EMI. This plugin reads Malum's table and adds one entry per drop.
 *
 * <p>Discovery mirrors EMI's: {@link AliEntrypoint} is a CLASS-retention (RuntimeInvisible) annotation
 * that ALI finds by scanning — do NOT give it {@code @Retention(RUNTIME)}. This class is only ever
 * loaded by ALI's own scan, so it is safe when ALI is absent; Malum's classes are quarantined in
 * {@link MalumReapingSource}, reached only after the {@code malum} check below.
 *
 * <p>Note the drops are gated in-game on killing the mob while its soul is exposed (Malum's scythe
 * mechanic) — a plain-weapon kill drops nothing. That condition is not expressible as a loot
 * condition, so it is stated in each entry's tooltip instead.
 */
@AliEntrypoint
public class MalumReapingAliPlugin implements IPlugin {

    private static final String REAP_NOTE = "Soul Reaping: kill with the soul exposed (Malum scythe)";

    @Override
    public String getModId() {
        return "berlords_emi";
    }

    @Override
    public void registerServer(IServerRegistry registry) {
        if (!ModList.get().isLoaded("malum")) {
            return;
        }
        // Lazy: evaluated when ALI builds loot info, by which point Malum's datapack registry is filled.
        registry.registerLootModifiers(utils -> buildModifiers());
    }

    private static List<ILootModifier<?>> buildModifiers() {
        List<ILootModifier<?>> modifiers = new ArrayList<>();
        try {
            for (MalumReapingSource.Drop drop : MalumReapingSource.collect()) {
                EntityType<?> type = entityType(drop.entityId());
                if (type == null) {
                    continue; // reaping data for a mod that isn't installed
                }
                IDataNode node = new ItemNode(
                        drop.chance(),
                        new RangeValue(drop.min(), drop.max()),
                        drop.stack(),
                        TooltipBuilder.value(REAP_NOTE).build(),
                        List.of(),
                        List.of());
                modifiers.add(new ReapingModifier(type, List.of(new IOperation.AddOperation(stack -> true, node))));
            }
        } catch (Throwable ignored) {
            // never let this break ALI's own loot pages
        }
        return modifiers;
    }

    /**
     * Resolve an entity type, or null if this pack doesn't have it. Uses {@code containsKey} first
     * because {@code ENTITY_TYPE} is a DEFAULTED registry — a plain {@code get} on a missing id would
     * silently return the default (pig) and stick the drop on the wrong mob.
     */
    private static EntityType<?> entityType(ResourceLocation id) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? BuiltInRegistries.ENTITY_TYPE.get(id) : null;
    }

    /** Adds one reaping drop to every entity of a given type. */
    private record ReapingModifier(EntityType<?> type, List<IOperation> operations)
            implements ILootModifier<Entity> {

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
