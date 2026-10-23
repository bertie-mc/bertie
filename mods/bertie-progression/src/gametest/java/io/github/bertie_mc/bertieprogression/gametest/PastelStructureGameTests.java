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
