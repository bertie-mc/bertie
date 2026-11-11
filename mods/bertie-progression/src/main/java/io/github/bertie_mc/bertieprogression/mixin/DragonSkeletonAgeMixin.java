package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.dragon.DragonProgression;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "com.iafenvoy.iceandfire.world.feature.DragonSkeletonSpawnFeature", remap = false)
public abstract class DragonSkeletonAgeMixin {
    @ModifyVariable(method = "place", at = @At("STORE"), ordinal = 0, require = 1)
    private int bertie$limitGeneratedSkeletonAge(int age, FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return DragonProgression.generatedAge(
                age, context.level().getLevel().dimension().location());
    }
}
