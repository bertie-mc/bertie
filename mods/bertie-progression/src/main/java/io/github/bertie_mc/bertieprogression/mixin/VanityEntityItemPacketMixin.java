package io.github.bertie_mc.bertieprogression.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanity sends the item every arrow-type projectile would drop when a player starts tracking it,
 * through a codec that refuses an empty stack. A modded projectile that drops nothing therefore
 * failed to encode and disconnected the player. The packet now carries a plain arrow instead: it has
 * no Vanity style, so the projectile renders exactly as the empty stack would have.
 */
@Pseudo
@Mixin(
        targets = "tech.thatgravyboat.vanity.common.network.packets.client.ClientboundSyncEntityItemPacket",
        remap = false)
public abstract class VanityEntityItemPacketMixin {
    @Shadow
    @Mutable
    @Final
    private ItemStack stack;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void bertie$neverEmpty(int entityId, ItemStack original, CallbackInfo ci) {
        if (stack.isEmpty()) {
            stack = new ItemStack(Items.ARROW);
        }
    }
}
