package io.github.bertie_mc.fletching.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.bertie_mc.fletching.ArrowProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BowItem.class, remap = false)
public abstract class DrawSpeedMixin {
    @ModifyExpressionValue(
            method = "releaseUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
    private float bertie$draw(float original, ItemStack bow, Level level, LivingEntity shooter, int remaining) {
        if (shooter instanceof Player player) {
            var data = player.getProjectile(bow)
                    .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag();
            if (ArrowProfile.redesigned(data) && ArrowProfile.read(data).tip("gargoyle"))
                return BowItem.getPowerForTime((int) ((bow.getUseDuration(shooter) - remaining) * .8));
        }
        return original;
    }
}
