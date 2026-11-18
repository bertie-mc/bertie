package io.github.bertie_mc.bertieprogression.clienttest;

import com.google.gson.JsonParser;
import io.github.bertie_mc.bertieprogression.BertieProgression;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import java.io.InputStreamReader;
import java.util.List;
import mekanism.api.MekanismAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;

public final class RetainedMaterialsClientTests {
    @ClientTest
    public static void keptMaterialsRecipesDropsAliasesAndModelsWorkWithoutStellaris(ClientTestContext context)
            throws Exception {
        var spec = JsonParser.parseReader(new InputStreamReader(
                        BertieProgression.class.getResourceAsStream("/retained_stellaris_materials.json")))
                .getAsJsonObject();
        try (var world = context.worldBuilder().create()) {
            world.server().runOnServer(server -> {
                check(!ModList.get().isLoaded("stellaris"), "Stellaris must remain absent");
                for (String array : List.of("items", "blocks"))
                    for (var name : spec.getAsJsonArray(array)) {
                        var id = ResourceLocation.parse("bertieprogression:" + name.getAsString());
                        check(BuiltInRegistries.ITEM.containsKey(id), "Missing retained material item " + id);
                        check(
                                id.equals(BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.get(id))),
                                "Retained item has wrong owner");
                    }
                var chemicals = server.registryAccess().registryOrThrow(MekanismAPI.CHEMICAL_REGISTRY_NAME);
                for (String m : List.of("corronium", "solerium")) {
                    for (String form : List.of(
                            "dust",
                            "dirty_dust",
                            "clump",
                            "gem",
                            "shard",
                            "crystal",
                            "spec",
                            "dense_charge",
                            "activated_crystal"))
                        check(
                                BuiltInRegistries.ITEM.containsKey(id(m + "_" + form)),
                                "Missing processing form " + m + "/" + form);
                    for (String state : List.of("dirty", "clean"))
                        check(chemicals.containsKey(id(state + "_" + m)), "Missing retained slurry");
                }
                for (var alias : spec.getAsJsonObject("item_aliases").entrySet()) {
                    check(
                            BuiltInRegistries.ITEM.get(ResourceLocation.parse(alias.getKey()))
                                    == BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                                            alias.getValue().getAsString())),
                            "Old item alias is not preserved");
                    var saved = new net.minecraft.nbt.CompoundTag();
                    saved.putString("id", alias.getKey());
                    saved.putInt("count", 3);
                    var ops = net.minecraft.resources.RegistryOps.create(
                            net.minecraft.nbt.NbtOps.INSTANCE, server.registryAccess());
                    var decoded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
                    check(
                            decoded.getCount() == 3
                                    && decoded.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                                            alias.getValue().getAsString()))),
                            "Saved legacy stack failed migration");
                }
                for (var alias : spec.getAsJsonObject("block_aliases").entrySet()) {
                    check(
                            BuiltInRegistries.BLOCK.get(ResourceLocation.parse(alias.getKey()))
                                    == BuiltInRegistries.BLOCK.get(ResourceLocation.parse(
                                            alias.getValue().getAsString())),
                            "Old block alias is not preserved");
                    var palette = new net.minecraft.nbt.CompoundTag();
                    palette.putString("Name", alias.getKey());
                    var decoded =
                            net.minecraft.nbt.NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette);
                    check(
                            decoded.is(BuiltInRegistries.BLOCK.get(
                                    ResourceLocation.parse(alias.getValue().getAsString()))),
                            "Saved legacy block palette failed migration");
                }
                for (var recipe : spec.getAsJsonArray("recipe_ids")) {
                    check(
                            server.getRecipeManager()
                                    .byKey(ResourceLocation.parse(recipe.getAsString()))
                                    .isPresent(),
                            "Retained recipe failed native loading: " + recipe.getAsString());
                }
                for (String material : List.of("solerium", "corronium")) {
                    var ore = BuiltInRegistries.BLOCK.get(id(material + "_ore"));
                    var drops = Block.getDrops(
                            ore.defaultBlockState(),
                            server.overworld(),
                            new BlockPos(0, 64, 0),
                            null,
                            null,
                            new ItemStack(Items.DIAMOND_PICKAXE));
                    check(
                            drops.stream().anyMatch(s -> s.is(BuiltInRegistries.ITEM.get(id("raw_" + material)))),
                            "Retained ore has incorrect drops");
                    var holder = server.getRecipeManager().getRecipes().stream()
                            .filter(r -> r.value() instanceof AbstractCookingRecipe cooking
                                    && cooking.matches(
                                            new SingleRecipeInput(
                                                    new ItemStack(BuiltInRegistries.ITEM.get(id("raw_" + material)))),
                                            server.overworld())
                                    && r.value()
                                            .getResultItem(server.registryAccess())
                                            .is(BuiltInRegistries.ITEM.get(
                                                    id(material.equals("solerium") ? "solerium" : "corronium_ingot"))))
                            .findFirst();
                    check(holder.isPresent(), "Raw retained material cannot be smelted");
                }
            });
            world.connection().waitForClientboundPackets();
            context.runOnClient(client -> {
                for (String array : List.of("items", "blocks"))
                    for (var name : spec.getAsJsonArray(array)) {
                        var stack = new ItemStack(BuiltInRegistries.ITEM.get(id(name.getAsString())));
                        var model = client.getItemRenderer().getModel(stack, client.level, client.player, 0);
                        check(
                                model != client.getModelManager().getMissingModel(),
                                "Retained material model is missing");
                        check(
                                !model.getParticleIcon()
                                        .contents()
                                        .name()
                                        .getPath()
                                        .equals("missingno"),
                                "Retained material texture is missing");
                    }
            });
        }
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.parse("bertieprogression:" + name);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
