package io.github.bertie_mc.bertieprogression.gametest;

import auviotre.enigmatic.legacy.contents.crafting.CursedShapedRecipe;
import auviotre.enigmatic.legacy.contents.item.books.EnchantmentTransposer;
import auviotre.enigmatic.legacy.registries.EnigmaticComponents;
import auviotre.enigmatic.legacy.registries.EnigmaticItems;
import com.mojang.authlib.GameProfile;
import io.github.bertie_mc.bertieprogression.recipe.CuttingRecipeRules;
import io.github.bertie_mc.bertieprogression.warden.WardenRules;
import io.github.bertie_mc.bertieprogression.warden.WardenState;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.mehvahdjukaar.sawmill.WoodcuttingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class ProgressionWardenGameTests {
    private ProgressionWardenGameTests() {}

    @GameTest(template = "empty", batch = "warden_progression")
    public static void shearingIsOnceAndStateSurvivesReload(GameTestHelper helper) {
        Warden warden = warden(helper);
        FakePlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        var first = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, warden);
        NeoForge.EVENT_BUS.post(first);
        helper.assertTrue(first.isCanceled(), "shearing must consume the interaction");
        helper.assertTrue(((WardenState) warden).bertie$tendrilsSheared(), "the synchronized flag must be set");
        helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "shearing must wear the tool");
        var second = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, warden);
        NeoForge.EVENT_BUS.post(second);
        helper.assertTrue(!second.isCanceled(), "a sheared Warden must not shear twice");
        long drops = helper
                .getLevel()
                .getEntities(
                        EntityType.ITEM,
                        new AABB(warden.blockPosition()).inflate(3),
                        entity -> BuiltInRegistries.ITEM
                                .getKey(entity.getItem().getItem())
                                .equals(WardenRules.TENDRIL))
                .stream()
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
        helper.assertTrue(drops == 1, "shearing must drop exactly one tendril");
        ((WardenState) warden).bertie$setRetaliationReadyAt(9876);
        CompoundTag saved = warden.saveWithoutId(new CompoundTag());
        Warden restored = EntityType.WARDEN.create(helper.getLevel());
        restored.load(saved);
        helper.assertTrue(((WardenState) restored).bertie$tendrilsSheared(), "the shear flag must survive save/load");
        helper.assertTrue(
                ((WardenState) restored).bertie$retaliationReadyAt() == 9876, "cooldown must survive save/load");
        warden.hurt(helper.getLevel().damageSources().genericKill(), 10000);
        long afterDeath = helper
                .getLevel()
                .getEntities(
                        EntityType.ITEM,
                        new AABB(warden.blockPosition()).inflate(3),
                        entity -> BuiltInRegistries.ITEM
                                .getKey(entity.getItem().getItem())
                                .equals(WardenRules.TENDRIL))
                .stream()
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
        helper.assertTrue(afterDeath == 1, "death must not add tendrils to the shearing drop");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "warden_progression")
    public static void retaliationHonoursBoundaryCooldownAndDistance(GameTestHelper helper) {
        Warden warden = warden(helper);
        FakePlayer player = player(helper);
        player.setPos(warden.position().add(12, 0, 0));
        float before = player.getHealth();
        warden.hurt(helper.getLevel().damageSources().playerAttack(player), 1);
        helper.assertTrue(player.getHealth() == before, "exactly twelve blocks must not trigger retaliation");
        player.invulnerableTime = 0;
        player.hurt(helper.getLevel().damageSources().sonicBoom(warden), 10);
        float ordinaryBoom = before - player.getHealth();
        player.setHealth(before);
        player.setPos(warden.position().add(1000, 0, 0));
        warden.invulnerableTime = 0;
        player.invulnerableTime = 100;
        warden.hurt(helper.getLevel().damageSources().playerAttack(player), 1);
        helper.assertTrue(player.getHealth() < before, "a far player must be hit through cooldown frames");
        helper.assertTrue(
                Math.abs(before - player.getHealth() - 2 * ordinaryBoom) < 0.01,
                "retaliation must deal twice ordinary sonic damage");
        long ready = ((WardenState) warden).bertie$retaliationReadyAt();
        helper.assertTrue(ready == helper.getLevel().getGameTime() + 200, "retaliation cooldown must be ten seconds");
        float after = player.getHealth();
        warden.invulnerableTime = 0;
        warden.hurt(helper.getLevel().damageSources().playerAttack(player), 1);
        helper.assertTrue(player.getHealth() == after, "a second hit inside cooldown must not retaliate");
        ((WardenState) warden).bertie$setRetaliationReadyAt(helper.getLevel().getGameTime());
        warden.invulnerableTime = 0;
        warden.hurt(helper.getLevel().damageSources().playerAttack(player), 1);
        helper.assertTrue(player.getHealth() < after, "the ability must fire again after cooldown expires");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "warden_progression")
    public static void tomeUsesNativeCurseAndTaintRules(GameTestHelper helper) throws Exception {
        ItemStack tome = new ItemStack(EnigmaticItems.ENCHANTMENT_TRANSPOSER.get());
        helper.assertTrue(
                tome.getOrDefault(EnigmaticComponents.CURSED.get(), false), "tome must belong to the cursed family");
        FakePlayer player = player(helper);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(helper.getLevel().registryAccess().holderOrThrow(Enchantments.UNBREAKING), 1);
        var method = EnchantmentTransposer.class.getDeclaredMethod(
                "canDisenchant", Player.class, ItemStack.class, ItemStack.class);
        method.setAccessible(true);
        helper.assertTrue(
                !(boolean) method.invoke(tome.getItem(), player, tome, sword),
                "a player without the ring cannot use the tome");
        var slots = CuriosApi.getCuriosInventory(player)
                .orElseThrow()
                .getStacksHandler("ring")
                .orElseThrow();
        CuriosApi.getCuriosInventory(player)
                .orElseThrow()
                .setEquippedCurio("ring", 0, new ItemStack(EnigmaticItems.CURSED_RING.get()));
        helper.assertTrue(
                CuriosApi.getCuriosInventory(player)
                        .orElseThrow()
                        .findFirstCurio(stack -> stack.is(EnigmaticItems.CURSED_RING.get()))
                        .isPresent(),
                "test ring must be equipped in Curios");
        helper.assertTrue(
                !sword.getOrDefault(
                                net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY)
                        .isEmpty(),
                "test target must retain an enchantment");
        helper.assertTrue(
                (boolean) method.invoke(tome.getItem(), player, tome, sword), "the ring must permit normal tome use");
        ItemStack heart = new ItemStack(EnigmaticItems.EARTH_HEART.get());
        var recipe = new CursedShapedRecipe(
                "",
                CraftingBookCategory.MISC,
                ShapedRecipePattern.of(
                        Map.of('B', Ingredient.of(Items.BOOK), 'E', Ingredient.of(heart)), List.of("BE")),
                tome,
                true);
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BOOK), heart));
        helper.assertTrue(!recipe.matches(input, helper.getLevel()), "an untainted heart must fail the cursed recipe");
        heart.set(EnigmaticComponents.TAINTABLE.get(), true);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "a tainted heart must enable the recipe");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "warden_progression")
    public static void woodCutsMoveWithoutDuplicateOrOutputLoss(GameTestHelper helper) {
        var wood = new RecipeHolder<>(
                ResourceLocation.parse("bertieprogression:test_existing_wood"),
                new WoodcuttingRecipe("", Ingredient.of(Items.OAK_PLANKS), new ItemStack(Items.OAK_SLAB, 2), 1));
        var duplicate = new RecipeHolder<>(
                ResourceLocation.parse("bertieprogression:test_duplicate"),
                new StonecutterRecipe("", Ingredient.of(Items.OAK_PLANKS), new ItemStack(Items.OAK_SLAB, 1)));
        var unique = new RecipeHolder<>(
                ResourceLocation.parse("bertieprogression:test_unique"),
                new StonecutterRecipe("", Ingredient.of(Items.OAK_PLANKS), new ItemStack(Items.STICK, 3)));
        var stone = new RecipeHolder<>(
                ResourceLocation.parse("bertieprogression:test_stone"),
                new StonecutterRecipe("", Ingredient.of(Items.COBBLESTONE), new ItemStack(Items.COBBLESTONE_SLAB)));
        var result = CuttingRecipeRules.separate(
                List.of(wood, duplicate, unique, stone), helper.getLevel().registryAccess());
        helper.assertTrue(result.size() == 3, "an existing wood cut must suppress its duplicate");
        var moved = result.stream()
                .filter(holder -> holder.id().equals(unique.id()))
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                moved.value() instanceof WoodcuttingRecipe
                        && moved.value()
                                        .getResultItem(helper.getLevel().registryAccess())
                                        .getCount()
                                == 3,
                "unique wood cuts must preserve their output quantity");
        helper.assertTrue(result.contains(stone), "stone recipes must remain unchanged");
        var mixed = new RecipeHolder<>(
                ResourceLocation.parse("bertieprogression:test_mixed"),
                new StonecutterRecipe(
                        "", Ingredient.of(Items.OAK_PLANKS, Items.COBBLESTONE), new ItemStack(Items.STICK, 4)));
        var split =
                CuttingRecipeRules.separate(List.of(mixed), helper.getLevel().registryAccess());
        helper.assertTrue(split.size() == 2, "mixed ingredients must split into wood and stone cuts");
        for (var holder : split) {
            var choices = holder.value().getIngredients().getFirst();
            helper.assertTrue(
                    holder.value()
                                    .getResultItem(helper.getLevel().registryAccess())
                                    .getCount()
                            == 4,
                    "splitting must retain the original output count");
            helper.assertTrue(
                    holder.value() instanceof StonecutterRecipe
                            ? !choices.test(new ItemStack(Items.OAK_PLANKS))
                            : !choices.test(new ItemStack(Items.COBBLESTONE)),
                    "each cutter must accept only its matching material");
        }
        helper.assertTrue(
                CuttingRecipeRules.separate(result, helper.getLevel().registryAccess())
                        .equals(result),
                "cutting separation must be idempotent");
        helper.assertTrue(
                helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.STONECUTTING).stream()
                        .noneMatch(holder -> java.util.Arrays.stream(holder.value()
                                        .getIngredients()
                                        .getFirst()
                                        .getItems())
                                .anyMatch(stack -> stack.is(net.minecraft.tags.ItemTags.PLANKS)
                                        || stack.is(net.minecraft.tags.ItemTags.LOGS))),
                "the reloaded stonecutter must contain no wooden inputs");
        helper.succeed();
    }

    private static Warden warden(GameTestHelper helper) {
        Warden warden = helper.spawn(EntityType.WARDEN, new net.minecraft.core.BlockPos(2, 2, 2));
        warden.setNoAi(true);
        return warden;
    }

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "WardenTest")) {
            @Override
            public boolean isInvulnerableTo(DamageSource source) {
                return false;
            }
        };
        player.setGameMode(GameType.SURVIVAL);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        try {
            var grace = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            grace.setAccessible(true);
            grace.setInt(player, 0);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
        player.setHealth(200);
        player.setPos(helper.absoluteVec(new Vec3(2, 2, 2)));
        player.giveExperienceLevels(100);
        return player;
    }
}
