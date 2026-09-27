package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.doll.DollLottery;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets {@link DollLottery} replace the doll a Kaleidoscope Doll Machine has just drawn. Targeted by
 * string because Kaleidoscope Doll is not on this module's compile classpath.
 */
@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopedoll.block.entity.DollMachineBlockEntity", remap = false)
public abstract class DollMachineLotteryMixin {
    @Shadow
    private int tier;

    @Inject(method = "onFinishLottery", at = @At("RETURN"), cancellable = true, require = 1)
    private void bertieprogression$drawSpecialDoll(CallbackInfoReturnable<ItemStack> cir) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && !level.isClientSide) {
            cir.setReturnValue(DollLottery.draw(cir.getReturnValue(), tier, level.getRandom()));
        }
    }
}
