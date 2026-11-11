package io.github.bertie_mc.bertieprogression.gametest;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import com.mojang.authlib.GameProfile;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import io.github.bertie_mc.bertieprogression.dragon.DragonProgression;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class DragonProgressionGameTests {
    private static final List<String> ELEMENTS = List.of("fire", "ice", "lightning");

    @GameTest(template = "empty", batch = "dragon_progression")
    public static void nativeCavesGenerateInImbriferAndLimitAgesElsewhere(GameTestHelper helper) throws Exception {
        var overworld = helper.getLevel().getServer().overworld();
        var imbrifer = helper.getLevel()
                .getServer()
                .getLevel(ResourceKey.create(Registries.DIMENSION, DragonProgression.IMBRIFER));
        helper.assertTrue(imbrifer != null, "Imbrifer test dimension must load");
        var registry = imbrifer.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var generator = imbrifer.getChunkSource().getGenerator();
        for (String element : ELEMENTS) {
            var cave = registry.get(ResourceLocation.parse("iceandfire:" + element + "_dragon_cave"));
            helper.assertTrue(cave != null, "Native cave must register: " + element);
            var biome = imbrifer.getBiome(new BlockPos(4096, 0, 4096));
            helper.assertTrue(cave.biomes().contains(biome), "Imbrifer biome must support " + element + " caves");
            StructureStart start = StructureStart.INVALID_START;
            for (int attempt = 0; attempt < 128 && !start.isValid(); attempt++) {
                start = cave.generate(
                        imbrifer.registryAccess(),
                        generator,
                        generator.getBiomeSource(),
                        imbrifer.getChunkSource().randomState(),
                        imbrifer.getServer().getStructureManager(),
                        0x73F012L + attempt,
                        new ChunkPos(256 + attempt, 256),
                        0,
                        imbrifer,
                        cave.biomes()::contains);
            }
            helper.assertTrue(
                    start.isValid() && start.getPieces().size() == 9,
                    "Native cave must produce its nine pieces in Imbrifer: valid=" + start.isValid()
                            + ", pieces=" + start.getPieces().size() + ", minY=" + imbrifer.getMinBuildHeight()
                            + ", generatorMinY=" + generator.getMinY() + ", biome=" + biome.unwrapKey());
            var piece = start.getPieces().getFirst();
            var create = piece.getClass()
                    .getSuperclass()
                    .getDeclaredMethod(
                            "createDragon", WorldGenLevel.class, RandomSource.class, BlockPos.class, int.class);
            create.setAccessible(true);
            for (ServerLevel level : List.of(overworld, imbrifer)) {
                var world = (WorldGenLevel) Proxy.newProxyInstance(
                        WorldGenLevel.class.getClassLoader(),
                        new Class<?>[] {WorldGenLevel.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("getLevel")) return level;
                            throw new UnsupportedOperationException(method.getName());
                        });
                var dragon = (DragonBaseEntity) create.invoke(piece, world, RandomSource.create(7), BlockPos.ZERO, 124);
                helper.assertTrue(
                        dragon.getDragonStage() == (level == imbrifer ? 5 : 4),
                        "Cave age restriction must follow the dimension");
                helper.assertTrue(dragon.isAgingDisabled(), "Generated dragons must retain disabled aging");
            }
            var growing = dragon(element, overworld);
            growing.setAgeInDays(124);
            helper.assertTrue(
                    growing.getDragonStage() == 5, "Ordinary growing or existing dragons must remain unchanged");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "dragon_progression")
    public static void stageFourHarvestKeepsHeartsWithoutEggs(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(
                level, new GameProfile(UUID.fromString("610760cc-3ab3-465c-958d-3340d296b2cf"), "dragon_harvest"));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var box = new AABB(pos).inflate(2);
        for (String element : ELEMENTS) {
            for (int age : new int[] {90, 120}) {
                level.getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
                var dragon = dragon(element, level);
                dragon.setPos(pos.getX(), pos.getY(), pos.getZ());
                dragon.setGender(false);
                dragon.setAgeInDays(age);
                dragon.setModelDead(true);
                dragon.setDeathStage(Math.min(age / 5, 25) / 2 - 1);
                dragon.mobInteract(player, InteractionHand.MAIN_HAND);
                var drops = level.getEntitiesOfClass(ItemEntity.class, box);
                long eggs = drops.stream()
                        .filter(item -> BuiltInRegistries.ITEM
                                .getKey(item.getItem().getItem())
                                .getPath()
                                .startsWith("dragonegg_"))
                        .count();
                helper.assertTrue(eggs == (age >= 100 ? 1 : 0), "Only Stage 5 may yield an egg: " + element);
                helper.assertTrue(
                        drops.stream()
                                .anyMatch(item -> item.getItem()
                                        .is(BuiltInRegistries.ITEM.get(
                                                ResourceLocation.parse("iceandfire:" + element + "_dragon_heart")))),
                        "Hearts must remain at both stages");
            }
        }
        level.getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "dragon_progression")
    public static void selectedDragonforgeRecipesLoadWithTheirNativeInputs(GameTestHelper helper) {
        var level = helper.getLevel();
        for (String element : ELEMENTS) {
            String sculk = element.equals("fire")
                    ? "l2complements:craft/sculkium_ingot"
                    : "bertieprogression:dragonforge/sculkium_" + element;
            var recipe = (DragonForgeRecipe) level.getRecipeManager()
                    .byKey(ResourceLocation.parse(sculk))
                    .orElseThrow()
                    .value();
            helper.assertTrue(recipe.getDragonType().equals(element), "Sculkium forge element must match");
            helper.assertTrue(recipe.isValidInput(stack("oritech:duratium_ingot")), "Duratium must be the solid input");
            helper.assertTrue(
                    recipe.isValidBlood(stack("deeperdarker:reinforced_echo_shard")),
                    "Echo Shard must be accepted in the second slot");
            helper.assertTrue(
                    recipe.getResultItem()
                                    .is(stack("l2complements:sculkium_ingot").getItem())
                            && recipe.getResultItem().getCount() == 1,
                    "Sculkium output must be one");
            var blood = (DragonForgeRecipe) level.getRecipeManager()
                    .byKey(ResourceLocation.parse("iceandfire:dragonforge/" + element + "_dragon_blood"))
                    .orElseThrow()
                    .value();
            helper.assertTrue(
                    blood.isValidBlood(new ItemStack(Items.DRAGON_BREATH))
                            && !blood.isValidBlood(new ItemStack(Items.GLASS_BOTTLE)),
                    "Blood production must consume Dragon's Breath rather than an empty bottle");
            helper.assertTrue(blood.getCookTime() == 750, "Blood cooking time must remain unchanged");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "dragon_progression")
    public static void forgeUpgradeAndCasingRitualsLoadAndMatch(GameTestHelper helper) {
        var rituals = helper.getLevel().registryAccess().registryOrThrow(FARegistries.RITUAL);
        var upgrade = rituals.get(ResourceLocation.parse("forbidden_arcanus:upgrade_tier_3"));
        helper.assertTrue(
                upgrade != null && upgrade.inputs().size() == 8,
                "Forge upgrade must load with eight pedestal ingredients");
        var essences = upgrade.requirements().essences();
        helper.assertTrue(
                essences.aureal() == 3000
                        && essences.blood() == 15000
                        && essences.souls() == 50
                        && essences.experience() == 1350,
                "Forge upgrade must consume every Tier II resource capacity");
        helper.assertTrue(
                upgrade.requirements().tier().test(2)
                        && !upgrade.requirements().tier().test(3),
                "Upgrade must retain exact Tier II matching");
        var casing = rituals.get(ResourceLocation.parse("bertieprogression:machine_casing"));
        helper.assertTrue(
                casing != null && casing.inputs().size() == 3, "Casing ritual must load with three ingredient groups");
        var inputs = casing.inputs().stream()
                .flatMap(input -> java.util.stream.IntStream.range(0, input.amount())
                        .mapToObj(index -> input.ingredient().getItems()[0].copy()))
                .toList();
        helper.assertTrue(
                inputs.size() == 12 && casing.checkIngredients(inputs, stack("cognition:primordial_assembly")),
                "Casing ritual must match all twelve pedestal items");
        helper.assertTrue(
                !casing.requirements().tier().test(2)
                        && casing.requirements().tier().test(3),
                "Casing must require Forge III");
        helper.assertTrue(
                rituals.get(ResourceLocation.parse("bertieprogression:controller")) == null,
                "The superseded Controller ritual must be absent");
        helper.assertTrue(
                casing.requirements().essences().aureal() == 3000
                        && casing.requirements().essences().experience() == 1000
                        && casing.requirements().essences().blood() == 0
                        && casing.requirements().essences().souls() == 0,
                "Casing essence costs must match");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "dragon_progression")
    public static void generatedAnvilHeatingKeepsSmeltingAndRejectsEquipment(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var wrap = Class.forName("dev.dubhe.anvilcraft.recipe.anvil.wrap.VanillaRecipesWrap")
                .getMethod("init", HolderLookup.Provider.class, Collection.class);
        var selected = List.<RecipeHolder<?>>of(
                new RecipeHolder<>(
                        ResourceLocation.parse("bertieprogression:test/equipment_smelting"),
                        new SmeltingRecipe(
                                "",
                                CookingBookCategory.MISC,
                                Ingredient.of(Items.IRON_PICKAXE),
                                new ItemStack(Items.IRON_NUGGET),
                                0,
                                200)),
                new RecipeHolder<>(
                        ResourceLocation.parse("bertieprogression:test/equipment_blasting"),
                        new BlastingRecipe(
                                "",
                                CookingBookCategory.MISC,
                                Ingredient.of(Items.GOLDEN_HELMET),
                                new ItemStack(Items.GOLD_NUGGET),
                                0,
                                100)),
                new RecipeHolder<>(
                        ResourceLocation.parse("bertieprogression:test/ordinary_smelting"),
                        new SmeltingRecipe(
                                "",
                                CookingBookCategory.MISC,
                                Ingredient.of(Items.COBBLESTONE),
                                new ItemStack(Items.STONE),
                                0,
                                200)),
                new RecipeHolder<>(
                        ResourceLocation.parse("bertieprogression:test/ordinary_blasting"),
                        new BlastingRecipe(
                                "",
                                CookingBookCategory.MISC,
                                Ingredient.of(Items.RAW_IRON),
                                new ItemStack(Items.IRON_INGOT),
                                0,
                                100)));
        try {
            var wrapped = (List<?>) wrap.invoke(null, level.registryAccess(), selected);
            helper.assertTrue(wrapped.size() == 2, "Only the two ordinary heating recipes may wrap");
            helper.assertTrue(
                    wrapped.stream()
                            .noneMatch(recipe ->
                                    ((RecipeHolder<?>) recipe).id().getPath().contains("nugget")),
                    "Equipment recovery must not be generated");
        } finally {
            wrap.invoke(null, level.registryAccess(), level.getRecipeManager().getRecipes());
        }
        helper.assertTrue(
                level.getRecipeManager()
                        .byKey(ResourceLocation.parse("anvilcraft:item_crush/tool/diamond_pickaxe_2_diamond"))
                        .isEmpty(),
                "Equipment crushing recipe must be absent");
        helper.assertTrue(
                level.getRecipeManager()
                        .byKey(ResourceLocation.parse("anvilcraft:super_heating/raw/copper_ingot"))
                        .isEmpty(),
                "Raw-metal multiplier must be absent");
        helper.assertTrue(
                level.getRecipeManager()
                        .byKey(ResourceLocation.parse("anvilcraft:super_heating/charcoal"))
                        .isPresent(),
                "Material heating must remain");
        helper.succeed();
    }

    private static DragonBaseEntity dragon(String element, Level level) {
        return (DragonBaseEntity) BuiltInRegistries.ENTITY_TYPE
                .get(ResourceLocation.parse("iceandfire:" + element + "_dragon"))
                .create(level);
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private DragonProgressionGameTests() {}
}
