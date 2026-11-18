package io.github.bertie_mc.bertieprogression.gametest;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import io.github.bertie_mc.bertieprogression.ModItems;
import io.github.bertie_mc.bertieprogression.cybernetics.CenterEnergyAccess;
import io.github.bertie_mc.bertieprogression.cybernetics.CopperAugmentation;
import io.github.bertie_mc.bertieprogression.cybernetics.MechanicalAdapterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class VacuumAdapterGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void bottleCollectionRespectsAtmosphereAndAirZones(GameTestHelper helper) throws Exception {
        var overworld = helper.getLevel().getServer().overworld();
        var moon = overworld;
        var profile = new GameProfile(UUID.fromString("bc81736d-bf9b-4fd4-a57e-563309857948"), "VacuumTest");
        var normal = FakePlayerFactory.get(overworld, profile);
        normal.setGameMode(GameType.SURVIVAL);
        normal.setPos(1000.5, 180, 1000.5);
        normal.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
        var breathable = new PlayerInteractEvent.RightClickItem(normal, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(breathable);
        check(!breathable.isCanceled() && normal.getMainHandItem().getCount() == 2, "Normal air produced Nothing");
        try (var vacuum = vacuumEnvironment(overworld)) {
            var player = FakePlayerFactory.get(moon, profile);
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(1000.5, 180, 1000.5);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
            var exposed = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
            NeoForge.EVENT_BUS.post(exposed);
            check(
                    exposed.isCanceled()
                            && player.getMainHandItem().getCount() == 1
                            && player.getInventory().countItem(ModItems.BOTTLE_OF_NOTHING.get()) == 1,
                    "Vacuum bottle count incorrect");
            var zonesClass = Class.forName("me.moltea.unbound.life.AirZones");
            var zones = zonesClass
                    .getMethod("of", net.minecraft.server.level.ServerLevel.class)
                    .invoke(null, moon);
            var zoneClass = Class.forName("me.moltea.unbound.life.AirZones$Zone");
            var sphere = Class.forName("me.moltea.unbound.life.AirZones$Sphere")
                    .getConstructor(Vec3.class, double.class)
                    .newInstance(player.getEyePosition(), 8d);
            var anchor = player.blockPosition();
            zonesClass
                    .getMethod("hold", net.minecraft.server.level.ServerLevel.class, BlockPos.class, zoneClass)
                    .invoke(zones, moon, anchor, sphere);
            var protectedAir = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
            NeoForge.EVENT_BUS.post(protectedAir);
            zonesClass.getMethod("close", BlockPos.class).invoke(zones, anchor);
            check(
                    !protectedAir.isCanceled() && player.getMainHandItem().getCount() == 1,
                    "Pressurized habitat produced Nothing");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 140)
    public static void copperFormationAndRealCreateInputsWorkInFourDirections(GameTestHelper helper) {
        var level = helper.getLevel();
        var centers = new ArrayList<AugmentApplicationEntity>();
        var adapters = new ArrayList<BlockPos>();
        var player = FakePlayerFactory.get(
                level, new GameProfile(UUID.fromString("be67993b-9889-4918-818f-b7c0d085bb4a"), "AdapterTest"));
        player.setGameMode(GameType.SURVIVAL);
        int i = 0;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var pos = helper.absolutePos(new BlockPos(10 + i++ * 12, 2, 10));
            var centerBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("oritech:augment_application_block"));
            level.setBlockAndUpdate(
                    pos, centerBlock.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
            var center = (AugmentApplicationEntity) level.getBlockEntity(pos);
            for (var support : CopperAugmentation.supports(center))
                level.setBlockAndUpdate(support, CopperAugmentation.casing().defaultBlockState());
            check(
                    center.initMultiblock(center.getBlockState()) && CopperAugmentation.intact(center),
                    "Copper center did not form");
            for (var support : CopperAugmentation.supports(center)) {
                check(
                        level.getBlockEntity(support) instanceof MachineCoreEntity core
                                && core.getControllerPos().equals(pos),
                        "Core not linked");
                check(
                        !level.getBlockState(support).is(CopperAugmentation.casing()),
                        "Ordinary casing clips through center");
            }
            var rear = facing.getOpposite();
            level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
            var adapter = pos.relative(rear, 2);
            level.setBlockAndUpdate(adapter.below(), Blocks.STONE.defaultBlockState());
            player.setYRot(rear.toYRot() + 180);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MechanicalAdapterRegistry.BLOCK.get()));
            var placement = new BlockPlaceContext(
                    player,
                    InteractionHand.MAIN_HAND,
                    player.getMainHandItem(),
                    new BlockHitResult(Vec3.atCenterOf(adapter.below()), Direction.UP, adapter.below(), false));
            ((BlockItem) player.getMainHandItem().getItem()).place(placement);
            check(
                    level.getBlockState(adapter).is(MechanicalAdapterRegistry.BLOCK.get())
                            && level.getBlockState(adapter.above()).isAir(),
                    "Controller placed both halves");
            level.setBlockAndUpdate(adapter.above(), CopperAugmentation.casing().defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var form = new PlayerInteractEvent.RightClickBlock(
                    player,
                    InteractionHand.MAIN_HAND,
                    adapter,
                    new BlockHitResult(Vec3.atCenterOf(adapter), Direction.UP, adapter, false));
            NeoForge.EVENT_BUS.post(form);
            check(
                    form.isCanceled() && level.getBlockState(adapter).getValue(MultiblockMachine.ASSEMBLED),
                    "Controller plus one casing did not form");
            check(
                    level.getBlockState(adapter).getValue(BlockStateProperties.HORIZONTAL_FACING) == rear,
                    "Adapter placement facing incorrect");
            for (var socket : List.of(adapter, adapter.above())) {
                level.setBlockAndUpdate(
                        socket.relative(rear),
                        AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, rear.getAxis()));
                level.setBlockAndUpdate(
                        socket.relative(rear, 2),
                        AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, facing));
            }
            for (var support : CopperAugmentation.supports(center)) {
                var core = level.getBlockEntity(support);
                var saved = core.saveWithFullMetadata(level.registryAccess());
                check(
                        saved.getString("id").equals("bertieprogression:assembled_copper_core"),
                        "Copper core saved with the wrong registry type");
                var restored = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                        support, level.getBlockState(support), saved, level.registryAccess());
                check(
                        restored instanceof MachineCoreEntity nativeCore
                                && nativeCore.getControllerPos().equals(pos),
                        "Copper core lost its controller on reload");
            }
            centers.add(center);
            adapters.add(adapter);
        }
        helper.runAtTickTime(50, () -> {
            for (var center : centers)
                check(
                        ((CenterEnergyAccess) center)
                                        .bertieprogression$internalEnergy()
                                        .getAmount()
                                > 0,
                        "Real dual Create inputs did not charge " + center.getBlockState());
            for (int n = 0; n < adapters.size(); n++) {
                level.destroyBlock(adapters.get(n).above(), false);
                ((CenterEnergyAccess) centers.get(n))
                        .bertieprogression$internalEnergy()
                        .setAmount(0);
            }
        });
        helper.runAtTickTime(60, () -> {
            for (var center : centers) {
                check(
                        ((CenterEnergyAccess) center)
                                        .bertieprogression$internalEnergy()
                                        .getAmount()
                                == 0,
                        "Broken upper input still charged");
                center.onControllerBroken();
                level.removeBlock(center.getBlockPos(), false);
            }
        });
        helper.runAtTickTime(65, () -> {
            for (var center : centers)
                for (var support : CopperAugmentation.supports(center))
                    check(
                            level.getBlockState(support).is(CopperAugmentation.casing()),
                            "Dismantling did not restore Copper Casing");
            helper.succeed();
        });
    }

    /** Temporarily apply a native airless environment without requiring additional GameTest levels. */
    private static AutoCloseable vacuumEnvironment(net.minecraft.server.level.ServerLevel level) throws Exception {
        Class<?> data = Class.forName("me.moltea.unbound.mc.env.EnvData");
        var field = data.getDeclaredField("server");
        field.setAccessible(true);
        var generation = data.getDeclaredField("generation");
        generation.setAccessible(true);
        Object previous = field.get(null);
        var components = previous.getClass().getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            var getter = components[i].getAccessor();
            getter.setAccessible(true);
            args[i] = getter.invoke(previous);
            types[i] = components[i].getType();
            if (components[i].getName().equals("dimensions")) {
                var dimensions = new java.util.HashMap<>((java.util.Map<?, ?>) args[i]);
                Class<?> dimension = Class.forName("me.moltea.unbound.mc.env.EnvData$Dimension");
                var constructor = dimension.getDeclaredConstructor(java.util.Map.class, java.util.List.class);
                constructor.setAccessible(true);
                var key = me.moltea.unbound.registry.ModAttributes.BREATHABLE.id();
                ((java.util.Map) dimensions)
                        .put(
                                level.dimension().location(),
                                constructor.newInstance(java.util.Map.of(key, false), java.util.List.of()));
                args[i] = dimensions;
            }
        }
        var constructor = previous.getClass().getDeclaredConstructor(types);
        constructor.setAccessible(true);
        field.set(null, constructor.newInstance(args));
        generation.setInt(null, generation.getInt(null) + 1);
        return () -> {
            field.set(null, previous);
            generation.setInt(null, generation.getInt(null) + 1);
        };
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new net.minecraft.gametest.framework.GameTestAssertException(message);
    }

    private VacuumAdapterGameTests() {}
}
