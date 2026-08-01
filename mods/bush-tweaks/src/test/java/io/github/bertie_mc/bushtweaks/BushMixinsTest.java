package io.github.bertie_mc.bushtweaks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BushMixinsTest {

    private static final List<String> SPEED_PATCHED_BLOCKS = List.of(
            "raspberrybush",
            "raspberry_bush_top",
            "raspberrybushnone",
            "raspberrybushnonetop",
            "strawberrybush",
            "strawberry_bush_none",
            "blueberry_bush_bottom",
            "blueberry_bush_top",
            "none_blueberry_bush_bottom",
            "none_blueberry_bush_top",
            "green_grape_tree_bottom",
            "green_grape_tree_top",
            "none_green_grape_tree_bottom",
            "none_green_grape_tree_top",
            "black_grape_tree_bottom",
            "black_grape_tree_top",
            "none_black_grape_tree_bottom",
            "none_black_grape_tree_top"
    );

    @Test
    void everyBerriesAndCherriesTargetUsesVanillaSpeed() {
        for (String path : SPEED_PATCHED_BLOCKS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("berries_and_cherries", path);
            assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "Missing target " + id);
            Block block = BuiltInRegistries.BLOCK.get(id);
            assertEquals(1.0F, block.getSpeedFactor(), "Unexpected speed factor for " + id);
        }
    }

    @Test
    void movingEntityTakesSweetBerryDamage() throws ReflectiveOperationException {
        for (String procedure : List.of("RaspberryBushDamageProcedure", "BlueberryBushDamageProcedure")) {
            ServerLevel level = damageLevel();
            LivingEntity entity = movingEntity(false);
            invokeProcedure(procedure, level, entity);
            verify(entity).hurt(level.damageSources().sweetBerryBush(), 1.0F);
        }
    }

    @Test
    void stationaryAndCrouchingEntitiesTakeNoDamage() throws ReflectiveOperationException {
        for (String procedure : List.of("RaspberryBushDamageProcedure", "BlueberryBushDamageProcedure")) {
            ServerLevel level = damageLevel();
            LivingEntity stationary = movingEntity(false);
            when(stationary.getX()).thenReturn(0.0D);
            when(stationary.getZ()).thenReturn(0.0D);
            invokeProcedure(procedure, level, stationary);
            verify(stationary, never()).hurt(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyFloat());

            LivingEntity crouching = movingEntity(true);
            invokeProcedure(procedure, level, crouching);
            verify(crouching, never()).hurt(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyFloat());
            verify(crouching).resetFallDistance();
        }
    }

    private static LivingEntity movingEntity(boolean crouching) {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getX()).thenReturn(0.01D);
        when(entity.getZ()).thenReturn(0.01D);
        when(entity.isSteppingCarefully()).thenReturn(crouching);
        entity.xOld = 0.0D;
        entity.zOld = 0.0D;
        return entity;
    }

    private static ServerLevel damageLevel() {
        ServerLevel level = mock(ServerLevel.class);
        DamageSources sources = mock(DamageSources.class);
        when(level.damageSources()).thenReturn(sources);
        when(sources.sweetBerryBush()).thenReturn(mock(DamageSource.class));
        return level;
    }

    private static void invokeProcedure(String simpleName, LevelAccessor level, Entity entity)
            throws ReflectiveOperationException {
        Class<?> procedure = Class.forName("net.mcreator.berriesandcherries.procedures." + simpleName);
        Method execute = procedure.getMethod("execute", LevelAccessor.class, Entity.class);
        try {
            execute.invoke(null, level, entity);
        } catch (InvocationTargetException exception) {
            throw new AssertionError(simpleName + " failed", exception.getCause());
        }
    }
}
