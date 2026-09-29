package io.github.bertie_mc.bertieprogression.combat;

import java.util.ArrayDeque;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class MagicOrigins {
    private record Origin(UUID caster, ResourceLocation school, Entity entity) {}

    private static final ThreadLocal<ArrayDeque<Origin>> ORIGINS = ThreadLocal.withInitial(ArrayDeque::new);

    private MagicOrigins() {}

    public static void begin(LivingEntity caster, ResourceLocation school) {
        ORIGINS.get().push(new Origin(caster.getUUID(), school, null));
    }

    public static void beginEntity(Entity entity) {
        MagicOrigin origin = (MagicOrigin) entity;
        ORIGINS.get().push(new Origin(origin.bertie$spellCaster(), origin.bertie$spellSchool(), entity));
    }

    public static void end() {
        var stack = ORIGINS.get();
        stack.pop();
        if (stack.isEmpty()) ORIGINS.remove();
    }

    public static boolean excludesProjectileDefense(Entity entity) {
        String namespace = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(entity.getType())
                .getNamespace();
        return ((MagicOrigin) entity).bertie$isSpellEntity()
                || namespace.equals("irons_spellbooks")
                || namespace.equals("fdbosses");
    }

    public static boolean isActiveSpellType(net.minecraft.world.entity.EntityType<?> type) {
        Origin origin = ORIGINS.get().peek();
        return origin != null && origin.entity() != null && origin.entity().getType() == type;
    }

    public static boolean isSpellDamage(DamageSource source) {
        if (source.getDirectEntity() instanceof MagicOrigin origin && origin.bertie$isSpellEntity()) return true;
        Origin active = ORIGINS.get().peek();
        if (active == null) return false;
        if (source.getEntity() != null && source.getEntity().getUUID().equals(active.caster())) return true;
        return active.entity() != null
                && (source.getDirectEntity() == active.entity()
                        || source.getDirectEntity() == null && source.getEntity() == null);
    }

    public static ResourceLocation school(DamageSource source) {
        if (source instanceof RoutedDamage routed && routed.bertie$school() != null) return routed.bertie$school();
        if (source.getDirectEntity() instanceof MagicOrigin origin && origin.bertie$isSpellEntity())
            return origin.bertie$spellSchool();
        Origin active = ORIGINS.get().peek();
        return active == null ? null : active.school();
    }

    @SubscribeEvent
    public static void spawned(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide()
                || entity instanceof LivingEntity
                || entity instanceof ItemEntity
                || entity instanceof ExperienceOrb) return;
        var data = entity.getPersistentData();
        if (data.hasUUID("BertieSpellCaster")) {
            ((MagicOrigin) entity)
                    .bertie$spellOrigin(
                            data.getUUID("BertieSpellCaster"),
                            ResourceLocation.tryParse(data.getString("BertieSpellSchool")));
            return;
        }
        Origin origin = ORIGINS.get().peek();
        if (origin == null || origin.caster() == null) return;
        if (entity instanceof TraceableEntity traceable
                && traceable.getOwner() instanceof LivingEntity owner
                && !owner.getUUID().equals(origin.caster())) return;
        ((MagicOrigin) entity).bertie$spellOrigin(origin.caster(), origin.school());
        data.putUUID("BertieSpellCaster", origin.caster());
        if (origin.school() != null)
            data.putString("BertieSpellSchool", origin.school().toString());
    }
}
