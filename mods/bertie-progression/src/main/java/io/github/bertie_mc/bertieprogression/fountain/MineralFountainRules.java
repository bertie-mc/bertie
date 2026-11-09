package io.github.bertie_mc.bertieprogression.fountain;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Optional AnvilCraft integration without a dependency on its nested recipe library. */
public final class MineralFountainRules {
    private static final ResourceLocation CORE_ORE = ResourceLocation.parse("anvilcraft:earth_core_shard_ore");
    private static final ResourceLocation VOID_ORE = ResourceLocation.parse("anvilcraft:void_stone");
    private static final ResourceLocation CORE_BLOCK = ResourceLocation.parse("anvilcraft:earth_core_shard_block");
    private static final ResourceLocation VOID_BLOCK = ResourceLocation.parse("anvilcraft:void_matter_block");
    private static final Map<ServerLevel, DimensionData> DATA = new WeakHashMap<>();

    private record Chance(Recipe<?> recipe, Object predicate, Method matches, Method chance, ResourceLocation result) {
        double evaluate(ServerLevel level, BlockState state) {
            try {
                return (boolean) matches.invoke(predicate, level, state, null)
                        ? ((Number) chance.invoke(recipe, level)).doubleValue()
                        : 0;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot evaluate AnvilCraft fountain chance", e);
            }
        }
    }

    private record DimensionData(FountainOrePool ores, List<Chance> chances) {}

    public record Probabilities(double core, double voidChance) {}

    public static boolean process(ServerLevel level, BlockPos pos, List<BlockState> neighbors) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMinBuildHeight() + 8) return false;
        BlockState input = level.getBlockState(pos.above());
        if (!input.is(Blocks.DEEPSLATE)) return false;
        DimensionData data = DATA.computeIfAbsent(level, MineralFountainRules::build);
        Probabilities probabilities = probabilities(level, input, neighbors);
        BlockState sturdy = block(SturdyDeepslateRules.ID);
        BlockState result =
                switch (FountainRules.roll(probabilities.core, probabilities.voidChance, level.random::nextDouble)) {
                    case CORE -> block(CORE_ORE);
                    case VOID -> block(VOID_ORE);
                    case ORE -> data.ores.select(neighbors, level.random, sturdy);
                    case STURDY -> sturdy;
                };
        level.setBlockAndUpdate(pos.above(), result);
        return true;
    }

    public static Probabilities probabilities(ServerLevel level, BlockState input, List<BlockState> neighbors) {
        DimensionData data = DATA.computeIfAbsent(level, MineralFountainRules::build);
        double core = 0, voidChance = 0;
        for (Chance entry : data.chances) {
            double chance = Math.clamp(entry.evaluate(level, input), 0, 1);
            if (entry.result.equals(CORE_ORE)) core = 1 - (1 - core) * (1 - chance);
            if (entry.result.equals(VOID_ORE)) voidChance = 1 - (1 - voidChance) * (1 - chance);
        }
        core = FountainRules.specialChance(core, count(neighbors, CORE_BLOCK));
        voidChance = FountainRules.specialChance(voidChance, count(neighbors, VOID_BLOCK));
        return new Probabilities(core, voidChance);
    }

    private static DimensionData build(ServerLevel level) {
        List<Chance> chances = new ArrayList<>();
        level.getRecipeManager().getRecipes().forEach(holder -> {
            Recipe<?> recipe = holder.value();
            if (!BuiltInRegistries.RECIPE_TYPE
                    .getKey(recipe.getType())
                    .toString()
                    .equals("anvilcraft:mineral_fountain_chance")) return;
            try {
                Class<?> type = recipe.getClass();
                if (!type.getMethod("getDimension")
                        .invoke(recipe)
                        .equals(level.dimension().location())) return;
                Object target = type.getMethod("getToBlock").invoke(recipe);
                BlockState result =
                        (BlockState) target.getClass().getMethod("state").invoke(target);
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(result.getBlock());
                if (!id.equals(CORE_ORE) && !id.equals(VOID_ORE)) return;
                Object predicate = type.getMethod("getFromBlock").invoke(recipe);
                chances.add(new Chance(
                        recipe,
                        predicate,
                        predicate
                                .getClass()
                                .getMethod("test", LevelAccessor.class, BlockState.class, BlockEntity.class),
                        type.getMethod("getChance", ServerLevel.class),
                        id));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unsupported AnvilCraft fountain recipe API", e);
            }
        });
        return new DimensionData(FountainOrePool.build(level), List.copyOf(chances));
    }

    private static int count(List<BlockState> states, ResourceLocation id) {
        return (int) states.stream()
                .filter(state -> id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock())))
                .count();
    }

    public static BlockState block(ResourceLocation id) {
        return BuiltInRegistries.BLOCK.get(id).defaultBlockState();
    }

    @SubscribeEvent
    public static void reload(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) DATA.clear();
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        DATA.remove(event.getLevel());
    }

    private MineralFountainRules() {}
}
