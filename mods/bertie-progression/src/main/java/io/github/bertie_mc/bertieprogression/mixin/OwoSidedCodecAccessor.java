package io.github.bertie_mc.bertieprogression.mixin;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "io.wispforest.owo.network.neoforge.SidedPacketCodec", remap = false)
public interface OwoSidedCodecAccessor {
    @Invoker("getCodec")
    StreamCodec<?, ?> bertieprogression$codec(PacketFlow flow);
}
