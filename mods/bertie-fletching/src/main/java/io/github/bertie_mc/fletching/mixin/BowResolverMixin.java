package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.client.render.LayeredArrowModelResolver;
import com.fletchery.mod.client.render.LayeredBowModelCache;
import com.fletchery.mod.client.render.NockedArrowResolver;
import io.github.bertie_mc.fletching.client.CoatingModels;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LayeredArrowModelResolver.class, remap = false)
public abstract class BowResolverMixin {
    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true)
    private static void bertie$coatedBow(LivingEntity entity, ItemStack bow, CallbackInfoReturnable<BakedModel> cir) {
        if (!(entity instanceof Player player)
                || !(bow.getItem() instanceof BowItem)
                || !player.isUsingItem()
                || player.getUseItem().getItem() != bow.getItem()) return;
        var tag = NockedArrowResolver.resolve(player)
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag();
        if (!tag.getBoolean("bertieCoating")) return;
        int ticks = bow.getUseDuration(player) - player.getUseItemRemainingTicks();
        int stage = ticks >= 18 ? 2 : ticks >= 13 ? 1 : 0;
        BakedModel model = LayeredBowModelCache.getOrBuild(
                stage, tag.getString("shaft"), tag.getString("tip"), tag.getString("effect"), tag.getString("feather"));
        if (model != null)
            cir.setReturnValue(CoatingModels.coat(model, "bow_" + stage, tag.getInt("bertiePotionColor")));
    }
}
