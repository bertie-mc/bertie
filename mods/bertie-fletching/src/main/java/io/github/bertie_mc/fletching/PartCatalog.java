package io.github.bertie_mc.fletching;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class PartCatalog {
    public record Part(int slot, String key, String itemId, String visual, int tint, float damage, float penetration) {
        public ItemStack stack() {
            return BuiltInRegistries.ITEM
                    .getOptional(ResourceLocation.parse(itemId))
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
        }

        public String translation() {
            return "bertiefletching.part." + slot + "." + key;
        }
    }

    public static final List<Part> ALL = List.of(
            new Part(0, "wheat", "minecraft:wheat", "wheat", -1, 0f, 0f),
            new Part(0, "feather", "minecraft:feather", "feather", -1, 0f, 0f),
            new Part(0, "emu", "alexsmobs:emu_feather", "feather", 9598815, 0f, 0f),
            new Part(0, "raven", "twilightforest:raven_feather", "feather", 6510456, 0f, 0f),
            new Part(0, "roadrunner", "alexsmobs:roadrunner_feather", "feather", 13210465, 0f, 0f),
            new Part(0, "phantom", "minecraft:phantom_membrane", "phantom_membrane", -1, 0f, 0f),
            new Part(0, "stymphalian", "iceandfire:stymphalian_bird_feather", "feather", 11969641, 0f, 0f),
            new Part(0, "amphithere", "iceandfire:amphithere_feather", "feather", 6540140, 0f, 0f),
            new Part(0, "resplendent", "pastel:resplendent_feather", "phantom_membrane", 15181011, 0f, 0f),
            new Part(0, "sun", "l2complements:sun_membrane", "phantom_membrane", 16769929, 0f, 0f),
            new Part(0, "resonant", "l2complements:resonant_feather", "feather", 5823462, 0f, 0f),
            new Part(0, "neutronium", "avaritia_delight:neutronium_wheat", "wheat", 13685994, 0f, 0f),
            new Part(1, "stick", "minecraft:stick", "stick", -1, 0f, 0f),
            new Part(1, "fishing", "minecraft:fishing_rod", "fishing_rod", -1, 0f, 0f),
            new Part(1, "chain", "minecraft:chain", "chain", -1, 0f, 0f),
            new Part(1, "bone", "minecraft:bone", "bone", -1, 0f, 0f),
            new Part(1, "breeze", "minecraft:breeze_rod", "breeze_rod", -1, 0f, 0f),
            new Part(1, "dark", "born_in_chaos_v1:dark_rod", "blaze_rod", 7751274, 0f, 0f),
            new Part(1, "blaze", "minecraft:blaze_rod", "blaze_rod", -1, 0f, 0f),
            new Part(1, "end", "minecraft:end_rod", "end_rod", -1, 0f, 0f),
            new Part(1, "experience", "enderio:experience_rod", "end_rod", 11001704, 0f, 0f),
            new Part(1, "infinity", "enderio:infinity_rod", "end_rod", 8018833, 0f, 0f),
            new Part(1, "dielectric", "powah:dielectric_rod", "stick", 10778798, 0f, 0f),
            new Part(1, "ender", "enigmaticlegacyplus:ender_rod", "end_rod", 7458998, 0f, 0f),
            new Part(2, "flint", "minecraft:flint", "flint", -1, 0f, 0f),
            new Part(2, "copper", "minecraft:copper_ingot", "copper_ingot", -1, 1f, 0f),
            new Part(2, "amethyst", "minecraft:amethyst_shard", "amethyst_shard", -1, 1f, 0f),
            new Part(2, "prismarine", "minecraft:prismarine_shard", "prismarine_shard", -1, 1f, 0f),
            new Part(2, "iron", "minecraft:iron_ingot", "iron_ingot", -1, 2f, 0f),
            new Part(2, "quartz", "minecraft:quartz", "quartz", -1, 2f, 0f),
            new Part(2, "gold", "minecraft:gold_ingot", "gold_ingot", -1, 3f, 0f),
            new Part(2, "gargoyle", "mythsandlegends:gargoyle_shard", "heavy_core", 9406874, 6f, 0f),
            new Part(2, "permafrost", "irons_spellbooks:permafrost_shard", "prismarine_shard", 11594490, 3f, 0f),
            new Part(2, "star", "pastel:star_fragment", "diamond", 16770978, 4f, 0f),
            new Part(2, "diamond", "minecraft:diamond", "diamond", -1, 4f, 0.2f),
            new Part(2, "mnemonic", "malum:mnemonic_fragment", "amethyst_shard", 14195171, 0f, 0f),
            new Part(2, "abyssal", "aquamirae:abyssal_amethyst", "amethyst_shard", 6716619, 4f, 0f),
            new Part(2, "onyx", "pastel:onyx_shard", "flint", 6903172, 3f, 0f),
            new Part(2, "echo", "minecraft:echo_shard", "echo_shard", -1, 5f, 0.25f),
            new Part(2, "moonstone", "pastel:moonstone_shard", "quartz", 12897023, 5f, 0.1f),
            new Part(2, "netherite", "minecraft:netherite_ingot", "netherite_ingot", -1, 8f, 0.35f),
            new Part(2, "resonance", "pastel:resonance_shard", "echo_shard", 14658285, 6f, 0.2f),
            new Part(2, "reinforced_echo", "deeperdarker:reinforced_echo_shard", "echo_shard", 7919304, 10f, 0.3f),
            new Part(2, "heavy", "minecraft:heavy_core", "heavy_core", -1, 0f, 0f),
            new Part(2, "divine", "irons_spellbooks:divine_soulshard", "diamond", 16772558, 0f, 0f),
            new Part(2, "eldritch", "discerning_the_eldritch:eldritch_soul_shard", "diamond", 9617571, 0f, 0f),
            new Part(3, "slime", "minecraft:slime_ball", "slime_ball", -1, 0f, 0f),
            new Part(3, "honey", "minecraft:honeycomb", "honeycomb", -1, 0f, 0f),
            new Part(3, "pearl", "minecraft:ender_pearl", "ender_pearl", -1, 0f, 0f),
            new Part(3, "blaze_powder", "minecraft:blaze_powder", "blaze_powder", -1, 0f, 0f),
            new Part(3, "dragon", "minecraft:dragon_breath", "dragon_breath", -1, 0f, 0f),
            new Part(3, "sea", "anvilcraft:sea_heart_shell_shard", "heart_of_the_sea", 5823958, 0f, 0f),
            new Part(3, "wind", "minecraft:wind_charge", "wind_charge", -1, 0f, 0f),
            new Part(3, "lapis", "minecraft:lapis_lazuli", "lapis_lazuli", -1, 0f, 0f),
            new Part(3, "torch", "minecraft:torch", "torch", -1, 0f, 0f),
            new Part(3, "tnt", "minecraft:tnt", "gunpowder", 16365232, 0f, 0f),
            new Part(3, "earth", "enigmaticlegacyplus:earth_heart_fragment", "gunpowder", 13805671, 0f, 0f),
            new Part(3, "sponge", "minecraft:sponge", "honeycomb", 14340477, 0f, 0f),
            new Part(3, "dread", "iceandfire:dread_shard", "dragon_breath", 6269392, 0f, 0f),
            new Part(3, "lightning", "irons_spellbooks:lightning_bottle", "glowstone_dust", 9882879, 0f, 0f),
            new Part(3, "blood", "irons_spellbooks:blood_vial", "slime_ball", 13390689, 0f, 0f),
            new Part(3, "fiery_blood", "twilightforest:fiery_blood", "blaze_powder", 15964003, 0f, 0f),
            new Part(3, "magnet", "anvilcraft:magnet_ingot", "ender_pearl", 14516601, 0f, 0f),
            new Part(3, "brilliance", "malum:refined_brilliance", "glowstone_dust", 16768420, 0f, 0f));

    private PartCatalog() {}

    public static List<Part> inSlot(int slot) {
        return ALL.stream().filter(part -> part.slot() == slot).toList();
    }

    public static Part find(int slot, String id) {
        return ALL.stream()
                .filter(part -> part.slot() == slot && part.itemId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static Part key(int slot, String key) {
        return ALL.stream()
                .filter(part -> part.slot() == slot && part.key().equals(key))
                .findFirst()
                .orElseThrow();
    }

    public static Part find(int slot, ItemStack stack) {
        return stack.isEmpty()
                ? null
                : find(slot, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
}
