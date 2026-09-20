package io.github.bertie_mc.creatures.server.item;

import io.github.bertie_mc.creatures.server.potion.ACEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ACFoods {
    public static final FoodProperties DINOSAUR_NUGGETS = (new FoodProperties.Builder())
            .nutrition(3)
            .saturationModifier(0.3F)
            .fast()
            .build();
    public static final FoodProperties SEETHING_STEW = (new FoodProperties.Builder())
            .nutrition(6)
            .saturationModifier(0.6F)
            .effect(() -> new MobEffectInstance(ACEffectRegistry.RAGE, 2200), 1.0F)
            .build();
    public static final FoodProperties VESPER_WING = (new FoodProperties.Builder())
            .nutrition(3)
            .saturationModifier(0.2F)
            .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 1200), 1.0F)
            .build();
    public static final FoodProperties VESPER_SOUP = (new FoodProperties.Builder())
            .usingConvertsTo(net.minecraft.world.item.Items.BOWL)
            .nutrition(5)
            .saturationModifier(0.3F)
            .alwaysEdible()
            .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 2400), 1.0F)
            .build();
}
