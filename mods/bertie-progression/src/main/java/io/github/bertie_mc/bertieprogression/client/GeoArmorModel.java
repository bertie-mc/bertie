package io.github.bertie_mc.bertieprogression.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * A humanoid armour model whose parts came from a mod's own Geckolib geometry.
 *
 * <p>The six anchors carry vanilla's names, so the layer poses this with the wearer and toggles
 * them per slot exactly as it does any other armour. What vanilla cannot decide is which plate
 * belongs to which slot when both hang off the same limb - the leg and the boot are both children
 * of the leg - so {@link #forSlot} makes that choice before the layer runs.
 */
public class GeoArmorModel extends HumanoidModel<LivingEntity> {

    private final Map<EquipmentSlot, List<ModelPart>> bySlot = new EnumMap<>(EquipmentSlot.class);
    private final List<ModelPart> switchable = new ArrayList<>();

    public GeoArmorModel(ModelPart root, Map<EquipmentSlot, List<String[]>> slotParts) {
        super(root);
        slotParts.forEach((slot, paths) -> {
            List<ModelPart> parts = new ArrayList<>();
            for (String[] path : paths) {
                ModelPart part = root;
                for (String step : path) {
                    part = part.getChild(step);
                }
                parts.add(part);
                switchable.add(part);
            }
            bySlot.put(slot, parts);
        });
    }

    /** Show only the plates belonging to this slot. The anchors are the layer's business. */
    public GeoArmorModel forSlot(EquipmentSlot slot) {
        for (ModelPart part : switchable) {
            part.visible = false;
        }
        for (ModelPart part : bySlot.getOrDefault(slot, List.of())) {
            part.visible = true;
        }
        return this;
    }
}
