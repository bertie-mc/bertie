package io.github.bertie_mc.toolcraft.mixin.magitech;

import io.github.bertie_mc.toolcraft.ToolcraftPolicy;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.stln.magitech.item.tool.ToolType;
import net.stln.magitech.item.tool.toolitem.PartToolItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the four kept Magitech tools enchantable.
 *
 * <p>Magitech blocks enchanting outright — {@code isEnchantable} and {@code supportsEnchantment}
 * are both hardcoded {@code return false}, because its trait and upgrade systems are meant to
 * replace enchanting. Bertie keeps the traits but wants the tools to sit in the pack's existing
 * enchanting progression too, so both gates are reopened here.
 *
 * <p>Which enchantments apply is answered by asking the vanilla counterpart rather than by listing
 * enchantments: a pickaxe takes what a diamond pickaxe takes, the scythe takes what a hoe takes.
 * That stays correct when another mod extends {@code #minecraft:enchantable/*}.
 */
@Mixin(PartToolItem.class)
public abstract class PartToolItemMixin {

    public abstract ToolType getToolType();

    private boolean bertietoolcraft$isKept() {
        ToolType type = this.getToolType();
        return type != null && ToolcraftPolicy.isKeptTool(type.getId());
    }

    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void bertietoolcraft$allowEnchanting(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (bertietoolcraft$isKept()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "supportsEnchantment", at = @At("HEAD"), cancellable = true)
    private void bertietoolcraft$supportVanillaEnchantments(
            ItemStack stack, Holder<Enchantment> enchantment, CallbackInfoReturnable<Boolean> cir) {
        ToolType type = this.getToolType();
        if (type == null) {
            return;
        }
        String proxyPath = ToolcraftPolicy.vanillaEnchantmentProxy(type.getId());
        if (proxyPath == null) {
            return; // not a kept tool - leave Magitech's "no enchanting" answer in place
        }
        Item proxy = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(proxyPath));
        cir.setReturnValue(
                enchantment.value().definition().supportedItems().contains(proxy.builtInRegistryHolder()));
    }

    /**
     * Magitech never declares an enchantment value, so the NeoForge default would report 0 and the
     * table would only ever offer the weakest roll. Adding the method here overrides the interface
     * default on the target class.
     */
    public int getEnchantmentValue(ItemStack stack) {
        return bertietoolcraft$isKept() ? ToolcraftPolicy.ENCHANTMENT_VALUE : 0;
    }
}
