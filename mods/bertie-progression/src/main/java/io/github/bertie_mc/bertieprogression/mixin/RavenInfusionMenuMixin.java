package io.github.bertie_mc.bertieprogression.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bertie_mc.bertieprogression.enchanting.RavenInfusion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu", remap = false)
public abstract class RavenInfusionMenuMixin {
    @Shadow
    @Final
    protected Player player;

    @Inject(
            method = {"slotsChanged", "setStats"},
            at = @At("TAIL"),
            require = 1)
    private void bertie$ravenLevelRequirement(CallbackInfo ci) {
        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;
        if (RavenInfusion.isInput(menu.getSlot(0).getItem()) && menu.enchantClue[2] >= 0) {
            var registry = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            var clue = registry.byId(menu.enchantClue[2]);
            if (clue != null
                    && ResourceLocation.fromNamespaceAndPath("apothic_enchanting", "infusion")
                            .equals(registry.getKey(clue))) {
                menu.costs[2] = RavenInfusion.LEVEL_COST;
            }
        }
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$requireRavenPayment(Player player, int option, CallbackInfoReturnable<Boolean> cir) {
        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;
        if (option == 2
                && RavenInfusion.isInput(menu.getSlot(0).getItem())
                && !player.getAbilities().instabuild
                && player.experienceLevel < RavenInfusion.LEVEL_COST) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(
            method = "lambda$clickMenuButton$0",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ldev/shadowsoffire/placebo/util/EnchantmentUtils;chargeExperience(Lnet/minecraft/world/entity/player/Player;I)Z"),
            require = 1)
    private boolean bertie$payRavenLevels(Player player, int points, Operation<Boolean> original) {
        if (RavenInfusion.isInput(((EnchantmentMenu) (Object) this).getSlot(0).getItem())) {
            if (!player.getAbilities().instabuild) {
                player.giveExperienceLevels(-RavenInfusion.LEVEL_COST);
            }
            return true;
        } else {
            return original.call(player, points);
        }
    }
}
