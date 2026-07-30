package com.berlord.forgeink.mixin;

import com.berlord.forgeink.ForgeInk;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeLevel;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.entity.forge.input.HephaestusForgeInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Gates the forge's EXPERIENCE ("Ink") input at the source of truth: {@code getInput}
 * backs both the item handler's slot validity (players + hoppers) and the server
 * tick's consumption, so one injection covers insertion and processing alike.
 */
@Mixin(HephaestusForgeBlockEntity.class)
public abstract class HephaestusForgeBlockEntityMixin {

    @Shadow(remap = false)
    private HephaestusForgeLevel forgeLevel;

    @Inject(method = "getInput", at = @At("HEAD"), cancellable = true, remap = false)
    private void forgeink$onlyMatchingInk(Level level, ItemStack stack, EssenceType type,
                                          CallbackInfoReturnable<Optional<HephaestusForgeInput>> cir) {
        if (type == EssenceType.EXPERIENCE && !ForgeInk.allowExperienceInput(stack, this.forgeLevel)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
