package io.github.bertie_mc.alexscavesworldgenfix.mixin.alexscaves;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops Alex's Caves asking a spawn egg for its entity type with a {@code null} item stack.
 *
 * <p>{@code ACItemRegistry.getSpawnEggFor} walks its own eggs looking for one that matches an
 * entity type, and identifies each by calling {@code egg.getType(null)}. Older NeoForge tolerated
 * that; 21.1.233's {@code SpawnEggItem#getType} goes straight to {@code stack.getOrDefault(...)}
 * and dies:
 * <pre>
 * NullPointerException: Cannot invoke "ItemStack.getOrDefault(...)" because "p_330335_" is null
 *   at SpawnEggItem.getType(SpawnEggItem.java:150)
 *   at ACItemRegistry.getSpawnEggFor(ACItemRegistry.java:389)
 * </pre>
 *
 * <p>It throws on the FIRST egg it examines, so the lookup is broken for every entity - not only
 * the one that happened to ask. berlord hit it by middle-clicking a Gum Worm segment, whose
 * {@code getPickResult} routes through here; the crash killed the client (2026-08-05).
 *
 * <p>Handing over a real stack restores the behaviour Alex's Caves wrote the method for: a spawn
 * egg with no {@code entity_data} component reports its own default type, so the search now matches
 * and pick block yields the Gum Worm egg it always meant to.
 */
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.item.ACItemRegistry", remap = false)
public abstract class ACItemRegistryMixin {

    @WrapOperation(
            method = "getSpawnEggFor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/DeferredSpawnEggItem;"
                            + "getType(Lnet/minecraft/world/item/ItemStack;)"
                            + "Lnet/minecraft/world/entity/EntityType;"),
            remap = false)
    private static EntityType<?> alexscavesworldgenfix$identifyEggWithARealStack(
            DeferredSpawnEggItem egg, ItemStack stack, Operation<EntityType<?>> original) {
        // Only the null case is substituted; a caller that already passes a stack keeps it, so a
        // future Alex's Caves build that fixes this itself is unaffected.
        return original.call(egg, stack == null ? new ItemStack(egg) : stack);
    }
}
