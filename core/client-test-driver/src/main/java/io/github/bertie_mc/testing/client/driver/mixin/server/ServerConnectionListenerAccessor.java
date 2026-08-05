package io.github.bertie_mc.testing.client.driver.mixin.server;

import io.netty.channel.ChannelFuture;
import java.util.List;
import net.minecraft.server.network.ServerConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes server listener channels so the driver can discover the ephemeral bound port. */
@Mixin(ServerConnectionListener.class)
public interface ServerConnectionListenerAccessor {
    @Accessor("channels")
    List<ChannelFuture> bertie$getChannels();
}
