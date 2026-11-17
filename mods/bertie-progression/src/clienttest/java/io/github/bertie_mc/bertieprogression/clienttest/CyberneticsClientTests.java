package io.github.bertie_mc.bertieprogression.clienttest;

import com.simibubi.create.AllBlocks;
import io.github.bertie_mc.bertieprogression.cybernetics.AdaptiveImmuneSystem;
import io.github.bertie_mc.bertieprogression.cybernetics.CenterEnergyAccess;
import io.github.bertie_mc.bertieprogression.cybernetics.CopperAugmentation;
import io.github.bertie_mc.bertieprogression.cybernetics.MechanicalAdapterBlockEntity;
import io.github.bertie_mc.bertieprogression.cybernetics.MechanicalAdapterRegistry;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;
import rearth.oritech.block.entity.augmenter.api.CustomAugmentsCollection;

public final class CyberneticsClientTests {
    @ClientTest
    public static void equipmentAndVacuumProtectionLoadAndRespectToggle(ClientTestContext context) throws Exception {
        try (var world = context.worldBuilder().create()) {
            world.server().runOnServer(server -> {
                var recipes = server.getRecipeManager();
                for (String name : List.of(
                        "mekanism/atomic_disassembler",
                        "mekanism/chargepad",
                        "mekmm/wireless_charging_station",
                        "oritech/charger_block",
                        "oritech/chainsaw",
                        "oritech/electric_mace",
                        "oritech/hand_drill",
                        "oritech/portable_laser",
                        "oritech/advanced_augment_station",
                        "minecraft/elytra_mechanical",
                        "minecraft/elytra_nether")) {
                    var id = ResourceLocation.parse("bertieprogression:crafting/" + name);
                    check(recipes.byKey(id).isPresent(), "Native serializer rejected captured recipe " + id);
                }
                check(
                        recipes.byKey(ResourceLocation.parse("anvilcraft:jewel_crafting/elytra"))
                                .isEmpty(),
                        "Jewelcrafting shortcut remains");
                var oxidizer = recipes.byKey(ResourceLocation.parse("mekanism:chemical_oxidizer"))
                        .orElseThrow()
                        .value();
                for (String item : List.of("mekanism:personal_barrel", "mekanism:personal_chest")) {
                    var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)));
                    check(oxidizer.getIngredients().stream().anyMatch(i -> i.test(stack)), "Oxidizer rejects " + item);
                }
                var augment = CustomAugmentsCollection.getById(AdaptiveImmuneSystem.ID);
                check(augment instanceof AdaptiveImmuneSystem, "Native Auto-Feeder effect was not replaced");
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.getFoodData().setFoodLevel(8);
                var food = new ItemStack(Items.GOLDEN_APPLE, 4);
                player.getInventory().setItem(0, food);
                augment.installToPlayer(player);
                augment.refreshServer(player);
                check(
                        food.getCount() == 4 && player.getFoodData().getFoodLevel() == 8,
                        "Adaptive immunity still eats food");
                var moon = server.getLevel(
                        ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("unbound:moon")));
                check(
                        moon != null
                                && (Boolean) Class.forName("me.moltea.unbound.life.Air")
                                        .getMethod("airless", net.minecraft.world.level.Level.class)
                                        .invoke(null, moon),
                        "Vacuum fixture must use an Unbound airless dimension");
                player.teleportTo(moon, 0.5, 160, 0.5, java.util.Set.of(), 0, 0);
                player.setAirSupply(10);
                check(
                        breathing("supplied", player) && !breathing("begin", player),
                        "Enabled augment does not provide air");
                breathing("end", player);
                check(player.getAirSupply() == 10, "Vacuum consumed air through active protection");
                augment.toggle(player);
                check(
                        !breathing("supplied", player) && breathing("begin", player),
                        "Disabled augment still protects from vacuum");
                breathing("end", player);
                check(player.getAirSupply() == 9, "Disabling protection must restore native vacuum behavior");
                augment.removeFromPlayer(player);
                player.teleportTo(server.overworld(), 0.5, 72, 0.5, java.util.Set.of(), 0, 0);
                player.setGameMode(GameType.CREATIVE);
            });
        }
    }

    @ClientTest
    public static void copperCenterBlocksRfAndUsesBothRearInputs(ClientTestContext context) throws Exception {
        try (var world = context.worldBuilder().create()) {
            var centerRef = new AtomicReference<AugmentApplicationEntity>();
            world.server().runCommand("fill -8 63 -8 12 63 12 minecraft:stone");
            world.server().runCommand("fill -8 64 -8 12 72 12 minecraft:air");
            world.server().runOnServer(server -> {
                var level = server.overworld();
                var pos = new BlockPos(2, 64, 2);
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("oritech:augment_application_block"));
                level.setBlockAndUpdate(
                        pos,
                        block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
                var center = (AugmentApplicationEntity) level.getBlockEntity(pos);
                centerRef.set(center);
                for (var support : CopperAugmentation.supports(center))
                    level.setBlockAndUpdate(support, CopperAugmentation.casing().defaultBlockState());
                check(center.initMultiblock(center.getBlockState()), "Center did not form from Copper Casings");
                var internal = ((CenterEnergyAccess) center).bertieprogression$internalEnergy();
                internal.setAmount(1000);
                for (Direction side : Direction.values()) {
                    for (var exposed :
                            List.of(center.getEnergyStorage(side), center.getEnergyStorageForMultiblock(side))) {
                        check(
                                !exposed.supportsInsertion() && !exposed.supportsExtraction(),
                                "External RF capability remains open");
                        check(
                                exposed.insert(500, false) == 0 && exposed.extract(500, false) == 0,
                                "External RF transfer succeeds");
                        exposed.setAmount(999999);
                        check(internal.getAmount() == 1000, "External setter bypasses Create-only power");
                    }
                }
                internal.setAmount(0);
                var rear = pos.south(2);
                level.setBlockAndUpdate(rear, CopperAugmentation.casing().defaultBlockState());
                level.setBlockAndUpdate(
                        rear.above(), CopperAugmentation.casing().defaultBlockState());
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setItemInHand(
                        InteractionHand.MAIN_HAND, new ItemStack(com.simibubi.create.AllItems.WRENCH.get()));
                var interaction = new PlayerInteractEvent.RightClickBlock(
                        player,
                        InteractionHand.MAIN_HAND,
                        rear,
                        new BlockHitResult(Vec3.atCenterOf(rear), Direction.SOUTH, rear, false));
                NeoForge.EVENT_BUS.post(interaction);
                check(
                        interaction.isCanceled()
                                && level.getBlockState(rear).is(MechanicalAdapterRegistry.BLOCK.get())
                                && level.getBlockState(rear.above()).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                                        == DoubleBlockHalf.UPPER,
                        "Wrench did not form the rear adapter from two Copper Casings");
                level.setBlockAndUpdate(
                        new BlockPos(4, 64, 4), CopperAugmentation.casing().defaultBlockState());
                for (var socket : List.of(rear, rear.above())) {
                    level.setBlockAndUpdate(
                            socket.south(),
                            AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
                    level.setBlockAndUpdate(
                            socket.south(2),
                            AllBlocks.CREATIVE_MOTOR
                                    .getDefaultState()
                                    .setValue(BlockStateProperties.FACING, Direction.NORTH));
                }
                check(
                        MechanicalAdapterBlockEntity.chargeRate(128, 64, true, true) == 2048,
                        "Unequal same-direction speeds must work");
                check(
                        MechanicalAdapterBlockEntity.chargeRate(128, -64, true, true) == 0,
                        "Opposing shafts charge the center");
                check(
                        MechanicalAdapterBlockEntity.chargeRate(128, 64, true, false) == 0,
                        "One input substitutes for the second");
                check(MechanicalAdapterBlockEntity.chargeRate(0, 64, true, true) == 0, "Stopped shaft still charges");
            });
            world.server()
                    .waitFor(
                            "Both external shafts charge the center",
                            server -> ((CenterEnergyAccess) centerRef.get())
                                            .bertieprogression$internalEnergy()
                                            .getAmount()
                                    > 0,
                            200);
            world.server().runOnServer(server -> {
                var level = server.overworld();
                level.destroyBlock(new BlockPos(2, 65, 5), false);
            });
            context.waitTicks(10);
            long stopped = world.server()
                    .computeOnServer(server -> ((CenterEnergyAccess) centerRef.get())
                            .bertieprogression$internalEnergy()
                            .getAmount());
            context.waitTicks(10);
            world.server()
                    .runOnServer(server -> check(
                            ((CenterEnergyAccess) centerRef.get())
                                            .bertieprogression$internalEnergy()
                                            .getAmount()
                                    == stopped,
                            "Disconnected upper input did not stop charging"));
            world.server().runCommand("tp @a 6.7 65.2 8.8 143 15");
            world.server().runCommand("time set noon");
            context.runOnClient(client -> {
                client.options.hideGui = true;
                client.options.pauseOnLostFocus = false;
            });
            world.connection().waitForClientboundPackets();
            world.connection().waitForChunksRender(false);
            context.waitTicks(20);
            context.takeScreenshot("mechanical-adapter-copper-palette-and-rear-mount");
        }
    }

    @ClientTest
    public static void carminiteOnAnvilMakesMechanicalFusionAnvil(ClientTestContext context) throws Exception {
        try (var world = context.worldBuilder().create()) {
            world.server().runOnServer(server -> {
                var level = server.overworld();
                var pos = new BlockPos(0, 65, 0);
                level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(pos, Blocks.ANVIL.defaultBlockState());
                level.setBlockAndUpdate(
                        pos.above(),
                        BuiltInRegistries.BLOCK
                                .get(ResourceLocation.parse("twilightforest:carminite_block"))
                                .defaultBlockState());
                FallingBlockEntity.fall(level, pos.above(4), Blocks.ANVIL.defaultBlockState());
            });
            world.server()
                    .waitFor(
                            "Carminite-on-Anvil compresses into Mechanical Fusion Anvil",
                            server -> {
                                var result = BuiltInRegistries.BLOCK.get(
                                        ResourceLocation.parse("cataclysm:mechanical_fusion_anvil"));
                                return server.overworld()
                                                .getBlockState(new BlockPos(0, 65, 0))
                                                .is(result)
                                        || server.overworld()
                                                .getBlockState(new BlockPos(0, 66, 0))
                                                .is(result);
                            },
                            200);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static boolean breathing(String method, net.minecraft.world.entity.LivingEntity entity) throws Exception {
        return Boolean.TRUE.equals(Class.forName("me.moltea.unbound.life.Breathing")
                .getMethod(method, net.minecraft.world.entity.LivingEntity.class)
                .invoke(null, entity));
    }
}
