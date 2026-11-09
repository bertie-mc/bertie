package io.github.bertie_mc.bertieprogression.fountain;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.bertie_mc.bertieprogression.RemovedItems;
import io.github.bertie_mc.bertieprogression.mixin.FountainCountAccessor;
import io.github.bertie_mc.bertieprogression.mixin.FountainRarityAccessor;
import io.github.bertie_mc.bertieprogression.worldgen.RisingTungstenOreFeature;
import io.github.bertie_mc.bertieprogression.worldgen.SolarPoolOreFeature;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.neoforged.neoforge.common.Tags;

/** Dimension-specific ore weights derived from effective biome generation settings. */
public final class FountainOrePool {
    private static final Map<String, String> MERGES = merges();
    private final List<Entry> entries;

    private static final class Entry {
        private BlockState output;
        private final Set<Block> boosters = new HashSet<>();
        private double weight;

        private Entry(BlockState state) {
            output = state;
        }
    }

    private FountainOrePool(List<Entry> entries) {
        this.entries = entries;
    }

    public static FountainOrePool build(ServerLevel level) {
        Set<Holder<Biome>> biomes = new HashSet<>(
                level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes());
        var tag = level.dimension().equals(Level.OVERWORLD)
                ? BiomeTags.IS_OVERWORLD
                : level.dimension().equals(Level.NETHER)
                        ? BiomeTags.IS_NETHER
                        : level.dimension().equals(Level.END) ? BiomeTags.IS_END : null;
        if (tag != null) {
            level.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getTag(tag)
                    .ifPresent(set -> set.forEach(biomes::add));
        }
        Map<PlacedFeature, Integer> occurrences = new LinkedHashMap<>();
        for (var biome : biomes) {
            Set<PlacedFeature> present = new HashSet<>();
            biome.value()
                    .getGenerationSettings()
                    .features()
                    .forEach(set -> set.forEach(holder -> present.add(holder.value())));
            present.forEach(feature -> occurrences.merge(feature, 1, Integer::sum));
        }
        Map<String, Entry> weights = new LinkedHashMap<>();
        occurrences.forEach((feature, count) -> {
            double frequency = frequency(feature) * count / Math.max(1.0, biomes.size());
            feature.getFeatures().forEach(configured -> {
                if (configured.config() instanceof OreConfiguration ore) {
                    addTargets(
                            weights,
                            ore.targetStates.stream()
                                    .map(target -> target.state)
                                    .toList(),
                            frequency * ore.size);
                } else if (configured.feature() instanceof RisingTungstenOreFeature
                        && level.dimension().equals(Level.OVERWORLD)) {
                    addTargets(
                            weights,
                            List.of(BuiltInRegistries.BLOCK
                                    .get(ResourceLocation.parse("anvilcraft:deepslate_tungsten_ore"))
                                    .defaultBlockState()),
                            frequency * 2.5);
                } else if (configured.feature() instanceof SolarPoolOreFeature
                        && level.dimension().equals(Level.NETHER)) {
                    addTargets(
                            weights,
                            List.of(BuiltInRegistries.BLOCK
                                    .get(ResourceLocation.parse("hazennstuff:solar_core_blackstone_ore"))
                                    .defaultBlockState()),
                            frequency * 6);
                }
            });
        });
        return new FountainOrePool(
                weights.values().stream().filter(entry -> entry.weight > 0).toList());
    }

    public static double frequency(PlacedFeature feature) {
        double result = 1;
        for (var modifier : feature.placement()) {
            if (modifier instanceof CountPlacement) {
                var count = ((FountainCountAccessor) modifier).bertieprogression$count();
                result *= (count.getMinValue() + (double) count.getMaxValue()) / 2;
            } else if (modifier instanceof RarityFilter) {
                result /= Math.max(1, ((FountainRarityAccessor) modifier).bertieprogression$chance());
            }
        }
        return result;
    }

    private static void addTargets(Map<String, Entry> weights, List<BlockState> targets, double weight) {
        Map<String, List<BlockState>> groups = new HashMap<>();
        for (BlockState target : targets) {
            BlockState state = canonical(target);
            if (eligible(state))
                groups.computeIfAbsent(group(state), ignored -> new ArrayList<>())
                        .add(state);
        }
        groups.forEach((key, states) -> {
            Entry entry = weights.computeIfAbsent(key, ignored -> new Entry(states.getFirst()));
            entry.weight += weight;
            states.forEach(state -> {
                entry.boosters.add(state.getBlock());
                if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().startsWith("deepslate_"))
                    entry.output = state;
            });
        });
    }

    public BlockState select(List<BlockState> neighbors, RandomSource random, BlockState fallback) {
        double total = 0;
        List<Double> weights = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            int count = (int) neighbors.stream()
                    .filter(state -> entry.boosters.contains(canonical(state).getBlock()))
                    .count();
            double weight = FountainRules.oreWeight(entry.weight, count);
            weights.add(weight);
            total += weight;
        }
        if (total <= 0) return fallback;
        double roll = random.nextDouble() * total;
        for (int i = 0; i < entries.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) return entries.get(i).output;
        }
        return entries.getLast().output;
    }

    public int size() {
        return entries.size();
    }

    public Set<ResourceLocation> outputs() {
        return entries.stream()
                .map(entry -> BuiltInRegistries.BLOCK.getKey(entry.output.getBlock()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static boolean eligible(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return (state.is(Tags.Blocks.ORES) || id.endsWith("_ore"))
                && !RemovedItems.isRemoved(ResourceLocation.parse(id))
                && !id.equals("anvilcraft:earth_core_shard_ore")
                && !id.equals("anvilcraft:void_stone");
    }

    private static String group(BlockState state) {
        return state.getTags()
                .map(key -> key.location().toString())
                .filter(name -> name.startsWith("c:ores/"))
                .sorted()
                .findFirst()
                .orElseGet(() -> BuiltInRegistries.BLOCK
                        .getKey(state.getBlock())
                        .toString()
                        .replace("deepslate_", ""));
    }

    private static BlockState canonical(BlockState state) {
        String original = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String target = MERGES.get(original);
        return target == null
                ? state
                : BuiltInRegistries.BLOCK.get(ResourceLocation.parse(target)).defaultBlockState();
    }

    private static Map<String, String> merges() {
        try (var stream = FountainOrePool.class.getResourceAsStream("/merged_items.json")) {
            if (stream == null) return Map.of();
            return new Gson()
                    .fromJson(
                            new InputStreamReader(stream, StandardCharsets.UTF_8),
                            new TypeToken<Map<String, String>>() {}.getType());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load canonical ore mappings", e);
        }
    }
}
