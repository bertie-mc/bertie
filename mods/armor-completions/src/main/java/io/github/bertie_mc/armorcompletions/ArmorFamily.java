package io.github.bertie_mc.armorcompletions;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;

public enum ArmorFamily {
    SPINY_SHELL("spiny_shell", "Spiny Shell", "born_in_chaos_v1:spiny_shell_armor_helmet", "born_in_chaos_v1:spiny_shell_armor_chestplate", null),
    BISHOP("bishop", "Bishop", "hazennstuff:bishop_of_deceit_helmet", "hazennstuff:bishop_of_deceit_chestplate", null),
    BONE_REPTILE("bone_reptile", "Bone Reptile", "cataclysm:bone_reptile_helmet", "cataclysm:bone_reptile_chestplate", null),
    PYROMANCER_BRUTE("pyromancer_brute", "Pyromancer Brute", "hazennstuff:pyromancer_brute_helmet", "hazennstuff:pyromancer_brute_chestplate", null),
    NAMELESS_ONE("nameless_one", "Nameless One", "hazennstuff:nameless_one_helmet", "hazennstuff:nameless_one_chestplate", "hazennstuff:nameless_one_leggings"),
    NECROMANCER("necromancer", "Necromancer", "hazennstuff:necromancer_helmet", "hazennstuff:necromancer_chestplate", "hazennstuff:necromancer_leggings");

    public final String id;
    public final String displayName;
    public final ResourceLocation helmet;
    public final ResourceLocation chestplate;
    public final ResourceLocation leggings;

    ArmorFamily(String id, String displayName, String helmet, String chestplate, String leggings) {
        this.id = id;
        this.displayName = displayName;
        this.helmet = ResourceLocation.parse(helmet);
        this.chestplate = ResourceLocation.parse(chestplate);
        this.leggings = leggings == null ? null : ResourceLocation.parse(leggings);
    }

    public List<ArmorItem.Type> missingTypes() {
        return leggings == null ? List.of(ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS) : List.of(ArmorItem.Type.BOOTS);
    }

    public String itemName(ArmorItem.Type type) {
        return id + "_" + type.getName();
    }
}
