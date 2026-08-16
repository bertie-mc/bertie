package io.github.bertie_mc.toolcraft.compat;

import io.github.bertie_mc.carving.Carving;
import io.github.bertie_mc.toolcraft.ToolcraftPolicy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.stln.magitech.MagitechRegistries;
import net.stln.magitech.item.component.ComponentInit;
import net.stln.magitech.item.component.MaterialComponent;
import net.stln.magitech.item.tool.material.ToolMaterial;

/**
 * Builds the Magitech part a carve produces. References Magitech and Carving classes, so it is only
 * ever reached from a path that has already checked both mods are loaded — when either is absent
 * this class never loads.
 *
 * <p>A Magitech part is one item per part type carrying its material in a component, exactly the way
 * Magitech's own part-cutting recipe leaves it: {@code MATERIAL_COMPONENT} and nothing else. Tier and
 * stats are read back off that component, so setting it is all there is to do.
 */
public final class MagitechParts {
    private static final String MAGITECH = "magitech";

    private MagitechParts() {}

    /**
     * @param carvingMaterialId the carving material id ({@code CarvingMaterial.id})
     * @param carvingToolId the carving tool id ({@code ToolKind.id})
     * @param flaws tier-1 carving errors, carried on the part
     * @param penalty tier-2 water-jet penalty steps, carried on the part
     * @return the part, or EMPTY when this material or kind has no Magitech equivalent — the caller
     *     then falls through to carving's own Slag/vanilla result
     */
    public static ItemStack build(String carvingMaterialId, String carvingToolId, int flaws, int penalty) {
        String materialPath = ToolcraftPolicy.magitechMaterial(carvingMaterialId);
        String partPath = ToolcraftPolicy.magitechPart(carvingToolId);
        if (materialPath == null || partPath == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MAGITECH, partPath));
        ToolMaterial material =
                MagitechRegistries.TOOL_MATERIAL.get(ResourceLocation.fromNamespaceAndPath(MAGITECH, materialPath));
        if (item == Items.AIR || material == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);
        stack.set(ComponentInit.MATERIAL_COMPONENT.get(), new MaterialComponent(material));
        // Carried, not yet spent: nothing assembles a flawed Magitech tool into a weaker one, the way
        // SlagCompat does for Slag. Keeping the components means the carve's cost is recorded on the
        // part for whenever that lands.
        if (flaws > 0) {
            stack.set(Carving.FLAWS.get(), flaws);
        }
        if (penalty > 0) {
            stack.set(Carving.PENALTY.get(), penalty);
        }
        return stack;
    }
}
