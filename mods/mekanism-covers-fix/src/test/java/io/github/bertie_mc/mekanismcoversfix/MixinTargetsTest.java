package io.github.bertie_mc.mekanismcoversfix;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MixinTargetsTest {
    @Test
    void refreshPatchLoadsAlongsideUpstreamCoverMixin() throws ClassNotFoundException {
        Class<?> transmitter = Class.forName("mekanism.common.tile.transmitter.TileEntityTransmitter");
        assertTrue(Arrays.stream(transmitter.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(name -> name.contains("mekanismcoversfix$refreshCoverModel")));
        assertTrue(Arrays.stream(transmitter.getInterfaces())
                .anyMatch(type ->
                        type.getName().equals("dev.lucaargolo.mekanismcovers.mixed.TileEntityTransmitterMixed")));
    }
}
