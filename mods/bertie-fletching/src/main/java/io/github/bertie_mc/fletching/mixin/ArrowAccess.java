package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.entity.CustomArrowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CustomArrowEntity.class, remap = false)
public interface ArrowAccess {
    @Invoker("applyPotionEffects")
    void bertie$potions(LivingEntity target);

    @Invoker("onHitEntity")
    void bertie$hit(EntityHitResult hit);

    @Invoker("replaceBlocksWithTemporaryHoney")
    static boolean bertie$honey(ServerLevel level, BlockPos pos) {
        throw new AssertionError();
    }
}
