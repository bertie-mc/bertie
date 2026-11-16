package io.github.bertie_mc.bertieprogression.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.SlotWidget;
import io.github.bertie_mc.bertieprogression.recipe.ReusableTableRecipe;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "committee.nova.mods.avaritia.init.compat.emi.category.tables.EndCraftingTableCategory", remap = false)
public abstract class ReusableTableEmiMixin {
    @Shadow
    @Final
    private RecipeHolder<?> recipe;

    @Inject(method = "getInputs", at = @At("RETURN"), require = 1)
    private void bertieprogression$remainder(CallbackInfoReturnable<List<EmiIngredient>> callback) {
        if (!(recipe.value() instanceof ReusableTableRecipe reusable)) return;
        for (var ingredient : callback.getReturnValue())
            for (var stack : ingredient.getEmiStacks()) {
                if (reusable.isCatalyst(stack.getItemStack()))
                    stack.setRemainder(stack.copy().setAmount(1));
            }
    }

    @ModifyExpressionValue(
            method = "addWidgets",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ldev/emi/emi/api/widget/WidgetHolder;addSlot(Ldev/emi/emi/api/stack/EmiIngredient;II)Ldev/emi/emi/api/widget/SlotWidget;"),
            require = 1)
    private SlotWidget bertieprogression$catalyst(SlotWidget slot) {
        if (recipe.value() instanceof ReusableTableRecipe reusable
                && slot.getStack().getEmiStacks().stream()
                        .anyMatch(stack -> reusable.isCatalyst(stack.getItemStack()))) {
            slot.catalyst(true);
        }
        return slot;
    }
}
