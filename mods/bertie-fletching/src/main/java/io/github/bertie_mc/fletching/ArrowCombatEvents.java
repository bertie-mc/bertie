package io.github.bertie_mc.fletching;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class ArrowCombatEvents {
    private static final String LINKS = "BertieArrowLinks", DELAYED = "BertieArrowDelayed", KILL = "BertieArrowKill";

    private ArrowCombatEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST, ArrowCombatEvents::incoming);
        bus.addListener(ArrowCombatEvents::post);
        bus.addListener(ArrowCombatEvents::shield);
        bus.addListener(EventPriority.LOWEST, ArrowCombatEvents::death);
        bus.addListener(EventPriority.LOWEST, ArrowCombatEvents::drops);
        bus.addListener(EventPriority.LOWEST, ArrowCombatEvents::experience);
        bus.addListener(ArrowCombatEvents::tick);
    }

    private static void incoming(LivingIncomingDamageEvent event) {
        var context = ArrowDamage.CURRENT.get();
        if (context != null)
            event.addReductionModifier(
                    DamageContainer.Reduction.ARMOR,
                    (container, reduction) ->
                            reduction * (1 - context.profile.tip().penetration()));
        if (event.getEntity().hasEffect(ArrowStatus.VULNERABLE)
                && event.getSource().is(DamageTypeTags.IS_PROJECTILE)) event.setAmount(event.getAmount() * 1.3f);
    }

    private static void shield(LivingShieldBlockEvent event) {
        var context = ArrowDamage.CURRENT.get();
        if (context != null && context.profile.shaft("ender")) event.setBlocked(false);
    }

    private static void post(LivingDamageEvent.Post event) {
        var context = ArrowDamage.CURRENT.get();
        if (context != null
                && (event.getNewDamage() > 0 || event.getReduction(DamageContainer.Reduction.ABSORPTION) > 0))
            context.damaged = true;
        if (ArrowDamage.SHARING.get()
                || CombatBridge.redirected(event.getSource())
                || event.getOriginalDamage() <= 0
                || !(event.getEntity().level() instanceof ServerLevel level)) return;
        LivingEntity victim = event.getEntity();
        prune(victim);
        ListTag links = victim.getPersistentData().getList(LINKS, 10).copy();
        ArrowDamage.SHARING.set(true);
        try {
            for (var entry : links) {
                CompoundTag link = (CompoundTag) entry;
                if (level.getEntity(link.getUUID("Peer")) instanceof LivingEntity peer && peer.isAlive()) {
                    DamageSource shared = CombatBridge.transfer(event.getSource());
                    int immunity = peer.invulnerableTime;
                    peer.invulnerableTime = 0;
                    try {
                        if (context == null) peer.hurt(shared, event.getOriginalDamage() * .5f);
                        else
                            ArrowDamage.with(
                                    new ArrowDamage.Context(
                                            context.data, context.owner, event.getOriginalDamage() * .5f),
                                    () -> peer.hurt(shared, event.getOriginalDamage() * .5f));
                    } finally {
                        peer.invulnerableTime = Math.max(immunity, peer.invulnerableTime);
                    }
                }
            }
        } finally {
            ArrowDamage.SHARING.set(false);
        }
    }

    public static void link(LivingEntity a, LivingEntity b) {
        prune(a);
        prune(b);
        remove(a, b.getUUID());
        remove(b, a.getUUID());
        makeRoom(a);
        makeRoom(b);
        add(a, b);
        add(b, a);
    }

    private static void add(LivingEntity a, LivingEntity b) {
        ListTag list = a.getPersistentData().getList(LINKS, 10);
        CompoundTag link = new CompoundTag();
        link.putUUID("Peer", b.getUUID());
        link.putLong("Until", a.level().getGameTime() + 200);
        list.add(link);
        a.getPersistentData().put(LINKS, list);
    }

    private static void prune(LivingEntity target) {
        ListTag list = target.getPersistentData().getList(LINKS, 10);
        list.removeIf(value ->
                ((CompoundTag) value).getLong("Until") <= target.level().getGameTime());
        target.getPersistentData().put(LINKS, list);
    }

    private static void remove(LivingEntity target, UUID peer) {
        ListTag list = target.getPersistentData().getList(LINKS, 10);
        list.removeIf(value -> ((CompoundTag) value).getUUID("Peer").equals(peer));
        target.getPersistentData().put(LINKS, list);
    }

    private static void makeRoom(LivingEntity target) {
        ListTag list = target.getPersistentData().getList(LINKS, 10);
        while (list.size() >= 3) {
            CompoundTag oldest = (CompoundTag) list.stream()
                    .min(Comparator.comparingLong(value -> ((CompoundTag) value).getLong("Until")))
                    .orElseThrow();
            if (target.level() instanceof ServerLevel level
                    && level.getEntity(oldest.getUUID("Peer")) instanceof LivingEntity peer)
                remove(peer, target.getUUID());
            list.remove(oldest);
        }
    }

    public static void delay(
            LivingEntity target, CompoundTag data, net.minecraft.world.entity.Entity owner, float raw) {
        ListTag list = target.getPersistentData().getList(DELAYED, 10);
        CompoundTag task = new CompoundTag();
        task.putLong("At", target.level().getGameTime() + 40);
        task.putFloat("Damage", raw);
        task.put("Arrow", data.copy());
        if (owner != null) task.putUUID("Owner", owner.getUUID());
        list.add(task);
        target.getPersistentData().put(DELAYED, list);
    }

    private static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || !(target.level() instanceof ServerLevel level))
            return;
        if (target.hasEffect(ArrowStatus.FROZEN)) {
            var data = target.getPersistentData();
            if (!data.contains("BertieFrozenX")) {
                data.putDouble("BertieFrozenX", target.getX());
                data.putDouble("BertieFrozenY", target.getY());
                data.putDouble("BertieFrozenZ", target.getZ());
            }
            target.setDeltaMovement(Vec3.ZERO);
            double x = data.getDouble("BertieFrozenX"),
                    y = data.getDouble("BertieFrozenY"),
                    z = data.getDouble("BertieFrozenZ");
            if (target instanceof ServerPlayer player)
                player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
            else target.setPos(x, y, z);
        } else target.getPersistentData().remove("BertieFrozenX");
        ListTag tasks = target.getPersistentData().getList(DELAYED, 10);
        var due = new ArrayList<CompoundTag>();
        tasks.removeIf(value -> {
            CompoundTag task = (CompoundTag) value;
            if (task.getLong("At") <= level.getGameTime()) {
                due.add(task);
                return true;
            }
            return false;
        });
        for (CompoundTag task : due)
            ArrowDamage.secondary(
                    task.getCompound("Arrow"),
                    task.hasUUID("Owner") ? level.getEntity(task.getUUID("Owner")) : null,
                    target,
                    task.getFloat("Damage"),
                    "secondary");
        if (target.tickCount % 10 == 0) {
            prune(target);
            for (var entry : target.getPersistentData().getList(LINKS, 10)) {
                if (level.getEntity(((CompoundTag) entry).getUUID("Peer")) instanceof LivingEntity peer
                        && target.getId() < peer.getId()) {
                    Vec3 from = target.position().add(0, target.getBbHeight() * .5, 0),
                            to = peer.position().add(0, peer.getBbHeight() * .5, 0);
                    for (int i = 0; i < 8; i++) {
                        Vec3 point = from.lerp(to, i / 8.0);
                        level.sendParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    private static void death(LivingDeathEvent event) {
        var context = ArrowDamage.CURRENT.get();
        if (context == null || context.owner == null) {
            event.getEntity().getPersistentData().remove(KILL);
            return;
        }
        if (context.profile.extra("lapis") && context.owner instanceof ServerPlayer player)
            player.giveExperiencePoints(2);
        CompoundTag meta = new CompoundTag();
        meta.putUUID("Owner", context.owner.getUUID());
        meta.putDouble("XP", context.profile.experience());
        meta.putBoolean("Loot", context.profile.loot());
        meta.put("Arrow", context.data.copy());
        meta.putBoolean(
                "Recover",
                context.profile.shaft("leather")
                        && context.data.getInt("bertieChildDepth") == 0
                        && !context.data.getBoolean("bertieNoRecovery"));
        event.getEntity().getPersistentData().put(KILL, meta);
    }

    private static void experience(LivingExperienceDropEvent event) {
        CompoundTag meta = event.getEntity().getPersistentData().getCompound(KILL);
        if (meta.contains("XP"))
            event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * meta.getDouble("XP")));
    }

    private static void drops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        CompoundTag meta = event.getEntity().getPersistentData().getCompound(KILL);
        if (!meta.hasUUID("Owner")) return;
        if (meta.getBoolean("Recover") && level.random.nextFloat() < .3f) {
            ItemStack recovered = ArrowRecipe.fromData(meta.getCompound("Arrow"), 1, level.registryAccess());
            event.getDrops()
                    .add(new net.minecraft.world.entity.item.ItemEntity(
                            level,
                            event.getEntity().getX(),
                            event.getEntity().getY(),
                            event.getEntity().getZ(),
                            recovered));
        }
        var player = level.getServer().getPlayerList().getPlayer(meta.getUUID("Owner"));
        if (meta.getBoolean("Loot") && player != null) {
            var it = event.getDrops().iterator();
            while (it.hasNext()) {
                var drop = it.next();
                ItemStack stack = drop.getItem();
                insertLoot(player.getInventory(), stack);
                if (stack.isEmpty()) it.remove();
                else {
                    drop.setPos(player.position());
                    drop.setPickUpDelay(0);
                }
            }
        }
    }

    public static void insertLoot(net.minecraft.world.entity.player.Inventory inventory, ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++)
            for (int slot = 0; slot < 36 && !stack.isEmpty(); slot++) {
                ItemStack current = inventory.getItem(slot);
                if (pass == 0 && !current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack)) {
                    int count = Math.min(stack.getCount(), Math.max(0, current.getMaxStackSize() - current.getCount()));
                    current.grow(count);
                    stack.shrink(count);
                } else if (pass == 1 && current.isEmpty()) {
                    int count = Math.min(stack.getCount(), stack.getMaxStackSize());
                    inventory.setItem(slot, stack.copyWithCount(count));
                    stack.shrink(count);
                }
            }
        inventory.setChanged();
    }
}
