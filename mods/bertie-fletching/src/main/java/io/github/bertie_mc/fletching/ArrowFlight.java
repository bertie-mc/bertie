package io.github.bertie_mc.fletching;

import com.fletchery.mod.entity.CustomArrowEntity;
import io.github.bertie_mc.fletching.mixin.ArrowAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class ArrowFlight {
    private static final TagKey<net.minecraft.world.level.block.Block> ORES =
            TagKey.create(Registries.BLOCK, ResourceLocation.parse("c:ores"));

    private ArrowFlight() {}

    public static boolean beforeTick(CustomArrowEntity arrow) {
        var state = ((ArrowRuntime) arrow).bertie$flight();
        var p = state.profile;
        if (!state.started) {
            state.started = true;
            if (!arrow.level().isClientSide)
                arrow.setDeltaMovement(arrow.getDeltaMovement().scale(p.speed(arrow.level())));
        }
        arrow.setNoGravity(p.gravity() == 0);
        state.age++;
        state.peak = Math.max(state.peak, arrow.getY());
        if (state.age >= 1200 || (p.shaft("breeze") && state.age >= 300)) {
            arrow.discard();
            return true;
        }
        if (arrow.level().isClientSide) return false;
        if (state.originalDirection.lengthSqr() < .5)
            state.originalDirection = arrow.getDeltaMovement().normalize();
        if (state.remainingRange >= 0 && state.remainingRange <= .01) {
            arrow.discard();
            return true;
        }
        if (p.homing() && arrow.getDeltaMovement().lengthSqr() > .001) {
            Vec3 originalEnd = arrow.position()
                    .add(arrow.getDeltaMovement()
                            .normalize()
                            .scale(p.hitscan() ? 128 : arrow.getDeltaMovement().length()));
            // Never pull a shot off a target already intersected by its current segment.
            if (rayHits(arrow, arrow.position(), originalEnd).stream()
                    .noneMatch(hit -> !state.hit.contains(
                            ArrowDamage.living(hit.getEntity()).getUUID())))
                homingTarget(arrow).ifPresent(target -> {
                    Vec3 velocity = arrow.getDeltaMovement();
                    Vec3 aim = target.getBoundingBox().getCenter().subtract(arrow.position());
                    arrow.setDeltaMovement(
                            p.hitscan()
                                    ? aim.normalize().scale(velocity.length())
                                    : Homing.turn(velocity, aim, p.gravity() != 0));
                });
        }
        if (p.hitscan()) {
            scan(arrow);
            return true;
        }
        if (state.remainingRange >= 0 && arrow.getDeltaMovement().length() > state.remainingRange)
            arrow.setDeltaMovement(arrow.getDeltaMovement().normalize().scale(state.remainingRange));
        preparePath(arrow, arrow.position(), arrow.position().add(arrow.getDeltaMovement()));
        return false;
    }

    public static void afterTick(CustomArrowEntity arrow, Vec3 start, Vec3 end) {
        var state = ((ArrowRuntime) arrow).bertie$flight();
        if (!arrow.level().isClientSide && state.profile.tip("eldritch")) area(arrow, start, end);
        state.travelledTo(state.redirectStart == null ? end : state.redirectStart);
        if (state.remainingRange >= 0 && state.remainingRange <= .01) arrow.discard();
    }

    public static List<LivingEntity> targets(CustomArrowEntity arrow, Vec3 start, Vec3 end, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : arrow.level()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(start, end).inflate(radius + 2),
                        e -> ArrowDamage.eligible(arrow.getOwner(), e))) {
            Vec3 segment = end.subtract(start);
            double progress = segment.lengthSqr() < 1e-9
                    ? 0
                    : Math.clamp(
                            entity.getBoundingBox().getCenter().subtract(start).dot(segment) / segment.lengthSqr(),
                            0,
                            1);
            boolean inside = radius > 1
                    ? entity.getBoundingBox().getCenter().distanceToSqr(start.add(segment.scale(progress)))
                            <= radius * radius
                    : entity.getBoundingBox().inflate(radius).clip(start, end).isPresent()
                            || entity.getBoundingBox().inflate(radius).contains(start);
            if (inside) result.add(entity);
        }
        result.sort(Comparator.comparingDouble(e -> e.distanceToSqr(start)));
        return result;
    }

    public static java.util.Optional<LivingEntity> nearest(CustomArrowEntity arrow, Vec3 point, double radius) {
        var state = ((ArrowRuntime) arrow).bertie$flight();
        return ArrowEffects.nearby(arrow, point, radius).stream()
                .filter(e -> !state.hit.contains(e.getUUID()))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(point)));
    }

    public static java.util.Optional<LivingEntity> homingTarget(CustomArrowEntity arrow) {
        var state = ((ArrowRuntime) arrow).bertie$flight();
        double radius = state.remainingRange < 0 ? 24 : Math.min(24, state.remainingRange);
        return ArrowEffects.nearby(arrow, arrow.position(), radius).stream()
                .filter(e -> !state.hit.contains(e.getUUID()))
                .filter(e -> Homing.inCone(
                        state.originalDirection, e.getBoundingBox().getCenter().subtract(arrow.position())))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(arrow)));
    }

    public static boolean ricochet(CustomArrowEntity arrow, Vec3 from) {
        var state = ((ArrowRuntime) arrow).bertie$flight();
        if (state.ricochets >= state.profile.ricochets()) return false;
        state.beginSecondary(from);
        state.travelledTo(from);
        if (state.remainingRange <= .01) return false;
        var next = nearest(arrow, from, Math.min(24, state.remainingRange));
        if (next.isEmpty()) return false;
        state.ricochets++;
        Vec3 direction = next.get().getBoundingBox().getCenter().subtract(from).normalize();
        arrow.setPos(from.add(direction.scale(.01)));
        state.originalDirection = direction;
        state.redirectStart = arrow.position();
        arrow.setDeltaMovement(direction.scale(arrow.getDeltaMovement().length()));
        ((ArrowRuntime) arrow).bertie$grounded(false);
        return true;
    }

    public static boolean block(CustomArrowEntity arrow, BlockHitResult hit) {
        if (!(arrow.level() instanceof ServerLevel level)) return false;
        var state = ((ArrowRuntime) arrow).bertie$flight();
        var p = state.profile;
        Vec3 point = hit.getLocation()
                .add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(.15));
        state.travelledTo(point);
        ArrowEffects.impact(arrow, point);
        if (p.extra("blaze_powder")) {
            for (BlockPos pos : BlockPos.betweenClosed(
                    hit.getBlockPos().offset(-2, -2, -2), hit.getBlockPos().offset(1, 1, 1))) {
                if (level.random.nextFloat() < .15f
                        && level.getBlockState(pos).isSolid()
                        && level.isEmptyBlock(pos.above()))
                    level.setBlockAndUpdate(pos.above(), Blocks.FIRE.defaultBlockState());
            }
        }
        boolean honey = p.extra("honey") && ArrowAccess.bertie$honey(level, hit.getBlockPos());
        if (p.extra("torch")) {
            BlockPos torch = hit.getBlockPos().relative(hit.getDirection());
            if (level.isEmptyBlock(torch) && Blocks.TORCH.defaultBlockState().canSurvive(level, torch))
                level.setBlockAndUpdate(torch, Blocks.TORCH.defaultBlockState());
        }
        if (p.extra("slime") && state.bounces > 0) {
            Vec3 velocity = arrow.getDeltaMovement();
            state.bounces--;
            arrow.setDeltaMovement(
                    switch (hit.getDirection().getAxis()) {
                        case X -> new Vec3(-velocity.x * .6, velocity.y, velocity.z);
                        case Y -> new Vec3(velocity.x, -velocity.y * .6, velocity.z);
                        case Z -> new Vec3(velocity.x, velocity.y, -velocity.z * .6);
                    });
            arrow.setPos(point);
            state.redirectStart = point;
            ((ArrowRuntime) arrow).bertie$grounded(false);
            return true;
        }
        if (ricochet(arrow, point)) return true;
        if (p.extra("tnt") || p.extra("dragon") || honey) {
            arrow.discard();
            return true;
        }
        return false;
    }

    private static void scan(CustomArrowEntity arrow) {
        var state = ((ArrowRuntime) arrow).bertie$flight();
        state.tracing = true;
        try {
            for (int branch = 0; branch <= state.profile.ricochets(); branch++) {
                Vec3 velocity = arrow.getDeltaMovement();
                Vec3 start = arrow.position(),
                        end =
                                start.add(arrow.getDeltaMovement()
                                        .normalize()
                                        .scale(state.remainingRange < 0 ? 128 : state.remainingRange));
                preparePath(arrow, start, end);
                BlockHitResult block = arrow.level()
                        .clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, arrow));
                if (!state.profile.phase() && block.getType() != HitResult.Type.MISS) end = block.getLocation();
                Vec3 last = end;
                for (EntityHitResult hit : rayHits(arrow, start, end)) {
                    LivingEntity target = ArrowDamage.living(hit.getEntity());
                    if (target == null || state.hit.contains(target.getUUID())) continue;
                    Vec3 point = hit.getLocation();
                    arrow.setPos(point);
                    arrow.setDeltaMovement(velocity);
                    ((ArrowAccess) arrow).bertie$hit(hit);
                    last = point;
                }
                if (state.profile.tip("eldritch")) area(arrow, start, end);
                ArrowEffects.sonic((ServerLevel) arrow.level(), start, end);
                if (block.getType() != HitResult.Type.MISS && !state.profile.phase()) ArrowEffects.impact(arrow, end);
                arrow.setDeltaMovement(velocity);
                if (!ricochet(arrow, last)) break;
            }
        } finally {
            state.tracing = false;
            arrow.discard();
        }
    }

    public static List<EntityHitResult> rayHits(CustomArrowEntity arrow, Vec3 start, Vec3 end) {
        var hits = new ArrayList<EntityHitResult>();
        // Level's entity query includes multipart hitboxes, as vanilla projectile collision does.
        for (var entity : arrow.level().getEntities(arrow, new AABB(start, end).inflate(1), e -> {
            LivingEntity target = ArrowDamage.living(e);
            return target != null
                    && !e.isSpectator()
                    && e.isPickable()
                    && ArrowDamage.eligible(arrow.getOwner(), target);
        })) {
            var box = entity.getBoundingBox().inflate(.3);
            Vec3 point = box.contains(start) ? start : box.clip(start, end).orElse(null);
            if (point != null) hits.add(new EntityHitResult(entity, point));
        }
        hits.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(start)));
        return hits;
    }

    private static void area(CustomArrowEntity arrow, Vec3 start, Vec3 end) {
        var state = ((ArrowRuntime) arrow).bertie$flight();
        for (LivingEntity target : targets(arrow, start, end, 6)) {
            if (state.hit.contains(target.getUUID()) || !state.areaHit.add(target.getUUID())) continue;
            float raw = ArrowDamage.estimate(arrow, target);
            if (ArrowDamage.secondary(arrow.getCustomProperties(), arrow.getOwner(), target, raw, "secondary"))
                ArrowDamage.execute(arrow.getCustomProperties(), arrow.getOwner(), target);
        }
    }

    private static void preparePath(CustomArrowEntity arrow, Vec3 start, Vec3 end) {
        if (!(arrow.level() instanceof ServerLevel level)) return;
        var state = ((ArrowRuntime) arrow).bertie$flight();
        var p = state.profile;
        if (!p.extra("earth")
                && !p.extra("sponge")
                && !p.extra("torch")
                && !p.extra("blaze_powder")
                && !p.tip("amethyst")) return;
        int steps = Math.max(1, Math.min(1024, (int) Math.ceil(start.distanceTo(end) * 4)));
        var visited = new HashSet<BlockPos>();
        for (int i = 0; i <= steps; i++) {
            BlockPos pos = BlockPos.containing(start.lerp(end, (double) i / steps));
            if (!visited.add(pos) || !level.hasChunkAt(pos)) continue;
            BlockState block = level.getBlockState(pos);
            boolean ore = block.is(ORES);
            if (p.extra("earth") && ore && !p.phase()) break;
            boolean glass = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(block.getBlock())
                    .getPath()
                    .contains("glass");
            if ((p.extra("earth") && !ore && state.drilled < 12
                            || p.tip("amethyst") && (block.is(BlockTags.LEAVES) || glass))
                    && !block.isAir()
                    && !block.getCollisionShape(level, pos).isEmpty()) {
                if (breakBlock(arrow, level, pos, block)) state.drilled++;
            }
            if (p.extra("sponge")) {
                for (BlockPos wet : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
                    if (wet.distSqr(pos) > 4 || !level.hasChunkAt(wet)) continue;
                    var wetState = level.getBlockState(wet);
                    if (wetState.is(Blocks.WATER) && wetState.getFluidState().isSource())
                        level.setBlockAndUpdate(wet, Blocks.AIR.defaultBlockState());
                    else if (wetState.hasProperty(BlockStateProperties.WATERLOGGED)
                            && wetState.getValue(BlockStateProperties.WATERLOGGED))
                        level.setBlockAndUpdate(wet, wetState.setValue(BlockStateProperties.WATERLOGGED, false));
                }
            }
            if (p.extra("blaze_powder"))
                for (BlockPos snow : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2)))
                    if (level.getBlockState(snow).is(Blocks.SNOW)) level.destroyBlock(snow, false, arrow.getOwner());
            if (p.extra("torch") && level.isEmptyBlock(pos))
                TrailLights.get(level).place(level, pos);
            if (!p.phase()
                    && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) break;
        }
    }

    private static boolean breakBlock(CustomArrowEntity arrow, ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getDestroySpeed(level, pos) < 0) return false;
        if (arrow.getOwner() instanceof Player player) {
            if (!level.mayInteract(player, pos)
                    || NeoForge.EVENT_BUS
                            .post(new BlockEvent.BreakEvent(level, pos, state, player))
                            .isCanceled()) return false;
        } else if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
        return level.destroyBlock(pos, true, arrow.getOwner());
    }
}
