package io.github.bertie_mc.bertieprogression.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Owo's ordinal-specific hook misses the newer NeoForge codec-return branch. */
@Mixin(value = NetworkRegistry.class, remap = false)
public abstract class OwoNetworkCodecMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @ModifyReturnValue(
            method =
                    "getCodec(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/ConnectionProtocol;Lnet/minecraft/network/protocol/PacketFlow;)Lnet/minecraft/network/codec/StreamCodec;",
            at = @At("RETURN"))
    private static StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload> bertieprogression$unwrapSide(
            StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload> original,
            @Local(argsOnly = true) PacketFlow flow) {
        return original instanceof OwoSidedCodecAccessor sided
                ? (StreamCodec) sided.bertieprogression$codec(flow)
                : original;
    }
}
