package io.github.bertie_mc.bertieprogression.mixin;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Some fuels superheat a Blaze Burner and then go out, rather than settling into a lit one.
 *
 * <p>Create's burner treats superheating as a phase of a long burn: when special fuel runs dry it
 * drops to NORMAL with five thousand ticks still on the clock. That is right for a Blaze Cake, which
 * is a considered investment. It is wrong for an egg, which superheats for ten ticks and would
 * otherwise leave the burner lit for four minutes afterwards - a hand-fed stopgap turning into a
 * better deal than the fuel it was standing in for.
 *
 * <p>Anything in {@code bertieprogression:snuffing_blaze_fuel} snuffs out instead. The tag is read
 * as the fuel goes in and remembered, because by the time the burn ends nothing records what lit it.
 * The flag is deliberately not saved to disk: it only has to survive ten ticks, and a burner that
 * was mid-egg across a reload falling back to Create's own behaviour is a fair worst case.
 */
@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public abstract class BlazeBurnerBlockEntityMixin {

    @Unique
    private static final TagKey<Item> bertieprogression$SNUFFING = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("bertieprogression", "snuffing_blaze_fuel"));

    @Unique
    private boolean bertieprogression$snuffOut;

    @Shadow
    protected BlazeBurnerBlockEntity.FuelType activeFuel;

    @Shadow
    protected int remainingBurnTime;

    @Shadow
    public abstract void updateBlockState();

    @Inject(method = "tryUpdateFuel", at = @At("RETURN"), remap = false)
    private void bertieprogression$rememberFuel(
            ItemStack stack,
            boolean forceOverflow,
            boolean simulate,
            CallbackInfoReturnable<Boolean> cir) {
        if (simulate || !cir.getReturnValueZ()) {
            return;
        }
        // A later, longer fuel clears the mark: a Blaze Cake fed onto an egg burns as a cake.
        if (activeFuel == BlazeBurnerBlockEntity.FuelType.SPECIAL) {
            bertieprogression$snuffOut = stack.is(bertieprogression$SNUFFING);
        }
    }

    /**
     * Pre-empts the SPECIAL -> NORMAL fallback by a tick, which is the only place it happens.
     */
    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void bertieprogression$snuff(CallbackInfo ci) {
        if (!bertieprogression$snuffOut) {
            return;
        }
        if (activeFuel != BlazeBurnerBlockEntity.FuelType.SPECIAL) {
            bertieprogression$snuffOut = false;
            return;
        }
        if (remainingBurnTime > 1) {
            return;
        }
        activeFuel = BlazeBurnerBlockEntity.FuelType.NONE;
        remainingBurnTime = 0;
        bertieprogression$snuffOut = false;
        updateBlockState();
    }
}
