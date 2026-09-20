package io.github.bertie_mc.armorcompletions;

import com.github.L_Ender.cataclysm.items.Bone_Reptile_Armor;
import io.github.bertie_mc.armorcompletions.client.CompletionClient;
import net.hazen.hazennstuff.Item.Armor.Misc.BishopOfDeceitArmor.BishopOfDeceitArmorItem;
import net.hazen.hazennstuff.Item.Armor.Misc.PyromancerBrute.PyromancerBruteArmorItem;
import net.hazen.hazennstuff.Item.Armor.Misc.NamelessOneArmor.NamelessOneArmorItem;
import net.hazen.hazennstuff.Item.Armor.Misc.NecromancerArmor.NecromancerArmorItem;
import net.mcreator.borninchaosv.item.SpinyShellArmorItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class CompletionItems {
    private CompletionItems() {}

    public static Item create(ArmorFamily family, ArmorItem.Type type) {
        Item.Properties properties = ArmorCompletions.properties(family, type);
        return switch (family) {
            case SPINY_SHELL -> new Shell(type, properties);
            case BONE_REPTILE -> new Reptile(type, properties);
            case BISHOP -> new Bishop(type, properties);
            case PYROMANCER_BRUTE -> new Pyromancer(type, properties);
            case NAMELESS_ONE -> new Nameless(type, properties);
            case NECROMANCER -> new Necromancer(type, properties);
        };
    }

    public static final class Shell extends SpinyShellArmorItem {
        Shell(Type type, Properties properties) { super(type, properties); }
        @Override
        public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean inner) {
            return ResourceLocation.fromNamespaceAndPath("born_in_chaos_v1", "textures/entities/spinyshellarmor" + getType().getName() + ".png");
        }
    }
    public static final class Reptile extends Bone_Reptile_Armor {
        Reptile(Type type, Properties properties) { super(ArmorCompletions.sourceArmor(ArmorFamily.BONE_REPTILE).getMaterial(), type, properties); }
        @Override
        public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean inner) {
            return ResourceLocation.fromNamespaceAndPath("cataclysm", "textures/armor/bone_reptile_completed.png");
        }
    }
    public static final class Bishop extends BishopOfDeceitArmorItem {
        Bishop(Type type, Properties properties) { super(type, properties); }
        @Override public GeoArmorRenderer<?> supplyRenderer() { return CompletionClient.geoRenderer(ArmorFamily.BISHOP); }
    }
    public static final class Pyromancer extends PyromancerBruteArmorItem {
        Pyromancer(Type type, Properties properties) { super(type, properties); }
        @Override public GeoArmorRenderer<?> supplyRenderer() { return CompletionClient.geoRenderer(ArmorFamily.PYROMANCER_BRUTE); }
    }
    public static final class Nameless extends NamelessOneArmorItem {
        Nameless(Type type, Properties properties) { super(type, properties); }
        @Override public GeoArmorRenderer<?> supplyRenderer() { return CompletionClient.geoRenderer(ArmorFamily.NAMELESS_ONE); }
    }
    public static final class Necromancer extends NecromancerArmorItem {
        Necromancer(Type type, Properties properties) { super(type, properties); }
        @Override public GeoArmorRenderer<?> supplyRenderer() { return CompletionClient.geoRenderer(ArmorFamily.NECROMANCER); }
    }
}
