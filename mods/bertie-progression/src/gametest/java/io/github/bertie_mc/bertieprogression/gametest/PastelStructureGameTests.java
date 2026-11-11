package io.github.bertie_mc.bertieprogression.gametest;

import io.github.bertie_mc.bertieprogression.worldgen.PastelStructureRemoval;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class PastelStructureGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void dyeFuelUsesNativeCostsAndDoesNotPartiallyPayFailedActions(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var color = earth.terrarium.pastel.api.energy.color.InkColors.BLUE;
        player.getInventory()
                .setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLUE_DYE));
        h.assertTrue(
                earth.terrarium.pastel.api.energy.InkPowered.canUse(player),
                "removed ink milestones must not block gear");
        h.assertTrue(
                earth.terrarium.pastel.api.energy.InkPowered.tryDrainEnergy(player, color, 25),
                "dye must fuel the ability");
        h.assertTrue(player.getInventory().getItem(0).isEmpty(), "one dye must be consumed");
        h.assertTrue(
                earth.terrarium.pastel.api.energy.InkPowered.getAvailableInk(player, color) == 75,
                "the unused part of the pigment-equivalent charge must remain");
        h.assertTrue(
                !earth.terrarium.pastel.api.energy.InkPowered.tryDrainEnergy(player, color, 76),
                "an unaffordable action must fail");
        h.assertTrue(
                earth.terrarium.pastel.api.energy.InkPowered.getAvailableInk(player, color) == 75,
                "a failed action must not consume credit");
        player.getInventory()
                .setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLUE_DYE, 2));
        h.assertTrue(
                earth.terrarium.pastel.api.energy.InkPowered.tryDrainEnergy(player, color, 175),
                "payments must span saved credit and dyes");
        h.assertTrue(player.getInventory().getItem(0).getCount() == 1, "only the required dye may be consumed");
        h.assertTrue(
                earth.terrarium.pastel.api.energy.InkPowered.getAvailableInk(player, color) == 100,
                "the last complete dye must remain available");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void dragonrotLakesUseLavaAndKeepTheirHornslake(GameTestHelper h) {
        var features = h.getLevel()
                .registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
        var lake = (net.minecraft.world.level.levelgen.feature.LakeFeature.Configuration)
                features.get(net.minecraft.resources.ResourceLocation.parse("pastel:lake_dragonrot"))
                        .config();
        var random = net.minecraft.util.RandomSource.create(0);
        h.assertTrue(
                lake.fluid()
                        .getState(random, net.minecraft.core.BlockPos.ZERO)
                        .is(net.minecraft.world.level.block.Blocks.LAVA),
                "withdrawn liquid must generate as lava");
        h.assertTrue(
                lake.barrier()
                        .getState(random, net.minecraft.core.BlockPos.ZERO)
                        .is(earth.terrarium.pastel.registries.PastelBlocks.HORNSLAKE.get()),
                "the native swamp lining must survive fluid removal");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void retiredRuinRequirementsAreRemovedWithoutOpeningMoonstoneChecks(GameTestHelper h)
            throws Exception {
        CompoundTag source = TagParser.parseTag("""
                {blocks:[{nbt:{RequiredAdvancement:"pastel:collect_all_basic_shards"}},
                         {nbt:{RequiredAdvancement:"pastel:midgame/get_azure_dike_charge"}},
                         {nbt:{RequiredAdvancement:"pastel:midgame/collect_moonstone_shard"}}]}
                """);
        PastelStructureRemoval.clean(source);
        var blocks = source.getList("blocks", Tag.TAG_COMPOUND);
        h.assertTrue(
                !blocks.getCompound(0).getCompound("nbt").contains("RequiredAdvancement"),
                "basic shard gate must open");
        h.assertTrue(
                !blocks.getCompound(1).getCompound("nbt").contains("RequiredAdvancement"), "Azure Dike gate must open");
        h.assertTrue(
                blocks.getCompound(2).getCompound("nbt").contains("RequiredAdvancement"),
                "retained Moonstone check must survive");
        var controller = new earth.terrarium.pastel.blocks.structure.PreservationControllerBlockEntity(
                net.minecraft.core.BlockPos.ZERO,
                earth.terrarium.pastel.registries.PastelBlocks.PRESERVATION_CONTROLLER
                        .get()
                        .defaultBlockState());
        controller.loadWithComponents(
                TagParser.parseTag("{RequiredAdvancement:\"pastel:collect_all_basic_shards\"}"),
                h.getLevel().registryAccess());
        h.assertTrue(
                !controller.saveWithoutMetadata(h.getLevel().registryAccess()).contains("RequiredAdvancement"),
                "existing controllers must also migrate");
        var gate =
                earth.terrarium.pastel.registries.PastelBlocks.DIKE_GATE.get().defaultBlockState();
        h.assertTrue(
                gate.getCollisionShape(
                                h.getLevel(),
                                h.absolutePos(net.minecraft.core.BlockPos.ZERO),
                                net.minecraft.world.phys.shapes.CollisionContext.of(
                                        h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL)))
                        .isEmpty(),
                "Overworld gate must not require removed charges");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void withdrawnOreIsInertAndBismuthDropsItsCrystal(GameTestHelper h) {
        var features = h.getLevel()
                .registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
        var shimmerstone = features.get(net.minecraft.resources.ResourceLocation.parse("pastel:shimmerstone_ore"));
        h.assertTrue(
                shimmerstone != null
                        && shimmerstone.feature() == net.minecraft.world.level.levelgen.feature.Feature.NO_OP,
                "both Overworld and Imbrifer placements must resolve to an inert Shimmerstone feature");
        var state = earth.terrarium.pastel.registries.PastelBlocks.BISMUTH_CLUSTER
                .get()
                .defaultBlockState();
        var drops = net.minecraft.world.level.block.Block.getDrops(
                state,
                h.getLevel(),
                h.absolutePos(net.minecraft.core.BlockPos.ZERO),
                null,
                null,
                net.minecraft.world.item.Items.NETHERITE_PICKAXE.getDefaultInstance());
        h.assertTrue(
                drops.stream()
                        .anyMatch(
                                stack -> stack.is(earth.terrarium.pastel.registries.PastelItems.BISMUTH_CRYSTAL.get())),
                "naturally generated Bismuth must have a usable drop");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void templateLoadRemovesFixedPastelInventoryContents(GameTestHelper h) throws Exception {
        CompoundTag source = TagParser.parseTag("""
                {size:[1,1,1],palette:[{Name:"minecraft:chest"}],
                 blocks:[{state:0,pos:[0,0,0],nbt:{id:"minecraft:chest",Items:[
                   {id:"pastel:nightdew_sprout",count:1},{id:"minecraft:diamond",count:1}]}}],entities:[]}
                """);
        StructureTemplate template = new StructureTemplate();
        template.load(BuiltInRegistries.BLOCK.asLookup(), source);
        var saved = template.save(new CompoundTag());
        var items = saved.getList("blocks", Tag.TAG_COMPOUND)
                .getCompound(0)
                .getCompound("nbt")
                .getList("Items", Tag.TAG_COMPOUND);
        h.assertTrue(items.size() == 1, "template loading must remove fixed withdrawn contents");
        h.assertTrue(items.getCompound(0).getString("id").equals("minecraft:diamond"), "other contents must survive");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedFurnitureClearsPropertiesAndBlockEntityData(GameTestHelper h) throws Exception {
        CompoundTag source = TagParser.parseTag("""
                {palettes:[[{Name:"pastel:item_roundel",Properties:{facing:"north"}},
                            {Name:"pastel:jadeite_lotus_stem"}],
                           [{Name:"pastel:axolotl_blue_wall_head",Properties:{facing:"east"}},
                            {Name:"pastel:jadeite_lotus_stem"}]],
                 blocks:[{state:0,pos:[0,0,0],nbt:{id:"pastel:item_roundel",secret:1}},
                         {state:1,pos:[1,0,0]}],
                 entities:[{nbt:{id:"minecraft:item_frame",Item:{id:"pastel:pure_azurite",count:1}}}]}
                """);
        PastelStructureRemoval.clean(source);
        for (Tag palette : source.getList("palettes", Tag.TAG_LIST)) {
            var states = (net.minecraft.nbt.ListTag) palette;
            h.assertTrue(
                    states.getCompound(0).getString("Name").equals("minecraft:air"),
                    "withdrawn furniture and wall heads must become air");
            h.assertTrue(!states.getCompound(0).contains("Properties"), "air must not inherit block properties");
            h.assertTrue(
                    states.getCompound(1).getString("Name").equals("pastel:jadeite_lotus_stem"),
                    "retained plants must survive");
        }
        h.assertTrue(
                !source.getList("blocks", Tag.TAG_COMPOUND).getCompound(0).contains("nbt"),
                "removed furniture must not retain its inventory");
        h.assertTrue(
                !source.getList("entities", Tag.TAG_COMPOUND)
                        .getCompound(0)
                        .getCompound("nbt")
                        .contains("Item"),
                "withdrawn framed items must be removed");
        h.succeed();
    }
}
