package io.github.bertie_mc.emi.integration.anvilcraft;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * AnvilCraft's Crab Trap: a block placed in water that catches things over time, and the only source
 * of the Crab Claw. It has no recipe — the catch comes from six {@code gameplay/crab_trap} loot
 * tables, one common and five keyed to the biome the trap sits in.
 *
 * <p>The tables are written out here rather than read at runtime because loot tables are server data
 * and are never sent to the client, so there is nothing for a client-side plugin to read. They are
 * fixed content of the mod, and the entries below are its 1.5.3 tables verbatim.
 */
public final class AnvilCraftCrabTrapEmiModule {

    /** Every pool rolls behind the same 10% check, so a trap catches on one attempt in ten. */
    private static final double ATTEMPT = 0.1;

    private AnvilCraftCrabTrapEmiModule() {}

    /** One pool: the biomes it needs (empty means anywhere) and its weighted catch. */
    private record Pool(String name, String biomes, List<Catch> entries) {}

    private record Catch(String item, int weight) {}

    private static final List<Pool> POOLS = List.of(
            new Pool("common", "", List.of(new Catch("anvilcraft:crab_claw", 1), new Catch("minecraft:seagrass", 15))),
            new Pool("river", "Rivers, frozen or not", List.of(new Catch("minecraft:salmon", 1))),
            new Pool("swamp", "Swamps and mangrove swamps", List.of(new Catch("minecraft:lily_pad", 1))),
            new Pool(
                    "jungle",
                    "Jungles, including bamboo and sparse",
                    List.of(new Catch("minecraft:bamboo", 1), new Catch("minecraft:cocoa_beans", 1))),
            new Pool(
                    "ocean",
                    "Oceans that are not warm, including cold, deep and frozen",
                    List.of(
                            new Catch("minecraft:cod", 1),
                            new Catch("minecraft:kelp", 1),
                            new Catch("minecraft:nautilus_shell", 1),
                            new Catch("minecraft:ink_sac", 1))),
            new Pool(
                    "warm_ocean",
                    "Warm, lukewarm and deep lukewarm oceans",
                    List.of(
                            new Catch("minecraft:tropical_fish", 1),
                            new Catch("minecraft:pufferfish", 1),
                            new Catch("minecraft:ink_sac", 1))));

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory trap = Categories.machine(reg, "anvilcraft_crab_trap", "anvilcraft:crab_trap", "Crab Trap");
        for (Pool pool : POOLS) {
            int total = pool.entries().stream().mapToInt(Catch::weight).sum();
            MachineDescriptor d = new MachineDescriptor();
            boolean any = false;
            for (Catch entry : pool.entries()) {
                EmiStack stack = Categories.stack(entry.item());
                if (stack.isEmpty()) {
                    continue;
                }
                d.itemOut(stack.copy().setChance((float) (ATTEMPT * entry.weight() / total)));
                any = true;
            }
            if (!any) {
                continue;
            }
            d.catalyst(Categories.stack("anvilcraft:crab_trap"));
            d.info(Component.literal(pool.biomes().isEmpty() ? "In water anywhere" : "In water in: " + pool.biomes()));
            d.info(Component.literal("One attempt in ten catches something"));
            reg.addRecipe(new GenericEmiRecipe(
                    trap,
                    ResourceLocation.fromNamespaceAndPath("bertieemi", "anvilcraft/crab_trap/" + pool.name()),
                    d));
        }
    }
}
