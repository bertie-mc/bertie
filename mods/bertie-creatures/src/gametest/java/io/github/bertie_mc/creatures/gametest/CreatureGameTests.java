package io.github.bertie_mc.creatures.gametest;

import io.github.bertie_mc.creatures.server.block.ACBlockRegistry;
import io.github.bertie_mc.creatures.server.block.grower.ThornwoodGrower;
import io.github.bertie_mc.creatures.server.entity.ACEntityRegistry;
import io.github.bertie_mc.creatures.server.entity.living.*;
import io.github.bertie_mc.creatures.server.entity.util.AlexsCavesBoat;
import io.github.bertie_mc.creatures.server.item.ACItemRegistry;
import io.github.bertie_mc.creatures.server.potion.ACEffectRegistry;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertiecreatures")
@PrefixGameTestTemplate(false)
public final class CreatureGameTests {
    private static final List<String> MOBS = List.of(
            "deep_one_mage",
            "hullbreaker",
            "vesper",
            "nucleeper",
            "luxtructosaurus",
            "tremorsaurus",
            "grottoceratops",
            "atlatitan");

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath("bertiecreatures", name);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void allEightMobsSpawnSaveAndTickWithoutAlexsCaves(GameTestHelper helper) {
        helper.assertTrue(!ModList.get().isLoaded("alexscaves"), "Full Alex's Caves must be absent");
        helper.getLevel()
                .getGameRules()
                .getRule(GameRules.RULE_MOBGRIEFING)
                .set(false, helper.getLevel().getServer());
        List<Mob> spawned = new ArrayList<>();
        for (int i = 0; i < MOBS.size(); i++) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id(MOBS.get(i)));
            var mob = (Mob) type.create(helper.getLevel());
            helper.assertTrue(mob != null && mob.getMaxHealth() > 0, "Missing entity attributes: " + MOBS.get(i));
            BlockPos pos = helper.absolutePos(new BlockPos(5 + (i % 4) * 12, 8, 5 + (i / 4) * 18));
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            mob.setNoGravity(true);
            mob.setInvulnerable(true);
            mob.setPersistenceRequired();
            helper.getLevel().addFreshEntity(mob);
            var saved = new CompoundTag();
            helper.assertTrue(mob.save(saved), "Entity must save: " + MOBS.get(i));
            Entity restored = EntityType.loadEntityRecursive(saved, helper.getLevel(), entity -> entity);
            helper.assertTrue(restored != null && restored.getType() == type, "Entity must reload: " + MOBS.get(i));
            spawned.add(mob);
        }
        helper.runAfterDelay(25, () -> {
            for (var mob : spawned)
                helper.assertTrue(mob.isAlive() && mob.tickCount >= 20, "Mob stopped ticking: " + mob.getType());
            spawned.forEach(Entity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void lootKeepsRequestedDrops(GameTestHelper helper) {
        Map<String, Set<Item>> allowed = Map.of(
                "hullbreaker", Set.of(ACItemRegistry.SEA_GLASS_SHARDS.get()),
                "vesper", Set.of(ACItemRegistry.VESPER_WING.get()),
                "nucleeper", Set.of(ACItemRegistry.FISSILE_CORE.get(), Items.GUNPOWDER),
                "luxtructosaurus", Set.of(ACItemRegistry.TECTONIC_SHARD.get()),
                "tremorsaurus", Set.of(ACBlockRegistry.DINOSAUR_CHOP.get().asItem(), ACItemRegistry.HEAVY_BONE.get()),
                "grottoceratops", Set.of(ACBlockRegistry.DINOSAUR_CHOP.get().asItem(), ACItemRegistry.TOUGH_HIDE.get()),
                "atlatitan", Set.of(ACBlockRegistry.DINOSAUR_CHOP.get().asItem(), ACItemRegistry.HEAVY_BONE.get()));
        for (var entry : allowed.entrySet()) {
            var entity = BuiltInRegistries.ENTITY_TYPE.get(id(entry.getKey())).create(helper.getLevel());
            var key = ResourceKey.create(Registries.LOOT_TABLE, id("entities/" + entry.getKey()));
            var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
            Set<Item> seen = new HashSet<>();
            for (int seed = 0; seed < 64; seed++) {
                var params = new LootParams.Builder(helper.getLevel())
                        .withParameter(LootContextParams.THIS_ENTITY, entity)
                        .withParameter(LootContextParams.ORIGIN, entity.position())
                        .withParameter(
                                LootContextParams.DAMAGE_SOURCE,
                                helper.getLevel().damageSources().generic())
                        .create(LootContextParamSets.ENTITY);
                for (var stack : table.getRandomItems(params, seed + 1)) {
                    if (stack.isEmpty()) continue;
                    helper.assertTrue(
                            entry.getValue().contains(stack.getItem()),
                            "Unexpected " + stack + " in " + entry.getKey() + " loot");
                    if (!stack.isEmpty()) seen.add(stack.getItem());
                    if (entry.getKey().equals("hullbreaker"))
                        helper.assertTrue(stack.getCount() >= 2 && stack.getCount() <= 5, "Sea glass count changed");
                    if (entry.getKey().equals("luxtructosaurus"))
                        helper.assertTrue(
                                stack.getCount() >= 7 && stack.getCount() <= 11, "Tectonic shard count changed");
                }
            }
            helper.assertTrue(seen.equals(entry.getValue()), "Missing drops for " + entry.getKey() + ": " + seen);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipesUseSelectedIngredients(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var chop = new ItemStack(ACBlockRegistry.COOKED_DINOSAUR_CHOP.get());
        var good = CraftingInput.of(
                2,
                2,
                List.of(
                        chop.copy(),
                        chop.copy(),
                        new ItemStack(ACItemRegistry.HEAVY_BONE.get()),
                        new ItemStack(Items.BOWL)));
        var recipe = recipes.getRecipeFor(RecipeType.CRAFTING, good, helper.getLevel());
        helper.assertTrue(
                recipe.isPresent()
                        && recipe.get()
                                .value()
                                .assemble(good, helper.getLevel().registryAccess())
                                .is(ACItemRegistry.SEETHING_STEW.get()),
                "Two cooked chops must craft seething stew");
        var bad = CraftingInput.of(
                2,
                2,
                List.of(
                        chop.copy(),
                        ItemStack.EMPTY,
                        new ItemStack(ACItemRegistry.HEAVY_BONE.get()),
                        new ItemStack(Items.BOWL)));
        helper.assertTrue(
                recipes.getRecipeFor(RecipeType.CRAFTING, bad, helper.getLevel())
                        .isEmpty(),
                "One chop must not suffice");
        var vesper = CraftingInput.of(
                2,
                2,
                List.of(
                        new ItemStack(ACItemRegistry.VESPER_WING.get()),
                        new ItemStack(ACBlockRegistry.THORNWOOD_BRANCH.get()),
                        new ItemStack(Items.BROWN_MUSHROOM),
                        new ItemStack(Items.BOWL)));
        helper.assertTrue(
                recipes.getRecipeFor(RecipeType.CRAFTING, vesper, helper.getLevel())
                        .orElseThrow()
                        .value()
                        .assemble(vesper, helper.getLevel().registryAccess())
                        .is(ACItemRegistry.VESPER_STEW.get()),
                "Vesper stew recipe failed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void thornwoodSignsBoatsAndSaplingWork(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 2, 2), ACBlockRegistry.THORNWOOD_SIGN.get());
        helper.assertTrue(
                helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 2, 2))) instanceof SignBlockEntity,
                "Thornwood sign must have a valid block entity");
        var boat = ACEntityRegistry.BOAT.get().create(helper.getLevel());
        helper.assertTrue(boat.getACBoatType() == AlexsCavesBoat.Type.THORNWOOD, "Boat variant must be Thornwood");
        helper.assertTrue(
                boat.getACBoatType().getPlankSupplier().get() == ACBlockRegistry.THORNWOOD_PLANKS.get(),
                "Boat must use Thornwood planks");
        BlockPos tree = helper.absolutePos(new BlockPos(25, 2, 25));
        for (int x = -12; x <= 12; x++)
            for (int z = -12; z <= 12; z++)
                helper.getLevel().setBlockAndUpdate(tree.offset(x, -1, z), Blocks.DIRT.defaultBlockState());
        var feature = helper.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ThornwoodGrower.THORNWOOD_TREE);
        boolean grew = feature.place(
                helper.getLevel(),
                helper.getLevel().getChunkSource().getGenerator(),
                net.minecraft.util.RandomSource.create(42),
                tree);
        helper.assertTrue(grew, "Thornwood tree must grow without cave blocks");
        helper.assertTrue(
                helper.getLevel().getBlockState(tree).is(ACBlockRegistry.THORNWOOD_LOG.get())
                        || helper.getLevel().getBlockState(tree).is(ACBlockRegistry.THORNWOOD_WOOD.get()),
                "Tree generated no trunk");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void multipartDamageAndVanillaTamingWork(GameTestHelper helper) {
        var clientCopy = ACEntityRegistry.HULLBREAKER.get().create(helper.getLevel());
        clientCopy.setId(900000);
        for (int i = 0; i < clientCopy.getParts().length; i++)
            helper.assertTrue(
                    clientCopy.getParts()[i].getId() == 900001 + i, "Part IDs must follow the parent's network ID");
        var hull = helper.spawn(ACEntityRegistry.HULLBREAKER.get(), new BlockPos(5, 4, 5));
        float before = hull.getHealth();
        hull.getParts()[0].hurt(helper.getLevel().damageSources().generic(), 8);
        helper.assertTrue(hull.getHealth() < before, "Hullbreaker parts must forward server damage");
        var atlatitan = helper.spawn(ACEntityRegistry.ATLATITAN.get(), new BlockPos(20, 4, 20));
        before = atlatitan.getHealth();
        atlatitan.getParts()[0].hurt(helper.getLevel().damageSources().generic(), 8);
        helper.assertTrue(atlatitan.getHealth() < before, "Sauropod parts must forward server damage");
        var tremor = helper.spawn(ACEntityRegistry.TREMORSAURUS.get(), new BlockPos(40, 4, 40));
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        tremor.setTameAttempts(8);
        tremor.addEffect(new MobEffectInstance(ACEffectRegistry.STUNNED, 200));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_CARROT, 2));
        tremor.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                tremor.isTame() && player.getUUID().equals(tremor.getOwnerUUID()),
                "Golden carrot must reach original taming behavior");
        helper.assertTrue(!tremor.hasEffect(ACEffectRegistry.STUNNED), "Taming food must clear stun");
        helper.assertTrue(atlatitan.isFood(new ItemStack(Items.WHEAT)), "Atlatitan must eat wheat");
        hull.discard();
        atlatitan.discard();
        tremor.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nucleeperDetonatesWithoutWorldgen(GameTestHelper helper) {
        helper.getLevel()
                .getGameRules()
                .getRule(GameRules.RULE_MOBGRIEFING)
                .set(false, helper.getLevel().getServer());
        var nucleeper = helper.spawn(ACEntityRegistry.NUCLEEPER.get(), new BlockPos(20, 5, 20));
        nucleeper.setNoAi(true);
        nucleeper.setNoGravity(true);
        nucleeper.setTriggered(true);
        nucleeper.setCloseTime(io.github.bertie_mc.creatures.BertieCreatures.COMMON_CONFIG.nucleeperFuseTime.get());
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(nucleeper.isRemoved(), "Nucleeper must complete its fuse");
            var explosions = helper.getLevel()
                    .getEntitiesOfClass(
                            io.github.bertie_mc.creatures.server.entity.item.NuclearExplosionEntity.class,
                            nucleeper.getBoundingBox().inflate(8));
            helper.assertTrue(explosions.size() == 1, "Nucleeper must create its nuclear explosion");
            helper.assertTrue(explosions.getFirst().isNoGriefing(), "Explosion must honor mobGriefing=false");
        });
        helper.runAfterDelay(60, helper::succeed);
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void bubbleDrainsAirAndRageExpires(GameTestHelper helper) {
        var cow = helper.spawn(EntityType.COW, new BlockPos(5, 4, 5));
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.setAirSupply(100);
        cow.addEffect(new MobEffectInstance(ACEffectRegistry.BUBBLED, 200));
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(40, 4, 40));
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        zombie.setHealth(10);
        zombie.addEffect(new MobEffectInstance(ACEffectRegistry.RAGE, 5));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(cow.getAirSupply() < 100, "Mage bubble must prevent normal air recovery");
            var attack = zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            helper.assertTrue(
                    attack.getModifier(id("rage_attack_boost")) == null,
                    "Rage modifier must disappear with its effect");
            cow.discard();
            zombie.discard();
            helper.succeed();
        });
    }
}
