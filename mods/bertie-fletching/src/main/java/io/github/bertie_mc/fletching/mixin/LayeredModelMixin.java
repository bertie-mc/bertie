package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.client.render.LayeredArrowInventoryModelCache;
import com.fletchery.mod.client.render.LayeredBakedModelFactory;
import com.fletchery.mod.client.render.LayeredBowModelCache;
import com.fletchery.mod.client.render.LayeredCrossbowArrowModelCache;
import io.github.bertie_mc.fletching.CoatingColor;
import io.github.bertie_mc.fletching.client.CoatingModels;
import net.minecraft.client.resources.model.BakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LayeredBakedModelFactory.class, remap = false)
public abstract class LayeredModelMixin {
    @Inject(method = "buildLayeredItemModel", at = @At("HEAD"), cancellable = true)
    private static void bertie$coatModel(
            BakedModel vanilla,
            String type,
            int stage,
            String feather,
            String shaft,
            String tip,
            String effect,
            String potion,
            CallbackInfoReturnable<BakedModel> cir) {
        var redesigned = io.github.bertie_mc.fletching.client.PartVisuals.model(
                type, stage, feather, shaft, tip, effect, potion);
        if (redesigned != null) {
            cir.setReturnValue(redesigned);
            return;
        }
        if (!CoatingColor.isEncoded(potion)) return;
        int color = CoatingColor.decode(potion).orElse(0xffffff);
        BakedModel base =
                switch (type) {
                    case "ARROW" -> LayeredArrowInventoryModelCache.getOrBuild(feather, shaft, tip, effect, "");
                    case "BOW" -> LayeredBowModelCache.getOrBuild(stage, shaft, tip, effect, feather, "");
                    case "CROSSBOW" -> LayeredCrossbowArrowModelCache.getOrBuild(feather, shaft, tip, effect);
                    default -> null;
                };
        String mask =
                switch (type) {
                    case "BOW" -> "bow_" + Math.clamp(stage, 0, 2);
                    case "CROSSBOW" -> "crossbow";
                    default -> "arrow";
                };
        cir.setReturnValue(base == null ? vanilla : CoatingModels.coat(base, mask, color));
    }
}
