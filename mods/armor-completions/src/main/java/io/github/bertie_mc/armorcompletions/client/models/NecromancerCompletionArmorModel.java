package io.github.bertie_mc.armorcompletions.client.models;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
/** Companion model adapter for Hazen 'n Stuff 1.4.0.13 / GeckoLib 4. */
public class NecromancerCompletionArmorModel<T extends GeoItem> extends DefaultedEntityGeoModel<T> {
    public NecromancerCompletionArmorModel() {
        super(ResourceLocation.fromNamespaceAndPath("hazennstuff", "armor/necromancer_completed"));
    }
    @Override
    public ResourceLocation getModelResource(T item) {
        return ResourceLocation.fromNamespaceAndPath("hazennstuff", "geo/armor/necromancer_completed.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(T item) {
        return ResourceLocation.fromNamespaceAndPath("hazennstuff", "textures/armor/necromancer_completed.png");
    }
    @Override
    public ResourceLocation getAnimationResource(T item) {
        return ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "animations/wizard_armor_animation.json");
    }
}
