package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.client.render.LayeredArrowInventoryModelCache;
import com.fletchery.mod.client.render.LayeredBakedModelFactory;
import com.fletchery.mod.client.render.LayeredBowModelCache;
import com.fletchery.mod.client.render.LayeredCrossbowArrowModelCache;
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
        if (potion == null || !potion.matches("#[0-9a-fA-F]{1,6}")) return;
        int color = Integer.parseInt(potion.substring(1), 16);
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
